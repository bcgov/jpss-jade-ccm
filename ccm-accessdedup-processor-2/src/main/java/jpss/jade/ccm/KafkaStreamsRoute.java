package jpss.jade.ccm;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.streams.KafkaStreamsConstants;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.KeyValueIterator;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class KafkaStreamsRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        
        from("kafka:inputTopic")
            .process(exchange -> {
                String messageKey = exchange.getIn().getHeader("kafka.KEY", String.class);
                String messageValue = exchange.getIn().getBody(String.class);

                KeyValueStore<String, String> stateStore = 
                    exchange.getIn().getHeader(KafkaStreamsConstants.KAFKA_STREAMS_STATE_STORE, KeyValueStore.class);

                // Check if the message value contains "important" and if the key is not already in the state store
                if (messageValue.contains("important") && stateStore.get(messageKey) == null) {
                    stateStore.put(messageKey, messageValue);
                }

                // If the event is marked "end", transmit all unique events from the state store
                if ("end".equals(messageValue)) {
                    try (KeyValueIterator<String, String> allEvents = stateStore.all()) {
                        while (allEvents.hasNext()) {
                            org.apache.kafka.streams.KeyValue<String, String> event = allEvents.next();
                            // Send to output topic
                            exchange.getContext().createProducerTemplate().sendBodyAndHeader("kafka:outputTopic", event.value, "kafka.KEY", event.key);
                        }
                    }
                }
            });
    }
}
