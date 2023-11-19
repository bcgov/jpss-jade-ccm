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

import ccm.models.common.versioning.Version;
import io.vertx.core.json.JsonObject;

public class CaseUserEventKpiStartProcessor extends AbstractProcessor<String, String> {

    private static final Logger LOG = LoggerFactory.getLogger(CaseUserEventKpiStartProcessor.class);

    private KeyValueStore<String, String> kpiStore;

    private static final String KPI_STORE_NAME = "kpi-store";

    @Override
    @SuppressWarnings("unchecked")
    public void init(ProcessorContext context) {
        super.init(context);
        // Initialize the state store.
        this.kpiStore = (KeyValueStore<String, String>) context.getStateStore(KPI_STORE_NAME);
        this.context = context;

        LOG.info("ccm model version: {}.", Version.V1_0);
    }

    @Override
    public void process(String key, String value) {
        if (key == null || key.isEmpty() || key.equalsIgnoreCase("END_OF_BATCH")) {
            return;
        }

        kpiStore.put("kpi: " + key, key + " event processing started.");

        return;
    }

    @Override
    public void close() {
        // No additional cleanup is required as the store is managed by Kafka Streams.
    }
}