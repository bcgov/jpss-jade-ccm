
package ccm;

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
public class TransformedAccessDedupTopology {
    Logger LOG = LoggerFactory.getLogger(TransformedAccessDedupTopology.class);

    private static final String STORE_NAME = "transform-store";

    @Inject
    @ConfigProperty(name = "custom.processor.name")
    String processorName;

    @Produces
    public Topology buildTopology() {
        // log the custom property
        LOG.info("Processor name: {}.", processorName);

        StreamsBuilder builder = new StreamsBuilder();
        
        // Define the state store.
        StoreBuilder<KeyValueStore<String, String>> dedupStoreBuilder = 
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(STORE_NAME),
                org.apache.kafka.common.serialization.Serdes.String(),
                org.apache.kafka.common.serialization.Serdes.String()
            );
        
        // Add the state store to the topology.
        builder.addStateStore(dedupStoreBuilder);

        // Define the source topic from which messages are consumed.
        KStream<String, String> sourceStream = builder.stream("transform-user-accesses", Consumed.with(Serdes.String(), Serdes.String()));

        // Process the messages using the AccessDedupTransformer.
        KStream<String, String> transformedStream = sourceStream.transform(AccessDedupTransformer::new, Named.as(processorName), STORE_NAME);

        // Send deduplicated messages to another topic.
        transformedStream.to("transform-user-accesses-dedup", Produced.with(Serdes.String(), Serdes.String()));

        return builder.build();
    }
}
