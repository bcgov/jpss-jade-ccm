// To run this integration use:
// kamel run CcmNotificationService.java --property file:application.properties --profile openshift
// 

// camel-k: language=java
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-kafka

//import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public class CcmNotificationService extends RouteBuilder {
  @Override
  public void configure() throws Exception {
    from("kafka:{{kafka.topic.courtcases.name}}")
    .routeId("courtcase_kpis")
    .log("Message received from Kafka : ${body}")
    .log("    on the topic ${headers[kafka.TOPIC]}")
    .log("    on the partition ${headers[kafka.PARTITION]}")
    .log("    with the offset ${headers[kafka.OFFSET]}")
    .log("    with the key ${headers[kafka.KEY]}")
    .unmarshal().json()
    .transform(simple("{\"type\": \"courtcase\", \"number\": \"${body[number]}\", \"status\": \"created\", \"public_content\": \"${body[public_content]}\", \"created_datetime\": \"${body[created_datetime]}\"}"))
    .log("body (after unmarshalling): '${body}'")
    .to("kafka:{{kafka.topic.kpis.name}}");
  }
}

// https://stackoverflow.com/questions/40756027/apache-camel-json-marshalling-to-pojo-java-bean
class CourtFileApproved {
  public String number;
  public String created_datetime;
  public String approved_datetime;
}