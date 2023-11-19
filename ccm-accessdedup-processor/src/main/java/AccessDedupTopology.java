import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
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
        // Log the applicaiton name.
        LOG.info("Quarkus Kafka Streams application name: {}.", quarkusKafkaStreamsAppName);

        LOG.info("CaseAccessSync store name: {}.", caseAccessSyncStoreName);

        // Create the topology.
        StreamsBuilder builder = new StreamsBuilder();

/*         // Define the kpi store.
        StoreBuilder<KeyValueStore<String, String>> kpiStoreBuilder = 
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(KPI_STORE_NAME),
                org.apache.kafka.common.serialization.Serdes.String(),
                org.apache.kafka.common.serialization.Serdes.String()
            );

        // Add the kpi state store to the topology.
        builder.addStateStore(kpiStoreBuilder); */
        
        // Define the deduplication store.
        StoreBuilder<KeyValueStore<String, String>> caseAccessSyncStoreBuilder = 
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(caseAccessSyncStoreName),
                org.apache.kafka.common.serialization.Serdes.String(),
                org.apache.kafka.common.serialization.Serdes.String()
            );
        
        // Add the dedup state store to the topology.
        builder.addStateStore(caseAccessSyncStoreBuilder);

        // Define the source topic from which messages are consumed.
        KStream<String, String> sourceStream = builder.stream(bulkCaseUsersTopicName, Consumed.with(Serdes.String(), Serdes.String()));

        // Process the messages using the CaseAccessSyncTransformer.
        KStream<String, String> transformedStream = sourceStream.transform(CaseAccessSyncTransformer::new, Named.as(CaseAccessSyncTransformer.class.getSimpleName()), caseAccessSyncStoreName);

        // Send consolidated event messsages to charge assessment topic.
        transformedStream.to(chargeAssessmentsTopicName, Produced.with(Serdes.String(), Serdes.String()));

        // Send kpi event messages to kpis topic.
        //transformedStream.to(kpisTopicName, Produced.with(Serdes.String(), Serdes.String()));
        //builder.addSink(kpisTopicName, kpisTopicName, Serdes.String().serializer(), Serdes.String().serializer(), transformedStream);

        Topology topology = builder.build();

        //topology.addSink(chargeAssessmentsTopicName, chargeAssessmentsTopicName, Serdes.String().serializer(), Serdes.String().serializer(), transformedStream);
        //topology.addSink(kpisTopicName, kpisTopicName, Serdes.String().serializer(), Serdes.String().serializer(), transformedStream);

        return topology;
    }
}
