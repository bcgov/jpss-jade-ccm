package jpss.jade.ccm;

import java.util.Properties;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jpss.jade.ccm.AccessDedupProcessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AccessDedupProcessorTest {

    private static final String INPUT_TOPIC = "user-accesses";
    private static final String OUTPUT_TOPIC = "case-accesses";

    private TopologyTestDriver testDriver;
    private TestInputTopic<String, String> inputTopic;
    private TestOutputTopic<String, String> outputTopic;

    @BeforeEach
    public void setUp() {
        Topology topology = new Topology();

        topology.addSource("source", "user-accesses")
                .addProcessor("processor", (ProcessorSupplier<String, String>) AccessDedupProcessor::new, "source")
                .addStateStore(Stores.keyValueStoreBuilder(Stores.inMemoryKeyValueStore("deduplication-store"), Serdes.String(), Serdes.String()).withLoggingDisabled(), "processor")  // Logging disabled for testing
                .addSink("sink", "case-accesses", "processor");

        Properties props = new Properties();

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE);

        testDriver = new TopologyTestDriver(topology, props);

        //inputTopic = testDriver.createInputTopic("user-accesses", Serdes.String().serializer(), Serdes.String().serializer());
        //outputTopic = testDriver.createOutputTopic("case-accesses", Serdes.String().deserializer(), Serdes.String().deserializer());
        inputTopic = testDriver.createInputTopic(INPUT_TOPIC, new StringSerializer(), new StringSerializer());
        outputTopic = testDriver.createOutputTopic(OUTPUT_TOPIC, new StringDeserializer(), new StringDeserializer());
    }

    @Test
    public void testDeduplication() {
        // Send some events with duplicates
        inputTopic.pipeInput("key1", "key1");
        inputTopic.pipeInput("key2", "key2");
        inputTopic.pipeInput("key1", "key1");  // Duplicate
        inputTopic.pipeInput("", "");  // End event

        assertEquals("key1", outputTopic.readValue());
        assertEquals("key2", outputTopic.readValue());

        // Expect more events, as the duplicate was filtered out
        assertTrue(outputTopic.isEmpty());

        testDriver.close();
    }

    @Test
    public void testTopicsCreated() {
        // Send some events with duplicates
        assertNotNull(inputTopic);
        assertNotNull(outputTopic);

        testDriver.close();
    }
}
