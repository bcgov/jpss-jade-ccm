// To run this integration use:
// kamel run CcmJustinUtilityAdapter.java --property file:application.properties --profile openshift
// 
// recover the service location. If you're running on minikube, minikube service platform-http-server --url=true
// curl -d '{}' http://ccm-justin-utility-adapter/courtFileCreated
//

// camel-k: language=java
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-kafka:camel-quarkus-jsonpath:camel-jackson
// camel-k: trait=jvm.classpath=/etc/camel/resources/

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;

class JustinEventListProcessor implements Processor {

  // example: https://github.com/apache/camel-examples/tree/main/examples/transformer-demo/src/main/java/org/apache/camel/example/transformer/demo
  // example: https://www.baeldung.com/java-camel-jackson-json-array
  //   Unmarshalling a JSON Array using camel-jackson

  // example: https://www.programcreek.com/java-api-examples/?api=org.apache.camel.component.jackson.JacksonDataFormat
  //   Marshalling and unmarshalling Json and Pojo

  // example: https://developers.redhat.com/articles/2021/11/24/normalize-web-services-camel-k-and-atlasmap-part-1#camel_k_implementation_overview
  //   Normalize web services with Camel K and AtlasMap, Part 1
  // example: https://developers.redhat.com/articles/2021/11/26/normalize-web-services-camel-k-and-atlasmap-part-2
  //   Normalize web services with Camel K and AtlasMap, Part 2

  @Override
  public void process(Exchange exchange) throws Exception {
    // Insert code that gets executed *before* delegating
    // to the next processor in the chain.

    String body = exchange.getIn().getBody(String.class);
    String exchangeId = exchange.getExchangeId();
    String messageId = exchange.getIn().getMessageId();
    JustinEventData jel = exchange.getIn().getBody(JustinEventData.class);

    System.out.println("Received message. Exchange Id = " + exchangeId + "; Message Id = " + messageId);
    System.out.println("Body length: " + body.length());
    System.out.println("Event list size: " + jel);

    if (body != null && body.contains("Kaboom")) {
      throw new Exception("Illegal data found!");
    }

    // Unmarshalling a JSON Array Using camel-jackson
    // https://www.baeldung.com/java-camel-jackson-json-array

    // Intro to the Jackson ObjectMapper
    // https://www.baeldung.com/jackson-object-mapper-tutorial

    // Quarkus Jsonpath
    // https://camel.apache.org/camel-quarkus/2.9.x/reference/extensions/jsonpath.html
    // https://camel.apache.org/components/3.16.x/languages/jsonpath-language.html)

    exchange.getMessage().setBody("OK");
  }
}

public class CcmJustinUtilityAdapter extends RouteBuilder {
  @Override
  public void configure() throws Exception {
    from("platform-http:/courtFileCreated?httpMethodRestrict=POST")
    .routeId("courtFileCreated")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .log("body (before unmarshalling): '${body}'")
    .unmarshal().json()
    .transform(simple("{\"number\": \"${body[number]}\", \"status\": \"created\", \"sensitive_content\": \"${body[sensitive_content]}\", \"public_content\": \"${body[public_content]}\", \"created_datetime\": \"${body[created_datetime]}\"}"))
    .log("body (after unmarshalling): '${body}'")
    //
    // BCPSDEMS-143 Kafka routing below commented out until deployment issue is resolved
    .to("kafka:{{kafka.topic.name}}");

    from("platform-http:/v1/health?httpMethodRestrict=GET")
    .routeId("healthCheck")
    .removeHeaders("CamelHttp*")
    .log("/v1/health request received")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("https://dev.jag.gov.bc.ca/ords/devj/justinords/dems/v1/health");

    // https://tomd.xyz/camel-transformation/
    // Youtube (30 min): Getting started with Apache Camel on Quarkus - https://www.youtube.com/watch?v=POWsZnGhVHM
    // https://developers.redhat.com/articles/2021/05/17/integrating-systems-apache-camel-and-quarkus-red-hat-openshift#
    //
    JsonDataFormat json = new JsonDataFormat(JsonLibrary.Jackson);
    json.setUnmarshalType(JustinEventList.class);

    from("timer://simpleTimer?period={{notification.check.frequency}}")
    //from("file:/etc/camel/resources/?fileName=getEventBatch.json&noop=true&idempotent=true")
    //from("file:/etc/camel/resources/?fileName=getEventData.json&noop=true&idempotent=true")
    .routeId("getJUSTINNotifications")
    .log("checking for new notifications...")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("https://dev.jag.gov.bc.ca/ords/devj/justinords/dems/v1/inProgressEvents")
    // .process(new EventsProcessor())
    .log("In progress events from JUSTIN:")
    // .unmarshal(new JsonDataFormat(JustinEventList.class))
    // .unmarshal().json(JsonLibrary.Jackson, JustinEventList.class)
    // .unmarshal(json)
    //       .unmarshal().json(JsonLibrary.Jackson, JustinEventList.class)
    // .choice()
    // .when().jsonpath("events[0].appl_application_cd", true)
    // .otherwise()
    //   .log("Unknown event found.")
    .log("Routing to \"direct:process\"")
    .to("direct:process");

    JustinEventListProcessor jp = new JustinEventListProcessor();

    from("direct:process")
    .log("In direct:process")
    .process(jp);
  }
}

// https://stackoverflow.com/questions/40756027/apache-camel-json-marshalling-to-pojo-java-bean
class CourtCaseCreated {
  public String number;
  public String created_datetime;
}