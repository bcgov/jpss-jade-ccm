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

public class CaseAccessSyncProcessor extends AbstractProcessor<String, String> {
    private static final Logger LOG = LoggerFactory.getLogger(CaseAccessSyncProcessor.class);

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

    @Override
    public void init(ProcessorContext context) {
        super.init(context);
        this.context = context;

        // Dependency injection doesn't work in Kafka Streams.  We have to use the ConfigProvider.
        appId = ConfigProvider.getConfig().getValue("quarkus.kafka-streams.application-id", String.class);
        chargeAssessmentsTopicName = ConfigProvider.getConfig().getValue("ccm.topic.chargeassessments.name", String.class);
        chargeAssessmentErrorsTopicName = ConfigProvider.getConfig().getValue("ccm.topic.chargeassessment-errors.name", String.class);
        bulkCaseUsersTopicName = ConfigProvider.getConfig().getValue("ccm.topic.bulk-caseusers.name", String.class);
        caseUserErrorsTopicName = ConfigProvider.getConfig().getValue("ccm.topic.caseuser-errors.name", String.class);
        kpisTopicName = ConfigProvider.getConfig().getValue("ccm.topic.kpis.name", String.class);
        kpiErrorsTopicName = ConfigProvider.getConfig().getValue("ccm.topic.kpi-errors.name", String.class);
        caseAccessSyncStoreName = ConfigProvider.getConfig().getValue("ccm.store.caseaccesssync.name", String.class);

        // Initialize the state store.
        this.accessdedupStore = (KeyValueStore<String, String>) context.getStateStore(caseAccessSyncStoreName);

        LOG.info("Processor name: {}.", appId);
        LOG.info("ccm model version: {}.", Version.V1_0);
        LOG.info("caseAccessSyncStoreName: {}.", caseAccessSyncStoreName);
    }

    @Override
    public void process(String key, String value) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            CaseUserEvent caseUserEvent = mapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(value, CaseUserEvent.class);

            // Produce a KPI event for the case user event.
            produceEventKpi(caseUserEvent, EventKPI.STATUS.EVENT_PROCESSING_STARTED, context.topic(), context.offset());

            String rcc_id = caseUserEvent.getJustin_rcc_id();

            if (CaseUserEvent.STATUS.EVENT_BATCH_ENDED.name().equals(caseUserEvent.getEvent_status())) {
                // End of the batch is detected; forward all stored events and clear the store.

                LOG.info("End of the event batch detected (event message key is empty).  Forwarding and clearing derived charge assessment events in {}...", caseAccessSyncStoreName);

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
            } else if (accessdedupStore.get(rcc_id) == null) {
                // This is for a new charge assessment.  Create a new charge assessment event and store it.

                ChargeAssessmentEvent chargeAssessmentEvent = new ChargeAssessmentEvent(ChargeAssessmentEvent.SOURCE.JADE_CCM,caseUserEvent);
                chargeAssessmentEvent.setEvent_key(rcc_id);

                String derivedEventValue = mapper.writeValueAsString(chargeAssessmentEvent);

                JsonObject json = new JsonObject(value);
                LOG.info("Storing the new charge assessment event {}, derived from case user event {}.", chargeAssessmentEvent.getEvent_key(), key);

                accessdedupStore.put(rcc_id, derivedEventValue);
            } else {
                LOG.info("Skipping case user event {}; the derived charge assessment event {} already exists in store.", key, rcc_id);
            }

            // Produce a KPI event for the case user event.
            produceEventKpi(caseUserEvent, EventKPI.STATUS.EVENT_PROCESSING_COMPLETED, context.topic(), context.offset());
        } catch (JsonProcessingException jpe) {
            LOG.error("Error processing case user event message for {}. Error message: {}", key, jpe.getMessage());
        } catch (Exception e) {
            LOG.error("General error processing {}.  Error message: {}", key, e.getMessage());
        }

        return;
    }

    @Override
    public void close() {
        // Cleanup logic as needed.
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

        eventKPI.setIntegration_component_name(appId);
        eventKPI.setComponent_route_name(this.getClass().getSimpleName());

        try {
            ObjectMapper mapper = new ObjectMapper();
            context.forward(null, mapper.writeValueAsString(eventKPI), "SinkFor-" + kpisTopicName); 
        } catch (JsonProcessingException jpe) {
            LOG.error("Error processing event KPI message for {}. Error message: {}", event.getEvent_key(), jpe.getMessage());

            //TODO: Produce an error event.
            //TODO: Add event message to error topic.
        } catch (Exception e) {
            LOG.error("General error processing event KPI message for {}.  Error message: {}", event.getEvent_key(), e.getMessage());
        }
    }
}
