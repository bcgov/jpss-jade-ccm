import java.nio.charset.StandardCharsets;

import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.To;
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

public class CaseUserEventHandler extends AbstractProcessor<String, String> {
    private static final Logger LOG = LoggerFactory.getLogger(CaseUserEventHandler.class);

    private ProcessorContext context;

    private KeyValueStore<String, String> accessdedupStore;

    private String appId;
    private String bulkChargeAssessmentsTopicName;
    private String bulkCaseUsersTopicName;
    private String caseUserErrorsTopicName;
    private String kpisTopicName;
    private String kpiErrorsTopicName;
    private String caseAccessSyncStoreName;
    private String appNameProperCase;

    private static final String EVENT_BATCH_COUNT_STRING = "event_batch_count";

    @Override
    public void init(ProcessorContext context) {
        super.init(context);
        this.context = context;

        // Dependency injection doesn't work in Kafka Streams.  We have to use the ConfigProvider.
        appId = ConfigProvider.getConfig().getValue("quarkus.kafka-streams.application-id", String.class);
        bulkChargeAssessmentsTopicName = ConfigProvider.getConfig().getValue("ccm.topic.bulk-chargeassessments.name", String.class);
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

            // skip if event is not in scope
            if (CaseUserEvent.STATUS.EVENT_BATCH_STARTED.name().equals(caseUserEvent.getEvent_status()) ||
                CaseUserEvent.STATUS.EVENT_BATCH_ENDED.name().equals(caseUserEvent.getEvent_status()) || 
                CaseUserEvent.STATUS.ACCESS_ADDED.name().equals(caseUserEvent.getEvent_status()) ||
                CaseUserEvent.STATUS.ACCESS_REMOVED.name().equals(caseUserEvent.getEvent_status())) {
                // This is an in-scope event.  Process it.
            } else {
                // This is a out-of-scope event.  Ignore it.
                return;
            }

            LOG.info("Processing case user {} event message{}...", 
                caseUserEvent.getEvent_status(), 
                (key != null && key.length() > 0) ? " for " + key : "");

            // Produce a KPI event for the case user event.
            produceEventKpi(key, value, null, EventKPI.STATUS.EVENT_PROCESSING_STARTED, context.topic(), context.offset());

            if (CaseUserEvent.STATUS.EVENT_BATCH_STARTED.name().equals(caseUserEvent.getEvent_status())) {
                // Start of the batch is detected; update batch count in the store.

                LOG.info("'Start of event batch' message received.");

                // Get the current batch count from the store.
                String batchCountString = accessdedupStore.get(EVENT_BATCH_COUNT_STRING);
                AtomicLong batchCount = new AtomicLong((batchCountString != null) ? Long.parseLong(batchCountString) : 0);

                // Increment the batch count and store it.
                batchCount.incrementAndGet();
                accessdedupStore.put(EVENT_BATCH_COUNT_STRING, batchCount.toString());

                LOG.info("Batch count incremented to {}.", batchCount.toString());

            } else if (CaseUserEvent.STATUS.EVENT_BATCH_ENDED.name().equals(caseUserEvent.getEvent_status())) {
                // End of the batch is detected; update batch count in the store.
                // If this is the last batch, forward all stored events and clear the store.

                LOG.info("'End of event batch' message received.");

                // Get the current batch count from the store.

                String batchCountString = accessdedupStore.get(EVENT_BATCH_COUNT_STRING);
                AtomicLong batchCount = new AtomicLong((batchCountString != null) ? Long.parseLong(batchCountString) : 0);

                // If this is the last batch, forward all stored events and clear the store.
                if (batchCount.get() > 1) {
                    // This is not the last batch.  Decrement the batch count and store it.

                    batchCount.decrementAndGet();
                    accessdedupStore.put(EVENT_BATCH_COUNT_STRING, batchCount.toString());

                    LOG.info("This is not the last batch.  Batch count decremented to {}.", batchCount.toString());
                    return;
                } else {
                    // This is the last batch.  Remove batch count entry, forward all stored events, and clear the store.

                    LOG.info("This is the last batch.  Remove batch count entry, forwarding all stored events, and clearing the store.");

                    accessdedupStore.delete(EVENT_BATCH_COUNT_STRING);

                    AtomicLong event_count = new AtomicLong(0);

                    accessdedupStore.all().forEachRemaining(keyValue -> {
                        event_count.incrementAndGet();
                        LOG.info("Forwarding from store: charge assessment {}.", keyValue.key);

                        context.forward(keyValue.key, keyValue.value, 
                            CaseUserEventHandler.util_getToplogySinkName(bulkChargeAssessmentsTopicName));
                    });

                    accessdedupStore.all().forEachRemaining(keyValue -> {
                        accessdedupStore.delete(keyValue.key);
                    });

                    LOG.info("Store cleared and {} event{} forwarded.", event_count.get(), event_count.get() == 1 ? "" : "s");
                }
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
            produceEventKpi(key, value, null, EventKPI.STATUS.EVENT_PROCESSING_COMPLETED, context.topic(), context.offset());
        } catch (JsonProcessingException jpe) {
            String errorDetails = "Error processing case user event message for " + key + ". Error message: " + jpe;
            LOG.error(errorDetails);

            Error error = new Error();
            error.setError_code(jpe.getClass().getSimpleName());
            error.setError_summary("Event processing failed");
            error.setError_details(errorDetails);

            produceEventKpi(key, value, error, EventKPI.STATUS.EVENT_PROCESSING_FAILED, context.topic(), context.offset());

            context.forward(key, value, CaseUserEventHandler.util_getToplogySinkName(caseUserErrorsTopicName));
        } catch (Exception e) {
            String errorDetails = "General error processing " + key + ".  Error message: " + e;
            LOG.error(errorDetails);

            Error error = new Error();
            error.setError_code(e.getClass().getSimpleName());
            error.setError_summary("General error");
            error.setError_details(errorDetails);

            produceEventKpi(key, value, error, EventKPI.STATUS.EVENT_PROCESSING_FAILED, context.topic(), context.offset());

            context.forward(key, value, CaseUserEventHandler.util_getToplogySinkName(caseUserErrorsTopicName));
        }
    }

    void produceEventKpi(String eventKey, String eventValue, Error eventError, EventKPI.STATUS status, String topicName, long topicOffset) {
        EventKPI eventKPI = new EventKPI(status);

        eventKPI.setEvent_topic_name(topicName);
        eventKPI.setEvent_topic_offset(Long.toString(topicOffset));

        eventKPI.setIntegration_component_name(appNameProperCase);
        eventKPI.setComponent_route_name(this.getClass().getSimpleName());

        // convert event value to json object if possible
        Object eventValueObject = null;
        try {
            // convert to json object
            eventValueObject = new ObjectMapper().readValue(eventValue, Object.class);
        } catch (Exception e) {
            // not a json object; use the string value
            eventValueObject = eventValue;
        }
        eventKPI.setEvent_details(eventValueObject);

        eventKPI.setError(eventError);

        try {
            ObjectMapper mapper = new ObjectMapper();
            context.forward(null, mapper.writeValueAsString(eventKPI), CaseUserEventHandler.util_getToplogySinkName(kpisTopicName)); 
        } catch (JsonProcessingException jpe) {
            LOG.error("Error processing event KPI message for {}. Error message: {}", eventKey, jpe.getMessage());
        } catch (Exception e) {
            LOG.error("General error processing event KPI message for {}.  Error message: {}", eventKey, e.getMessage());
        }
    }

    public static String util_getTopologySourceName() {
        return CaseUserEvent.class.getSimpleName() + "Source";
    }

    public static String util_getTopologyProcessorName() {
        return CaseUserEventHandler.class.getSimpleName();
    }

    public static String util_getToplogySinkName(String topicName) {
        return CaseUserEvent.class.getSimpleName() + "Sink-" + topicName;
    }
}
