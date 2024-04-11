package ccm.utils;

import java.util.StringTokenizer;

public class KafkaComponentUtils {
    public static String extractOffsetFromRecordMetadata(Object o) {
        // Extract the offset from response header, produced by the Camel Kafka compoenent
        // after publishing a Kafka record for a given topic.

        // assume o = simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}")

        // https://kafka.apache.org/30/javadoc/org/apache/kafka/clients/producer/RecordMetadata.html
        
        // Expected format: "[topic-partition@offset]"
        // Example: "[users-0@301]"

        String offset = null;

        if (o == null) {
            return offset;
        }

        String recordMetadata = o.toString();

        StringTokenizer tokenizer = new StringTokenizer(recordMetadata, "[@]");
        if (tokenizer.countTokens() == 2) {
            // ignore first token
            tokenizer.nextToken();

            // this is the metadata we are looking for
            offset = tokenizer.nextToken();
        }

        return offset;
    }
    public static String extractPartitionFromRecordMetadata(Object o) {
        // Extract the partition from response header, produced by the Camel Kafka compoenent
        // after publishing a Kafka record for a given topic.

        // assume o = simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}")

        // https://kafka.apache.org/30/javadoc/org/apache/kafka/clients/producer/RecordMetadata.html
        
        // Expected format: "[topic-partition@offset]"
        // Example: "[users-0@301]"

        String partition = null;

        if (o == null) {
            return partition;
        }

        String recordMetadata = o.toString();

        StringTokenizer tokenizer = new StringTokenizer(recordMetadata, "[@]");
        if (tokenizer.countTokens() >= 1) {
            partition = tokenizer.nextToken();
            tokenizer = new StringTokenizer(partition, "-");
            if(tokenizer.countTokens() >= 1) {
                while(tokenizer.hasMoreTokens()) {
                    partition = tokenizer.nextToken();
                }
            }
        }

        return partition;
    }
}
