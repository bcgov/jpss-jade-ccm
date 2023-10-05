
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
public class AccessDedupTopology {
    Logger LOG = LoggerFactory.getLogger(AccessDedupTopology.class);

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
                Stores.persistentKeyValueStore("store"),
                org.apache.kafka.common.serialization.Serdes.String(),
                org.apache.kafka.common.serialization.Serdes.String()
            );
        
        // Add the state store to the topology.
        builder.addStateStore(dedupStoreBuilder);

        // Define the source topic from which messages are consumed.
        KStream<String, String> sourceStream = builder.stream("user-accesses", Consumed.with(Serdes.String(), Serdes.String()));

        // Process the messages using the AccessDedupProcessor.
        sourceStream.process(AccessDedupProcessor::new, Named.as(processorName), "store");

        // Send deduplicated messages to another topic.
        sourceStream.to("user-accesses-dedup", Produced.with(Serdes.String(), Serdes.String()));

        return builder.build();
    }
}
