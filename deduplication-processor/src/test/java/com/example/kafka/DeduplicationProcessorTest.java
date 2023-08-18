package com.example.kafka;

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
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.example.kafka.DeduplicationProcessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DeduplicationProcessorTest {

    private static final String INPUT_TOPIC = "user-accesses";
    private static final String OUTPUT_TOPIC = "case-accesses";

    private TopologyTestDriver testDriver;
    private TestInputTopic<String, String> inputTopic;
    private TestOutputTopic<String, String> outputTopic;

    @Before
    public void setUp() {
        Topology topology = new Topology();

        topology.addSource("source", "user-accesses")
                .addProcessor("processor", (ProcessorSupplier<String, String>) DeduplicationProcessor::new, "source")
                .addStateStore(Stores.keyValueStoreBuilder(Stores.inMemoryKeyValueStore("deduplication-store"), Serdes.String(), Serdes.String()).withLoggingDisabled(), "processor")  // Logging disabled for testing
                .addSink("sink", "case-accesses", "processor");

        testDriver = new TopologyTestDriver(topology, new Properties());

        //inputTopic = testDriver.createInputTopic("user-accesses", Serdes.String().serializer(), Serdes.String().serializer());
        //outputTopic = testDriver.createOutputTopic("case-accesses", Serdes.String().deserializer(), Serdes.String().deserializer());
        inputTopic = testDriver.createInputTopic(INPUT_TOPIC, new StringSerializer(), new StringSerializer());
        outputTopic = testDriver.createOutputTopic(OUTPUT_TOPIC, new StringDeserializer(), new StringDeserializer());

    }

    @Test
    public void testDeduplication() {
        // Send some events with duplicates
        inputTopic.pipeInput("key1", "value1");
        inputTopic.pipeInput("key2", "value2");
        inputTopic.pipeInput("key1", "value1");  // Duplicate
        inputTopic.pipeInput("", "");  // End event

        assertEquals("value1", outputTopic.readValue());
        assertEquals("value2", outputTopic.readValue());
        // No more events, as the duplicate was filtered out
        assertNull(outputTopic.readValue());

        testDriver.close();
    }
}
