package ccm;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

// To run this integration use:
// kamel run CcmNotificationService.java --property file:ccmNotificationService.properties --profile openshift
// 

// camel-k: language=java
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-kafka
// camel-k: trait=jvm.classpath=/etc/camel/resources/ccm-models.jar

//import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

import ccm.models.business.BusinessCourtCaseEvent;

public class CcmNotificationService extends RouteBuilder {
  @Override
  public void configure() throws Exception {
    // from("kafka:{{kafka.topic.courtcases.name}}")
    // .routeId("courtcases")
    // .log("Message received from Kafka : ${body}")
    // .log("    on the topic ${headers[kafka.TOPIC]}")
    // .log("    on the partition ${headers[kafka.PARTITION]}")
    // .log("    with the offset ${headers[kafka.OFFSET]}")
    // .log("    with the key ${headers[kafka.KEY]}")
    // .unmarshal().json()
    // .transform(simple("{\"type\": \"courtcase\", \"number\": \"${body[number]}\", \"status\": \"created\", \"public_content\": \"${body[public_content]}\", \"created_datetime\": \"${body[created_datetime]}\"}"))
    // .log("body (after unmarshalling): '${body}'")
    // .to("kafka:{{kafka.topic.kpis.name}}");

    from("kafka:{{kafka.topic.courtcases.name}}")
    .routeId("processCourtcaseEvents")
    .log("Message received from Kafka : ${body}\n" + 
      "    on the topic ${headers[kafka.TOPIC]}\n" +
      "    on the partition ${headers[kafka.PARTITION]}\n" +
      "    with the offset ${headers[kafka.OFFSET]}\n" +
      "    with the key ${headers[kafka.KEY]}")
    .setHeader("event_object_id")
      .jsonpath("$.event_object_id")
    .setHeader("court_case_status")
      .jsonpath("$.court_case_status")
    .choice()
      .when(header("court_case_status").isEqualTo(BusinessCourtCaseEvent.STATUS_CHANGED))
        .to("direct:processCourtCaseChanged")
      .when(header("court_case_status").isEqualTo(BusinessCourtCaseEvent.STATUS_CREATED))
        .to("direct:processCourtCaseCreated")
      .when(header("court_case_status").isEqualTo(BusinessCourtCaseEvent.STATUS_AUTH_LIST_CHANGED))
        .to("direct:processCourtCaseAuthListChanged")
      .otherwise()
        .to("direct:processUnknownStatus");
    ;

    from("direct:processCourtCaseChanged")
    .log("processCourtCaseChanged.  event_object_id = ${header[event_object_id]}")
    .setHeader("number", simple("${header[event_object_id]}"))
    .to("http://ccm-lookup-service/getCourtCaseExists?number=${header[event_object_id}")
    .process(new Processor() {
      @Override
      public void process(Exchange ex) {
        BusinessCourtCaseEvent be = new BusinessCourtCaseEvent();

        boolean court_case_exists = ex.getIn().getBody() != null && ex.getIn().getBody().toString().length() > 0;

        String event_object_id = ex.getIn().getHeader("event_object_id").toString();

        be.setEvent_object_id(event_object_id);
        be.setJustin_rcc_id(event_object_id);

        if (court_case_exists) {
          be.setCourt_case_status(BusinessCourtCaseEvent.STATUS_UPDATED);
        } else {
          be.setCourt_case_status(BusinessCourtCaseEvent.STATUS_CREATED);
        }

        ex.getMessage().setBody(be);
      }
    })
    .marshal().json(JsonLibrary.Jackson, BusinessCourtCaseEvent.class)
    .log("Generating derived court case event: ${body}")
    ;

    from("direct:processCourtCaseCreated")
    .log("processCourtCaseCreated.  event_object_id = ${header[event_object_id]}")
    ;

    from("direct:processCourtCaseAuthListChanged")
    .log("processCourtCaseAuthListChanged.  event_object_id = ${header[event_object_id]}")
    ;

    from("direct:processUnknownStatus")
    .log("processUnknownStatus.  event_object_id = ${header[event_object_id]}")
    ;

  }
}

// https://stackoverflow.com/questions/40756027/apache-camel-json-marshalling-to-pojo-java-bean
class CourtFileApproved {
  public String number;
  public String created_datetime;
  public String approved_datetime;
}