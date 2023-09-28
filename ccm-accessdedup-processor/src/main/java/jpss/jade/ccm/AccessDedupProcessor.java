package jpss.jade.ccm;

import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;

import javax.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessDedupProcessor extends AbstractProcessor<String, String> {

    private static final Logger LOG = LoggerFactory.getLogger(AccessDedupProcessor.class);

    private KeyValueStore<String, String> accessdedupStore;
    private static final String STORE_NAME = "store";

    @Override
    @SuppressWarnings("unchecked")
    public void init(ProcessorContext context) {
        super.init(context);
        // Initialize the state store.
        this.accessdedupStore = (KeyValueStore<String, String>) context.getStateStore(STORE_NAME);
        this.context = context;

        // Log the custom property

        // Dependency injection doesn't work in Kafka Streams.  We have to use the ConfigProvider.
        String property = ConfigProvider.getConfig().getValue("custom.processor.name", String.class);

        LOG.info("Processor name: {}.", property);
    }

    @Override
    public void process(String key, String value) {
        if (key == null || key.isEmpty() || key.equalsIgnoreCase("END_OF_BATCH")) {
            // End of the batch is detected
            LOG.info("End of the batch is detected.");

            accessdedupStore.all().forEachRemaining(keyValue -> {
                LOG.info("Forwarding (Key,Value) = ({},{}).", keyValue.key);
                context().forward(keyValue.key, keyValue.value + " - " + "flushed");
            });

            accessdedupStore.all().forEachRemaining(keyValue -> {
                LOG.info("Deleting (Key,Value) = ({},{}).", keyValue.key);
                accessdedupStore.delete(keyValue.key);
            });
            
            return;
        }

        // Only forward the message if the key is found in the deduplication store.
        if (accessdedupStore.get(key) == null) {
            LOG.info("Forwarding message with key: {}; new key in the batch.", key);
            accessdedupStore.put(key, key);
            context().forward(key, value);
        } else {
            LOG.info("Skipping message with key: {}; key already found in the batch.", key);
            context().forward("Duplicate " + key, value);
        }
    }

    @Override
    public void close() {
        // No additional cleanup is required as the store is managed by Kafka Streams.
    }
}
