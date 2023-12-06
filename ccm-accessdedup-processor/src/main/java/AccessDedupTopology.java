import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.apache.kafka.streams.state.Stores;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.KeyValueStore;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ccm.models.common.event.CaseUserEvent;
import io.vertx.codegen.format.Case;

@ApplicationScoped
public class AccessDedupTopology {
    Logger LOG = LoggerFactory.getLogger(AccessDedupTopology.class);

    @Inject
    @ConfigProperty(name = "quarkus.kafka-streams.application-id")
    String quarkusKafkaStreamsAppName;

    @Inject
    @ConfigProperty(name = "ccm.topic.chargeassessments.name")
    String chargeAssessmentsTopicName;
    
    @Inject
    @ConfigProperty(name = "ccm.topic.bulk-caseusers.name")
    String bulkCaseUsersTopicName;
    
    @Inject
    @ConfigProperty(name = "ccm.topic.caseuser-errors.name")
    String caseUserErrorsTopicName;
    
    @Inject
    @ConfigProperty(name = "ccm.topic.kpis.name")
    String kpisTopicName;

    @Inject
    @ConfigProperty(name = "ccm.store.caseaccesssync.name")
    String caseAccessSyncStoreName;

    @Inject
    @ConfigProperty(name = "ccm.application.name.propercase")
    String applicationNameProperCase;

    @Produces
    public Topology buildTopology() {
        LOG.info("Quarkus Kafka Streams application name: {}.", quarkusKafkaStreamsAppName);
        LOG.info("CaseAccessSync store name: {}.", caseAccessSyncStoreName);

        Topology topology = new Topology();

        // Create the case user event handling topology
        //// Define the case user event source
        topology.addSource(CaseUserEventHandler.util_getTopologySourceName(), 
            Serdes.String().deserializer(), Serdes.String().deserializer(), 
            bulkCaseUsersTopicName);
        //// Add case user event custom processor
        topology.addProcessor(CaseUserEventHandler.class.getSimpleName(), 
            CaseUserEventHandler::new, CaseUserEventHandler.util_getTopologySourceName());
        //// Define the state store
        StoreBuilder<KeyValueStore<String, String>> caseAccessSyncStoreBuilder =
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(caseAccessSyncStoreName),
                Serdes.String(),
                Serdes.String());
        topology.addStateStore(caseAccessSyncStoreBuilder, 
            CaseUserEventHandler.util_getTopologyProcessorName());
        //// Add case user event sink processors
        topology.addSink(CaseUserEventHandler.util_getToplogySinkName(caseUserErrorsTopicName), caseUserErrorsTopicName, 
            Serdes.String().serializer(), Serdes.String().serializer(), 
            CaseUserEventHandler.util_getTopologyProcessorName());
        topology.addSink(CaseUserEventHandler.util_getToplogySinkName(chargeAssessmentsTopicName), chargeAssessmentsTopicName, 
            Serdes.String().serializer(), Serdes.String().serializer(), 
            CaseUserEventHandler.util_getTopologyProcessorName());
        topology.addSink(CaseUserEventHandler.util_getToplogySinkName(kpisTopicName), kpisTopicName, 
            Serdes.String().serializer(), Serdes.String().serializer(), 
            CaseUserEventHandler.util_getTopologyProcessorName());

        // Create the charge assessment event handling topology
        //// Define the charge assessment event source
        topology.addSource(ChargeAssessmentEventHandler.util_getTopologySourceName(), 
            Serdes.String().deserializer(), Serdes.String().deserializer(), 
            chargeAssessmentsTopicName);
        //// Add charge assessment event custom processor
        topology.addProcessor(ChargeAssessmentEventHandler.class.getSimpleName(), 
            ChargeAssessmentEventHandler::new, ChargeAssessmentEventHandler.util_getTopologySourceName());
        //// Add charge assessment event sink processor
        topology.addSink(ChargeAssessmentEventHandler.util_getToplogySinkName(kpisTopicName), kpisTopicName, 
            Serdes.String().serializer(), Serdes.String().serializer(), 
            ChargeAssessmentEventHandler.util_getTopologyProcessorName());

        return topology;
    }
}
