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
    @ConfigProperty(name = "ccm.topic.chargeassessment-errors.name")
    String chargeAssessmentErrorsTopicName;
    
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

    @Produces
    public Topology buildTopology() {
        LOG.info("Quarkus Kafka Streams application name: {}.", quarkusKafkaStreamsAppName);
        LOG.info("CaseAccessSync store name: {}.", caseAccessSyncStoreName);

        Topology topology = new Topology();

        // Define the source processor
        topology.addSource("Source", Serdes.String().deserializer(), Serdes.String().deserializer(), bulkCaseUsersTopicName);

        // Add custom processor
        topology.addProcessor("Processor", CaseAccessSyncProcessor::new, "Source");

        // Define the state store
        StoreBuilder<KeyValueStore<String, String>> caseAccessSyncStoreBuilder =
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(caseAccessSyncStoreName),
                Serdes.String(),
                Serdes.String()
            );
        topology.addStateStore(caseAccessSyncStoreBuilder, "Processor");

        // Add sink processors
        topology.addSink("SinkFor-" + chargeAssessmentsTopicName, chargeAssessmentsTopicName, 
            Serdes.String().serializer(), Serdes.String().serializer(), 
            "Processor");
        topology.addSink("SinkFor-" + kpisTopicName, kpisTopicName, 
            Serdes.String().serializer(), Serdes.String().serializer(), 
            "Processor");

        return topology;
    }
}
