package com.example.kafka;

import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;

import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class AccessDedupProcessor extends AbstractProcessor<String, String> {

    private KeyValueStore<String, String> accessdedupStore;
    private static final String STORE_NAME = "ccm-accessdeup-store";

    private Serializer<String> stringSerializer;
    private Deserializer<String> stringDeserializer;

    @Inject
    @ConfigProperty(name = "custom.property")
    String customProperty;

    @Override
    @SuppressWarnings("unchecked")
    public void init(ProcessorContext context) {
        super.init(context);
        // Initialize the state store.
        this.accessdedupStore = (KeyValueStore<String, String>) context.getStateStore(STORE_NAME);
        this.context = context;
        this.stringSerializer = Serdes.String().serializer();
        this.stringDeserializer = Serdes.String().deserializer();

        System.out.println("customProperty: " + customProperty);
    }

    @Override
    public void process(String key, String value) {
        if (key.isEmpty()) {
            // If the key is empty, it's considered the end of the batch.
            // We'll clear the state store for the next batch.
            accessdedupStore.all().forEachRemaining(keyValue -> accessdedupStore.delete(keyValue.key));
            return;
        }

        // If this key is not in the deduplication store, forward the record.
        if (accessdedupStore.get(key) == null) {
            accessdedupStore.put(key, key);
            context().forward(key, value);
        }
    }

    @Override
    public void close() {
        // No additional cleanup is required as the store is managed by Kafka Streams.
    }
}
