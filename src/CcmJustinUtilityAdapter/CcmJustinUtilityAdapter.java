// To run this integration use:
// kamel run CcmJustinUtilityAdapter.java --property file:application.properties --profile openshift
// 
// recover the service location. If you're running on minikube, minikube service platform-http-server --url=true
// curl -d '{}' http://ccm-justin-utility-adapter/courtFileCreated
//

// camel-k: language=java
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-kafka:camel-quarkus-jsonpath

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
//import org.apache.camel.model.dataformat.JsonDataFormat;

class EventsProcessor implements Processor {

  public EventsProcessor() {
  }

  public void process(Exchange exchange) throws Exception {
    // Insert code that gets executed *before* delegating
    // to the next processor in the chain.

    String body = exchange.getIn().getBody(String.class);

    System.out.println("Received message: " + body);

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

// class JustinEventData {
//   String data_element_nm;
//   String data_value_txt;
// }

// class JustinEvent {
//   Integer event_message_id;
//   String appl_application_cd;
//   String message_event_type_cd;
//   String event_dtm;
//   JustinEventData event_data;
// }

// class JustinEventList {
//   // Unmarshalling a JSON Array Using camel-jackson
//   // https://www.baeldung.com/java-camel-jackson-json-array

//   JustinEvent[] events;
// }

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
    JsonDataFormat json = new JsonDataFormat(JsonLibrary.Jackson);

    from("timer://simpleTimer?period={{notification.check.frequency}}")
    .routeId("getNotifications")
    .log("checking for new notifications...")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("https://dev.jag.gov.bc.ca/ords/devj/justinords/dems/v1/inProgressEvents")
    // .process(new EventsProcessor())
    .log("In progress events from JUSTIN: ${body}")
    // .unmarshal(new JasonDataFormat(JustinEventList.class))
    ;
  }
}

// https://stackoverflow.com/questions/40756027/apache-camel-json-marshalling-to-pojo-java-bean
class CourtCaseCreated {
  public String number;
  public String created_datetime;
}