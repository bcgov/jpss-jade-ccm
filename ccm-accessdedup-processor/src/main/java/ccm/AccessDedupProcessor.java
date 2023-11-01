package ccm;

import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.kstream.Transformer;

import javax.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ccm.models.common.versioning.Version;
import io.vertx.core.json.JsonObject;

public class AccessDedupProcessor implements Transformer<String, String, KeyValue<String, String>> {

    private static final Logger LOG = LoggerFactory.getLogger(AccessDedupProcessor.class);

    private KeyValueStore<String, String> accessdedupStore;
    private static final String STORE_NAME = "store";

    private ProcessorContext context;

    @Override
    @SuppressWarnings("unchecked")
    public void init(ProcessorContext context) {
        // Initialize the state store.
        this.accessdedupStore = (KeyValueStore<String, String>) context.getStateStore(STORE_NAME);
        this.context = context;

        // Log the custom property

        // Dependency injection doesn't work in Kafka Streams.  We have to use the ConfigProvider.
        String property = ConfigProvider.getConfig().getValue("custom.processor.name", String.class);

        LOG.info("Processor name: {}.", property);
        LOG.info("ccm model version: {}.", Version.V1_0);
    }

    @Override
    public KeyValue<String, String> transform(String key, String value) {
        if (key == null || key.isEmpty() || key.equalsIgnoreCase("END_OF_BATCH")) {
            // End of the batch is detected
            LOG.info("End of the batch is detected.");

            accessdedupStore.all().forEachRemaining(keyValue -> {
                LOG.info("Forwarding (Key,Value) = ({},{}).", keyValue.key, keyValue.value);
                context.forward(keyValue.key, keyValue.value);
            });

            accessdedupStore.all().forEachRemaining(keyValue -> {
                LOG.info("Deleting (Key,Value) = ({},{}).", keyValue.key, keyValue.value);
                accessdedupStore.delete(keyValue.key);
            });
            
            return null;
        }

        // Only forward the message if the key is found in the deduplication store.
        if (accessdedupStore.get(key) == null) {
            try {
                JsonObject json = new JsonObject(value);
                String message = json.getString("message", "");
                LOG.info("Storing message with key: {}; new key in the batch. Message = {}", key, message);
                accessdedupStore.put(key, value);
                //context().forward(key, value);
            } catch (Exception e) {
                LOG.error("Error processing message with key: {}.", key, e);
            }
        } else {
            LOG.info("Skipping message with key: {}; key already found in the batch.", key);
        }

        return null;
    }

    public void close() {
        // No additional cleanup is required as the store is managed by Kafka Streams.
    }
}
