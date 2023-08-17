package com.example.kafka;

import io.quarkus.kafka.client.serialization.JsonbSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.Topology;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.inject.Inject;
import java.util.Properties;

@ApplicationScoped
public class DeduplicationProcessor {

    @ConfigProperty(name = "quarkus.kafka-streams.bootstrap-servers")
    String bootstrapServers;

    @ConfigProperty(name = "quarkus.kafka-streams.application-id")
    String applicationId;

    private KafkaStreams streams;

    public void run() {
        StreamsBuilder builder = new StreamsBuilder();

        // Assuming the event has a String key and a Json value
        KTable<String, String> deduplicatedTable = builder.table(
            "<YOUR_TOPIC_NAME>",
            Consumed.with(Serdes.String(), new JsonbSerde<>(String.class))
        );

        // TODO: Further processing or sending to another topic

        Properties properties = new Properties();
        properties.put("bootstrap.servers", bootstrapServers);
        properties.put("application.id", applicationId);
        
        streams = new KafkaStreams(builder.build(), properties);
        streams.start();
    }

    void onStop(@Disposes KafkaStreams streams) {
        streams.close();
    }

    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();
        // Your Kafka Streams logic here...
        return builder.build();
    }
}

