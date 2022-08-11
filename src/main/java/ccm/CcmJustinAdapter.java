package ccm;
// To run this integration use:
// kamel run CcmJustinAdapter.java --property file:application.properties --profile openshift
// 
// recover the service location. If you're running on minikube, minikube service platform-http-server --url=true
// curl -d '{}' http://ccm-justin-adapter/courtFileCreated
//

// camel-k: language=java
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-kafka:camel-quarkus-jsonpath:camel-jackson:camel-splunk-hec
// camel-k: trait=jvm.classpath=/etc/camel/resources/ccm-models.jar

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
//import org.apache.camel.model.;

import ccm.models.system.justin.JustinEventBatch;
import ccm.models.business.BusinessCourtCaseEvent;
import ccm.models.system.justin.JustinEvent;

class JustinEventBatchProcessor implements Processor {

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

    String exchangeId = exchange.getExchangeId();
    String messageId = exchange.getIn().getMessageId();

    JustinEventBatch jeb = exchange.getIn().getBody(JustinEventBatch.class);

    int batchSize = jeb.getEvents().size();
    System.out.println("Retrieved " + batchSize + (batchSize == 1 ? " record " : " records ") + "from JUSTIN Interface.  JADE-CCM Exchange Id = " + exchangeId + "; JADE-CCM Message Id = " + messageId);
    System.out.println("Total number of JUSTIN events retrieved: " + jeb.getEvents().size());

    if (jeb.getEvents().size() > 0) {
      for (JustinEvent e: jeb.getEvents()) {
        System.out.print("Processing JUSTIN event " + e.getEvent_message_id() + " (" + e.getMessage_event_type_cd() + ").");

        BusinessCourtCaseEvent be = new BusinessCourtCaseEvent(e);

        if (e.isAgenFileEvent()) {
          // court case changed.  generate new business event.
          System.out.println(" Generating 'Court Case Changed' event (RCC_ID = '" + be.getJustin_rcc_id() + "')..");
        } else if (e.isAuthListEvent()) {
          // auth list changed.  Generate new business event.
          System.out.println(" Generating 'Court Case Auth List Changed' event (RCC_ID = '" + be.getJustin_rcc_id() + "')..");
        } else {
          System.out.println(" Unknown JUSTIN event type; Do nothing.");
        }
      }
    }

    exchange.getMessage().setBody("OK");
  }
}

class JustinAgenFileEventProcessor implements Processor {
  @Override
  public void process(Exchange exchange) throws Exception {
    // Insert code that gets executed *before* delegating
    // to the next processor in the chain.

    JustinEvent je = exchange.getIn().getBody(JustinEvent.class);

    BusinessCourtCaseEvent be = new BusinessCourtCaseEvent(je);

    exchange.getMessage().setBody(be, BusinessCourtCaseEvent.class);
  }
}

class JustinAuthListEventProcessor implements Processor {
  @Override
  public void process(Exchange exchange) throws Exception {
    // Insert code that gets executed *before* delegating
    // to the next processor in the chain.

    String exchangeId = exchange.getExchangeId();
    String messageId = exchange.getIn().getMessageId();

    JustinEvent je = exchange.getIn().getBody(JustinEvent.class);

    BusinessCourtCaseEvent be = new BusinessCourtCaseEvent(je);

    exchange.getMessage().setBody(be, BusinessCourtCaseEvent.class);
  }
}

public class CcmJustinAdapter extends RouteBuilder {
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
    .to("kafka:{{kafka.topic.courtcases.name}}");

    from("platform-http:/v1/health?httpMethodRestrict=GET")
    .routeId("healthCheck")
    .removeHeaders("CamelHttp*")
    .log("/v1/health request received")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("https://dev.jag.gov.bc.ca/ords/devj/justinords/dems/v1/health");

    //from("timer://simpleTimer?period={{notification.check.frequency}}")
    from("file:/etc/camel/resources/?fileName=eventBatch-oneRCC.json&noop=true&exchangePattern=InOnly&readLock=none")
    //from("file:/etc/camel/resources/?fileName=eventBatch-empty.json&noop=true&exchangePattern=InOnly&readLock=none")
    //from("file:/etc/camel/resources/?fileName=eventBatch.json&noop=true&exchangePattern=InOnly&readLock=none")
    .routeId("processNewJUSTINEvents")
    //.to("splunk-hec://hec.monitoring.ag.gov.bc.ca:8088/services/collector/f38b6861-1947-474b-bf6c-a743f2c6a413?")
    // .to("https://dev.jag.gov.bc.ca/ords/devj/justinords/dems/v1/inProgressEvents")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    //.to("direct:processNewJUSTINEvents");
    .log("Processing new JUSTIN events: ${body}")
    //.unmarshal().json(JsonLibrary.Jackson, JustinEventBatch.class)
    .setHeader("numOfEvents")
      .jsonpath("$.events.length()")
    .log("Event count: ${header[numOfEvents]}")
    .setHeader("events")
      .jsonpath("$.events")
    //.log("Events: ${header[events]}")
    // .split(jsonpath("$.events[*]"))
    .split()
      .jsonpathWriteAsString("$.events")  // https://stackoverflow.com/questions/51124978/splitting-a-json-array-with-camel
      .setHeader("message_event_type_cd")
        .jsonpath("$.message_event_type_cd")
      .choice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.EVENT_TYPE_AGEN_FILE))
          .to("direct:processAgenFileEvent")
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.EVENT_TYPE_AUTH_LIST))
          .to("direct:processAuthListEvent")
        .otherwise()
          .log("message_event_type_cd = ${header[message_event_type_cd]}")
          .to("direct:processUnknownEvent")
      .end()
      ;

      //.to("direct:processOneJUSTINEvent");
    
    // https://github.com/json-path/JsonPath

    //JustinEventBatchProcessor jp = new JustinEventBatchProcessor();

    from("direct:processNewJUSTINEvents")
    .log("Processing new JUSTIN events: ${body}")
    .unmarshal().json(JsonLibrary.Jackson, JustinEventBatch.class)
    .process(new JustinEventBatchProcessor())
    .log("Getting ready to send to Kafka: ${body}")
    ;

    from("direct:processAgenFileEvent")
    .setHeader("event").body()
    .log("Processing AGEN_FILE event: ${header[event]}")
    .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
    .process(new JustinAgenFileEventProcessor())
    .marshal().json(JsonLibrary.Jackson, BusinessCourtCaseEvent.class)
    .log("Generate converted business event: ${body}")
    .to("kafka:{{kafka.topic.courtcases.name}}")
    .setBody(simple("${header[event]}"))
    .to("direct:confirmEventProcessed")
    ;

    from("direct:processAuthListEvent")
    .setHeader("event").body()
    .log("Processing AUTH_LIST event: ${body}")
    .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
    .process(new JustinAuthListEventProcessor())
    .marshal().json(JsonLibrary.Jackson, BusinessCourtCaseEvent.class)
    .log("Generate converted business event: ${body}")
    .to("kafka:{{kafka.topic.courtcases.name}}")
    .setBody(simple("${header[event]}"))
    .to("direct:confirmEventProcessed")
    ;

    from("direct:processUnknownEvent")
    .log("Ignoring unknown event: ${body}")
    .to("direct:confirmEventProcessed")
    ;

    from("direct:confirmEventProcessed")
    .setHeader("event_message_id")
      .jsonpath("$.event_message_id")
    .setHeader("message_event_type_cd")
      .jsonpath("$.message_event_type_cd")
    .log("Marking event ${header[event_message_id]} (${header[message_event_type_cd]}) as processed.")
    ;

    from("platform-http:/getCourtCaseDetails?httpMethodRestrict=GET")
    .routeId("getCourtCaseDetails")
    .log("getCourtCaseDetails request received");
  }
}