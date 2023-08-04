import io.quarkus.kafka.client.serialization.ObjectMapperSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.Properties;

@ApplicationScoped
public class UserProvisionProcessor {

    @Inject
    Properties kafkaStreamsProperties;

    @Produces
    public Properties produceKafkaProperties() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("application.id", "user-provision-processor");
        properties.put("default.key.serde", Serdes.String().getClass().getName());
        properties.put("default.value.serde", Serdes.String().getClass().getName());
        return properties;
    }

    @Produces
    public KafkaStreams buildKafkaStreams() {

        StreamsBuilder builder = new StreamsBuilder();
        ObjectMapperSerde<String> pageViewEventSerde = new ObjectMapperSerde<>(String.class);

        KStream<String, String> pageViews = builder.stream("page-views", Consumed.with(Serdes.String(), pageViewEventSerde));
        KTable<String, Long> userPageViewCounts = pageViews
            .groupBy((userId, pageView) -> userId, Grouped.with(Serdes.String(), pageViewEventSerde))
            .count(Materialized.as("counts"));

        return new KafkaStreams(builder.build(), kafkaStreamsProperties);
    }

    void onStop(@Disposes KafkaStreams streams) {
        streams.close();
    }
}
