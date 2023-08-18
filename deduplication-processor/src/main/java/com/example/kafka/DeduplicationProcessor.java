package com.example.kafka;

import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;

public class DeduplicationProcessor extends AbstractProcessor<String, String> {

    private KeyValueStore<String, Boolean> deduplicationStore;
    private static final String STORE_NAME = "deduplication-store";

    @Override
    @SuppressWarnings("unchecked")
    public void init(ProcessorContext context) {
        super.init(context);
        // Initialize the state store.
        this.deduplicationStore = (KeyValueStore<String, Boolean>) context.getStateStore(STORE_NAME);
    }

    @Override
    public void process(String key, String value) {
        if (key.isEmpty()) {
            // If the key is empty, it's considered the end of the batch.
            // We'll clear the state store for the next batch.
            deduplicationStore.all().forEachRemaining(keyValue -> deduplicationStore.delete(keyValue.key));
            return;
        }

        // If this key is not in the deduplication store, forward the record.
        if (deduplicationStore.get(key) == null) {
            deduplicationStore.put(key, true);
            context().forward(key, value);
        }
    }

    @Override
    public void close() {
        // No additional cleanup is required as the store is managed by Kafka Streams.
    }
}
