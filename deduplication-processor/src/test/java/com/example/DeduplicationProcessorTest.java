package com.example.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.streams.StreamsBuilder;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.test.ConsumerRecordFactory;
import org.apache.kafka.streams.test.OutputVerifier;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

public class DeduplicationProcessorTest {

    private TopologyTestDriver testDriver;
    private ConsumerRecordFactory<String, String> factory;
    private TestInputTopic<String, String> inputTopic;
    private TestOutputTopic<String, String> outputTopic;

    @BeforeEach
    public void setUp() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "dummy:1234");
        properties.put("application.id", "test");

        //DeduplicationProcessor processor = new DeduplicationProcessor();
        //testDriver = new TopologyTestDriver(processor.buildTopology(), properties);

        DeduplicationTopology deduplicationTopology = new DeduplicationTopology();
        Topology topology = deduplicationTopology.createTopology();
        testDriver = new TopologyTestDriver(topology, props);


        factory = new ConsumerRecordFactory<>(Serdes.String().serializer(), Serdes.String().serializer());

        // Create topics in the test driver
        inputTopic = testDriver.createInputTopic("user-accesses", new StringSerializer(), new StringSerializer());
        outputTopic = testDriver.createOutputTopic("case-accesses", new StringDeserializer(),
                new StringDeserializer());
    }

    @AfterEach
    public void tearDown() {
        testDriver.close();
    }

    @Test
    public void testDeduplication() {
        // Send two identical records with the same key
        inputTopic.pipeInput("user1", "case1");
        inputTopic.pipeInput("user2", "case1");
        inputTopic.pipeInput("user3", "case2");

        // Verify that only one record is produced (or whatever your expected behavior
        // is)
        ProducerRecord<String, String> outputRecord = testDriver.readOutput("case-accesses",
                new StringDeserializer(), new StringDeserializer());
        OutputVerifier.compareKeyValue(outputRecord, "case1", "value1");
        OutputVerifier.compareKeyValue(outputRecord, "case2", "value1");

    }
}
