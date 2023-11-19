import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.To;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.kstream.Transformer;

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
import ccm.models.common.event.CaseUserEvent;
import ccm.models.common.event.ChargeAssessmentEvent;
import ccm.models.common.event.EventKPI;

public class CaseAccessSyncTransformer implements Transformer<String, String, KeyValue<String, String>> {

    private static final Logger LOG = LoggerFactory.getLogger(CaseAccessSyncTransformer.class);

    private KeyValueStore<String, String> accessdedupStore;

    private ProcessorContext context;

    private String appId;
    private String chargeAssessmentsTopicName;
    private String chargeAssessmentErrorsTopicName;
    private String bulkCaseUsersTopicName;
    private String caseUserErrorsTopicName;
    private String kpisTopicName;
    private String kpiErrorsTopicName;
    private String caseAccessSyncStoreName;

    @Override
    @SuppressWarnings("unchecked")
    public void init(ProcessorContext context) {
        // Dependency injection doesn't work in Kafka Streams.  We have to use the ConfigProvider.
        appId = ConfigProvider.getConfig().getValue("quarkus.kafka-streams.application-id", String.class);
        chargeAssessmentsTopicName = ConfigProvider.getConfig().getValue("ccm.topic.chargeassessments.name", String.class);
        chargeAssessmentErrorsTopicName = ConfigProvider.getConfig().getValue("ccm.topic.chargeassessment-errors.name", String.class);
        bulkCaseUsersTopicName = ConfigProvider.getConfig().getValue("ccm.topic.bulk-caseusers.name", String.class);
        caseUserErrorsTopicName = ConfigProvider.getConfig().getValue("ccm.topic.caseuser-errors.name", String.class);
        kpisTopicName = ConfigProvider.getConfig().getValue("ccm.topic.kpis.name", String.class);
        kpiErrorsTopicName = ConfigProvider.getConfig().getValue("ccm.topic.kpi-errors.name", String.class);
        caseAccessSyncStoreName = ConfigProvider.getConfig().getValue("ccm.store.caseaccesssync.name", String.class);

        LOG.info("caseAccessSyncStoreName: {}.", caseAccessSyncStoreName);

        // Initialize the state store.
        this.accessdedupStore = (KeyValueStore<String, String>) context.getStateStore(caseAccessSyncStoreName);
        this.context = context;

        LOG.info("Processor name: {}.", appId);
        LOG.info("ccm model version: {}.", Version.V1_0);

        // Log the topic names.
        LOG.info("bulkCaseUsersTopicName: {}.", bulkCaseUsersTopicName);
    }

    @Override
    public KeyValue<String, String> transform(String key, String value) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            CaseUserEvent caseUserEvent = mapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(value, CaseUserEvent.class);

            String rcc_id = caseUserEvent.getJustin_rcc_id();

            if (key == null || key.isEmpty()) {
                // End of the batch is detected
                LOG.info("End of the event batch detected (event message key is empty).  Forwarding and clearing derived charge assessment events in {}...", caseAccessSyncStoreName);

                EventKPI eventKPI = new EventKPI(caseUserEvent, EventKPI.STATUS.EVENT_PROCESSING_STARTED);
                eventKPI.setIntegration_component_name(appId);
                eventKPI.setComponent_route_name(this.getClass().getSimpleName());
                //this.context.forward(key, mapper.writeValueAsString(eventKPI), To.child(kpisTopicName)); 

                AtomicLong event_count = new AtomicLong(0);

                accessdedupStore.all().forEachRemaining(keyValue -> {
                    event_count.incrementAndGet();
                    LOG.info("Forwarding from store: charge assessment {}.", keyValue.key);
                    //context.forward(keyValue.key, keyValue.value, To.child(chargeAssessmentsTopicName));
                    context.forward(keyValue.key, keyValue.value);
                });

                accessdedupStore.all().forEachRemaining(keyValue -> {
                    accessdedupStore.delete(keyValue.key);
                });

                LOG.info("Store cleared and {} event{} forwarded.", event_count.get(), event_count.get() == 1 ? "" : "s");
                
                return null;
            }

            // Only forward the message if the key is found in the deduplication store.
            if (accessdedupStore.get(rcc_id) == null) {
                    ChargeAssessmentEvent chargeAssessmentEvent = new ChargeAssessmentEvent(ChargeAssessmentEvent.SOURCE.JADE_CCM,caseUserEvent);
                    chargeAssessmentEvent.setEvent_key(rcc_id);

                    String derivedEventValue = mapper.writeValueAsString(chargeAssessmentEvent);

                    JsonObject json = new JsonObject(value);
                    LOG.info("Storing the new charge assessment event {}, derived from case user event {}.", chargeAssessmentEvent.getEvent_key(), key);

                    accessdedupStore.put(rcc_id, derivedEventValue);
            } else {
                LOG.info("Skipping case user event {}; the derived charge assessment event {} already exists in store.", key, rcc_id);
            }
        } catch (JsonProcessingException jpe) {
            LOG.error("Error processing case user event message for {}. Error message: {}", key, jpe.getMessage());
        } catch (Exception e) {
            LOG.error("General error processing {}.  Error message: {}", key, e.getMessage());
        }

        return null;
    }

    public void close() {
        // No additional cleanup is required as the store is managed by Kafka Streams.
    }
}
