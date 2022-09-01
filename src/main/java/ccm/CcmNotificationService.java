package ccm;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

// To run this integration use:
// kamel run CcmNotificationService.java --property file:ccmNotificationService.properties --profile openshift
// 

// camel-k: language=java
// camel-k: dependency=mvn:org.apache.camel.quarkus
// camel-k: dependency=mvn:org.apache.camel.camel-quarkus-kafka
// camel-k: dependency=mvn:org.apache.camel.camel-quarkus-jsonpath
// camel-k: dependency=mvn:org.apache.camel.camel-jackson
// camel-k: dependency=mvn:org.apache.camel.camel-splunk-hec
// camel-k: dependency=mvn:org.apache.camel.camel-http
// camel-k: dependency=mvn:org.apache.camel.camel-http-common

//import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

import ccm.models.business.BusinessCourtCaseData;
import ccm.models.business.BusinessCourtCaseEvent;
import ccm.models.system.dems.DemsCreateCourtCaseData;

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

    //from("kafka:{{kafka.topic.courtcases.name}}?groupId=ccm-notification-service")
    from("kafka:{{kafka.topic.courtcases.name}}?groupId=ccm-notification-service")
    .routeId("processCourtcaseEvents")
    .log("Event from Kafka {{kafka.topic.courtcases.name}} topic (offset=${headers[kafka.OFFSET]}): ${body}\n" + 
      "    on the topic ${headers[kafka.TOPIC]}\n" +
      "    on the partition ${headers[kafka.PARTITION]}\n" +
      "    with the offset ${headers[kafka.OFFSET]}\n" +
      "    with the key ${headers[kafka.KEY]}")
    .setHeader("event_object_id")
      .jsonpath("$.event_object_id")
    .setHeader("court_case_status")
      .jsonpath("$.court_case_status")
    .setHeader("event")
      .simple("${body}")
    .choice()
      .when(header("court_case_status").isEqualTo(BusinessCourtCaseEvent.STATUS_CHANGED))
        .to("direct:processCourtCaseChanged")
      .when(header("court_case_status").isEqualTo(BusinessCourtCaseEvent.STATUS_CREATED))
        .to("direct:processCourtCaseCreated")
      .when(header("court_case_status").isEqualTo(BusinessCourtCaseEvent.STATUS_UPDATED))
        .to("direct:processCourtCaseUpdated")
      .when(header("court_case_status").isEqualTo(BusinessCourtCaseEvent.STATUS_AUTH_LIST_CHANGED))
        .to("direct:processCourtCaseAuthListChanged")
      .otherwise()
        .to("direct:processUnknownStatus");
    ;

    from("direct:processCourtCaseChanged")
    .routeId("processCourtCaseChanged")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("processCourtCaseChanged.  event_object_id = ${header[event_object_id]}")
    .setHeader("number", simple("${header[event_object_id]}"))
    .to("http://ccm-lookup-service/getCourtCaseExists")
    .unmarshal().json()
    .setProperty("caseFound").simple("${body[id]}")
    .process(new Processor() {
      @Override
      public void process(Exchange ex) {
        BusinessCourtCaseEvent be = new BusinessCourtCaseEvent();

        // hardcoding boolean to false for first implementation
        //boolean court_case_exists = ex.getIn().getBody() != null && ex.getIn().getBody().toString().length() > 0;
        boolean court_case_exists = ex.getProperty("caseFound").toString().length() > 0;

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
    .to("kafka:{{kafka.topic.courtcases.name}}")
    ;

    from("direct:processCourtCaseCreated")
    .routeId("processCourtCaseCreated")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("processCourtCaseCreated.  event_object_id = ${header[event_object_id]}")
    .log("Retrieve latest court case details from JUSTIN.")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("number").simple("${header.event_object_id}")
    .to("http://ccm-lookup-service/getCourtCaseDetails")
    .log("Create court case in DEMS.  Court case data = ${body}.")
    .to("http://ccm-dems-adapter/createCourtCase")
    .log("Update court case auth list.")
    .to("direct:processCourtCaseAuthListChanged")
    ;

    from("direct:processCourtCaseUpdated")
    .routeId("processCourtCaseUpdated")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("processCourtCaseCreated.  event_object_id = ${header[event_object_id]}")
    .log("Retrieve latest court case details from JUSTIN.")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("number").simple("${header.event_object_id}")
    .to("http://ccm-lookup-service/getCourtCaseDetails")
    .log("Update court case in DEMS.  Court case data = ${body}.")
    .to("http://ccm-dems-adapter/updateCourtCase")
    .log("Update court case auth list.")
    .to("direct:processCourtCaseAuthListChanged")
    ;

    from("direct:processCourtCaseAuthListChanged")
    .routeId("processCourtCaseAuthListChanged")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("processCourtCaseAuthListChanged.  event_object_id = ${header[event_object_id]}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("number").simple("${header.event_object_id}")
    .log("Retrieve court case auth list")
    .to("http://ccm-lookup-service/getCourtCaseAuthList")
    .log("Update court case auth list in DEMS.  Court case auth list = ${body}")
    // work around -- not sure why body doesn't make it into dems-adapter
    .setHeader("temp-body", simple("${body}"))
    .to("http://ccm-dems-adapter/syncCaseUserList")
    ;

    from("direct:processUnknownStatus")
    .routeId("processUnknownStatus")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("processUnknownStatus.  event_object_id = ${header[event_object_id]}")
    ;

  }
}