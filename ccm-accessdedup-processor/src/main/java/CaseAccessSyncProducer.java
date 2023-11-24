import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;

import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.json.JsonObject;

import ccm.models.common.versioning.Version;
import ccm.models.common.event.BaseEvent;
import ccm.models.common.event.CaseUserEvent;
import ccm.models.common.event.ChargeAssessmentEvent;
import ccm.models.common.event.EventKPI;
import ccm.models.common.event.Error;

public class CaseAccessSyncProducer extends AbstractProcessor<String, String> {
    private static final Logger LOG = LoggerFactory.getLogger(CaseAccessSyncProducer.class);

    private ProcessorContext context;

    private KeyValueStore<String, String> accessdedupStore;

    private String appId;
    private String chargeAssessmentsTopicName;
    private String chargeAssessmentErrorsTopicName;
    private String bulkCaseUsersTopicName;
    private String caseUserErrorsTopicName;
    private String kpisTopicName;
    private String kpiErrorsTopicName;
    private String caseAccessSyncStoreName;
    private String appNameProperCase;

    @Override
    public void init(ProcessorContext context) {
        super.init(context);
        this.context = context;

        // Dependency injection doesn't work in Kafka Streams.  We have to use the ConfigProvider.
        appId = ConfigProvider.getConfig().getValue("quarkus.kafka-streams.application-id", String.class);
        chargeAssessmentsTopicName = ConfigProvider.getConfig().getValue("ccm.topic.chargeassessments.name", String.class);
        bulkCaseUsersTopicName = ConfigProvider.getConfig().getValue("ccm.topic.bulk-caseusers.name", String.class);
        caseUserErrorsTopicName = ConfigProvider.getConfig().getValue("ccm.topic.caseuser-errors.name", String.class);
        kpisTopicName = ConfigProvider.getConfig().getValue("ccm.topic.kpis.name", String.class);
        caseAccessSyncStoreName = ConfigProvider.getConfig().getValue("ccm.store.caseaccesssync.name", String.class);
        appNameProperCase = ConfigProvider.getConfig().getValue("ccm.application.name.propercase", String.class);

        // Initialize the state store.
        this.accessdedupStore = (KeyValueStore<String, String>) context.getStateStore(caseAccessSyncStoreName);

        LOG.info("Processor name: {}.", appId);
        LOG.info("Processor name (proper case): {}.", appNameProperCase);
        LOG.info("ccm model version: {}.", Version.V1_0);
        LOG.info("caseAccessSyncStoreName: {}.", caseAccessSyncStoreName);
    }

    @Override
    public void process(String key, String value) {
        handleEventMessage(key, value);
    }

    @Override
    public void close() {
        // Cleanup logic as needed.
    }

    void handleEventMessage(String key, String value) { 
        // use method name as route id
        String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

        try {
            ObjectMapper mapper = new ObjectMapper();
            CaseUserEvent caseUserEvent = mapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(value, CaseUserEvent.class);

            if (!CaseUserEvent.STATUS.ACCESS_ADDED.name().equals(caseUserEvent.getEvent_status()) &&
                !CaseUserEvent.STATUS.EVENT_BATCH_ENDED.name().equals(caseUserEvent.getEvent_status())) {
                // This is not a new access event or the end of the batch.  Ignore it.
                return;
            }

            LOG.info("Processing case user {} event message{}...", 
                caseUserEvent.getEvent_status(), 
                (key != null && key.length() > 0) ? " for " + key : "");

            // Produce a KPI event for the case user event.
            produceEventKpi(caseUserEvent, EventKPI.STATUS.EVENT_PROCESSING_STARTED, context.topic(), context.offset());

            if (CaseUserEvent.STATUS.EVENT_BATCH_ENDED.name().equals(caseUserEvent.getEvent_status())) {
                // End of the batch is detected; forward all stored events and clear the store.

                LOG.info("'End of event batch' message received.  Forwarding and clearing derived charge assessment event messages in {}...", caseAccessSyncStoreName);

                AtomicLong event_count = new AtomicLong(0);

                accessdedupStore.all().forEachRemaining(keyValue -> {
                    event_count.incrementAndGet();
                    LOG.info("Forwarding from store: charge assessment {}.", keyValue.key);
                    context.forward(keyValue.key, keyValue.value, "SinkFor-" + chargeAssessmentsTopicName);

                    // Produce a KPI event for the charge assessment event.
                    try {
                        ChargeAssessmentEvent event = mapper
                            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                            .readValue(keyValue.value, ChargeAssessmentEvent.class);
                        produceEventKpiNoContext(event, EventKPI.STATUS.EVENT_CREATED, chargeAssessmentsTopicName);
                    } catch (JsonProcessingException jpe) {
                        LOG.error("Error producing KPI for charge assessment event message for {}. Error message: {}", keyValue.key, keyValue.value, jpe.getMessage());
                    } catch (Exception e) {
                        LOG.error("General error producing KPI.  Error message: {}", keyValue.key, keyValue.value, e.getMessage());
                    }
                });

                accessdedupStore.all().forEachRemaining(keyValue -> {
                    accessdedupStore.delete(keyValue.key);
                });

                LOG.info("Store cleared and {} event{} forwarded.", event_count.get(), event_count.get() == 1 ? "" : "s");
            } else if (accessdedupStore.get(caseUserEvent.getJustin_rcc_id()) == null) {
                // This is for a new charge assessment.  Create a new charge assessment event and store it.

                String rcc_id = caseUserEvent.getJustin_rcc_id();

                ChargeAssessmentEvent chargeAssessmentEvent = new ChargeAssessmentEvent(ChargeAssessmentEvent.SOURCE.JADE_CCM,caseUserEvent);
                chargeAssessmentEvent.setEvent_key(rcc_id);

                String derivedEventValue = mapper.writeValueAsString(chargeAssessmentEvent);

                JsonObject json = new JsonObject(value);
                LOG.info("Storing the new charge assessment event message for {}, derived from case user event for {}.", chargeAssessmentEvent.getEvent_key(), key);

                accessdedupStore.put(rcc_id, derivedEventValue);
            } else {
                LOG.info("Not creating a new charge assessment event message; the derived event for {} already exists in the store.", 
                    caseUserEvent.getJustin_rcc_id());
            }
            
            // Produce a KPI event for the case user event.
            produceEventKpi(caseUserEvent, EventKPI.STATUS.EVENT_PROCESSING_COMPLETED, context.topic(), context.offset());
        } catch (JsonProcessingException jpe) {
            String errorDetails = "Error processing case user event message for " + key + ". Error message: " + jpe;
            LOG.error(errorDetails);

            Error error = new Error();
            error.setError_code(jpe.getClass().getSimpleName());
            error.setError_summary("Event processing failed");
            error.setError_details(errorDetails);

            produceEventKpiByEventKeyValueError(key, value, error, EventKPI.STATUS.EVENT_PROCESSING_FAILED, context.topic(), context.offset());

            context.forward(key, value, "SinkFor-" + caseUserErrorsTopicName);
        } catch (Exception e) {
            String errorDetails = "General error processing " + key + ".  Error message: " + e;
            LOG.error(errorDetails);

            Error error = new Error();
            error.setError_code(e.getClass().getSimpleName());
            error.setError_summary("General error");
            error.setError_details(errorDetails);

            produceEventKpiByEventKeyValueError(key, value, error, EventKPI.STATUS.EVENT_PROCESSING_FAILED, context.topic(), context.offset());

            context.forward(key, value, "SinkFor-" + caseUserErrorsTopicName);
        }
    }

    void produceEventKpiNoContext(BaseEvent event, EventKPI.STATUS status, String topicName) {
        produceEventKpi(event, status, topicName, null);
    }

    void produceEventKpi(BaseEvent event, EventKPI.STATUS status, String topicName, long topicOffset) {
        produceEventKpi(event, status, topicName, Long.toString(topicOffset));
    }

    void produceEventKpi(BaseEvent event, EventKPI.STATUS status, String topicName, String topicOffset) {
        EventKPI eventKPI = new EventKPI(event, status);

        eventKPI.setEvent_topic_name(topicName);
        eventKPI.setEvent_topic_offset(topicOffset);

        eventKPI.setIntegration_component_name(appNameProperCase);
        eventKPI.setComponent_route_name(this.getClass().getSimpleName());

        try {
            ObjectMapper mapper = new ObjectMapper();
            context.forward(null, mapper.writeValueAsString(eventKPI), "SinkFor-" + kpisTopicName); 
        } catch (JsonProcessingException jpe) {
            LOG.error("Error processing event KPI message for {}. Error message: {}", event.getEvent_key(), jpe.getMessage());
        } catch (Exception e) {
            LOG.error("General error processing event KPI message for {}.  Error message: {}", event.getEvent_key(), e.getMessage());
        }
    }

    void produceEventKpiByEventKeyValueError(String eventKey, String eventValue, Error eventError, EventKPI.STATUS status, String topicName, long topicOffset) {
        EventKPI eventKPI = new EventKPI(status);

        eventKPI.setEvent_topic_name(topicName);
        eventKPI.setEvent_topic_offset(Long.toString(topicOffset));

        eventKPI.setIntegration_component_name(appNameProperCase);
        eventKPI.setComponent_route_name(this.getClass().getSimpleName());

        eventKPI.setEvent_details(eventValue);

        eventKPI.setError(eventError);

        try {
            ObjectMapper mapper = new ObjectMapper();
            context.forward(null, mapper.writeValueAsString(eventKPI), "SinkFor-" + kpisTopicName); 
        } catch (JsonProcessingException jpe) {
            LOG.error("Error processing event KPI message for {}. Error message: {}", eventKey, jpe.getMessage());
        } catch (Exception e) {
            LOG.error("General error processing event KPI message for {}.  Error message: {}", eventKey, e.getMessage());
        }
    }
}
