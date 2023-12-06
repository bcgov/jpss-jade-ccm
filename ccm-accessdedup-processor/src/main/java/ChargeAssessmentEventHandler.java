import java.nio.charset.StandardCharsets;

import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;

import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;

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

public class ChargeAssessmentEventHandler extends AbstractProcessor<String, String> {
    private static final Logger LOG = LoggerFactory.getLogger(ChargeAssessmentEventHandler.class);

    private ProcessorContext context;

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
    }

    @Override
    public void process(String key, String value) {
        ChargeAssessmentEvent chargeAssessmentEvent = null;

        try {
            chargeAssessmentEvent = new ObjectMapper().readValue(value, ChargeAssessmentEvent.class);

            LOG.debug("Recevied ChargeAssessmentEvent message for {}.", key);

            // If the event is an inferr, then we need to produce a EVENT_CREATED KPI.
            if (ChargeAssessmentEvent.STATUS.INFERRED_AUTH_LIST_CHANGED.name().equals(chargeAssessmentEvent.getEvent_status())) {
                LOG.debug("Post-processing ChargeAssessmentEvent message for {} ...", key);

                String topicName = this.context.topic();
                long topicOffset = this.context.offset();
                produceEventKpiForCaseUserEventHandler(key, value, null, EventKPI.STATUS.EVENT_CREATED, topicName, topicOffset);

                LOG.debug("Produced KPI for newly created ChargeAssessmentEvent message.", key);
            }
        } catch (Exception e) {
            // log parsing error
            LOG.error("Error parsing ChargeAssessmentEvent message for {}. Error message: {}", key, e);
        }
    }

    @Override
    public void close() {
        // Cleanup logic as needed.
    }

    void produceEventKpiForCaseUserEventHandler(String eventKey, String eventValue, Error eventError, EventKPI.STATUS status, String topicName, long topicOffset) {
        produceEventKpi(eventKey, eventValue, eventError, status, CaseUserEventHandler.class.getSimpleName(), topicName, topicOffset);
    }

    void produceEventKpi(String eventKey, String eventValue, Error eventError, EventKPI.STATUS status, String topicName, long topicOffset) {
        produceEventKpi(eventKey, eventValue, eventError, status, this.getClass().getSimpleName(), topicName, topicOffset);
    }

    void produceEventKpi(String eventKey, String eventValue, Error eventError, EventKPI.STATUS status, String routeName, String topicName, long topicOffset) {
        EventKPI eventKPI = new EventKPI(status);

        eventKPI.setEvent_topic_name(topicName);
        eventKPI.setEvent_topic_offset(Long.toString(topicOffset));

        eventKPI.setIntegration_component_name(appNameProperCase);
        eventKPI.setComponent_route_name(routeName);

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
            context.forward(null, mapper.writeValueAsString(eventKPI), ChargeAssessmentEventHandler.util_getToplogySinkName(kpisTopicName)); 
        } catch (JsonProcessingException jpe) {
            LOG.error("Error processing event KPI message for {}. Error message: {}", eventKey, jpe.getMessage());
        } catch (Exception e) {
            LOG.error("General error processing event KPI message for {}.  Error message: {}", eventKey, e.getMessage());
        }
    }

    public static String util_getTopologySourceName() {
        return ChargeAssessmentEventHandler.class.getSimpleName() + "Source";
    }

    public static String util_getTopologyProcessorName() {
        return ChargeAssessmentEventHandler.class.getSimpleName();
    }

    public static String util_getToplogySinkName(String topicName) {
        return ChargeAssessmentEvent.class.getSimpleName() + "Sink-" + topicName;
    }
}
