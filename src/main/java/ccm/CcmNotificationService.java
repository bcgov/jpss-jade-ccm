package ccm;

import java.util.StringTokenizer;

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

import ccm.models.common.data.ChargeAssessmentDataRef;
import ccm.models.common.event.CourtCaseEvent;
import ccm.models.common.event.BaseEvent;
import ccm.models.common.event.CaseUserEvent;
import ccm.models.common.event.ChargeAssessmentEvent;
import ccm.models.common.event.Error;
import ccm.models.common.event.EventKPI;
import ccm.utils.DateTimeUtils;
import ccm.utils.KafkaComponentUtils;

public class CcmNotificationService extends RouteBuilder {
  @Override
  public void configure() throws Exception {

    processChargeAssessmentCaseEvents();
    processApprovedCourtCaseEvents();
    processChargeAssessmentChanged();
    processManualChargeAssessmentChanged();
    processChargeAssessmentCreated();
    processChargeAssessmentUpdated();
    processCourtCaseAuthListChanged();
    processCourtCaseAuthListUpdated();
    processCourtCaseChanged();
    processManualCourtCaseChanged();
    processCourtCaseAppearanceChanged();
    processCourtCaseCrownAssignmentChanged();
    processCaseUserEvents();
    processCaseUserAccessAdded();
    processCaseUserAccessRemoved();
    processUnknownStatus();
    preprocessAndPublishEventCreatedKPI();
    publishEventKPI();
    publishBodyAsEventKPI();
  }

  private void processChargeAssessmentCaseEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //from("kafka:{{kafka.topic.chargeassessmentcases.name}}?groupId=ccm-notification-service")
    from("kafka:{{kafka.topic.chargeassessmentcases.name}}?groupId=ccm-notification-service")
    .routeId(routeId)
    .log("Event from Kafka {{kafka.topic.chargeassessmentcases.name}} topic (offset=${headers[kafka.OFFSET]}): ${body}\n" + 
      "    on the topic ${headers[kafka.TOPIC]}\n" +
      "    on the partition ${headers[kafka.PARTITION]}\n" +
      "    with the offset ${headers[kafka.OFFSET]}\n" +
      "    with the key ${headers[kafka.KEY]}")
    .setHeader("event_key")
      .jsonpath("$.event_key")
    .setHeader("event_status")
      .jsonpath("$.event_status")
    .setHeader("event")
      .simple("${body}")
    .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentEvent.class)
    .setProperty("kpi_event_object", body())
    .setProperty("kpi_event_topic_name", simple("${headers[kafka.TOPIC]}"))
    .setProperty("kpi_event_topic_offset", simple("${headers[kafka.OFFSET]}"))
    .marshal().json(JsonLibrary.Jackson, ChargeAssessmentEvent.class)
    .choice()
      // .when(header("event_status").isEqualTo(ChargeAssessmentEvent.STATUS.CHANGED))
      //   .setProperty("kpi_component_route_name", simple("processChargeAssessmentChanged"))
      //   .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
      //   .to("direct:publishEventKPI")
      //   .setBody(header("event"))
      //   .to("direct:processChargeAssessmentChanged")
      //   .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
      //   .to("direct:publishEventKPI")
      //   .endChoice()
      .when(header("event_status").isEqualTo(ChargeAssessmentEvent.STATUS.MANUALLY_CHANGED))
        .setProperty("kpi_component_route_name", simple("processManualChargeAssessmentChanged"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processManualChargeAssessmentChanged")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .when(header("event_status").isEqualTo(ChargeAssessmentEvent.STATUS.CREATED))
        .setProperty("kpi_component_route_name", simple("processChargeAssessmentCreated"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processChargeAssessmentCreated")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .when(header("event_status").isEqualTo(ChargeAssessmentEvent.STATUS.UPDATED))
        .setProperty("kpi_component_route_name", simple("processChargeAssessmentUpdated"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processChargeAssessmentUpdated")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .when(header("event_status").isEqualTo(ChargeAssessmentEvent.STATUS.AUTH_LIST_CHANGED))
        .setProperty("kpi_component_route_name", simple("processCourtCaseAuthListChanged"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processCourtCaseAuthListChanged")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .otherwise()
        .to("direct:processUnknownStatus")
        .setProperty("kpi_component_route_name", simple("processUnknownStatus"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .end();
    ;
  }

  private void processChargeAssessmentCreated() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("event_key = ${header[event_key]}")
    .log("Retrieve latest court case details from JUSTIN.")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("number").simple("${header.event_key}")
    .to("http://ccm-lookup-service/getCourtCaseDetails")
    .log("Create court case in DEMS.  Court case data = ${body}.")
    .setProperty("courtcase_data", simple("${bodyAs(String)}"))
    .to("http://ccm-dems-adapter/createCourtCase")
    .log("Update court case auth list.")
    .to("direct:processCourtCaseAuthListChanged")
    ;
  }

  private void processApprovedCourtCaseEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("kafka:{{kafka.topic.approvedcourtcases.name}}?groupId=ccm-notification-service")
    .routeId(routeId)
    .log("Event from Kafka {{kafka.topic.approvedcourtcases.name}} topic (offset=${headers[kafka.OFFSET]}): ${body}\n" + 
      "    on the topic ${headers[kafka.TOPIC]}\n" +
      "    on the partition ${headers[kafka.PARTITION]}\n" +
      "    with the offset ${headers[kafka.OFFSET]}\n" +
      "    with the key ${headers[kafka.KEY]}")
    .setHeader("event_key")
      .jsonpath("$.event_key")
    .setHeader("event_status")
      .jsonpath("$.event_status")
    .setHeader("event")
      .simple("${body}")
    .unmarshal().json(JsonLibrary.Jackson, CourtCaseEvent.class)
    .setProperty("kpi_event_object", body())
    .setProperty("kpi_event_topic_name", simple("${headers[kafka.TOPIC]}"))
    .setProperty("kpi_event_topic_offset", simple("${headers[kafka.OFFSET]}"))
    .marshal().json(JsonLibrary.Jackson, CourtCaseEvent.class)
    .choice()
      .when(header("event_status").isEqualTo(CourtCaseEvent.STATUS.CHANGED))
        .setProperty("kpi_component_route_name", simple("processCourtCaseChanged"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processCourtCaseChanged")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .when(header("event_status").isEqualTo(CourtCaseEvent.STATUS.MANUALLY_CHANGED))
        .setProperty("kpi_component_route_name", simple("processManualCourtCaseChanged"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processManualCourtCaseChanged")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .when(header("event_status").isEqualTo(CourtCaseEvent.STATUS.APPEARANCE_CHANGED))
        .setProperty("kpi_component_route_name", simple("processCourtCaseAppearanceChanged"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processCourtCaseAppearanceChanged")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .when(header("event_status").isEqualTo(CourtCaseEvent.STATUS.CROWN_ASSIGNMENT_CHANGED))
        .setProperty("kpi_component_route_name", simple("processCourtCaseCrownAssignmentChanged"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processCourtCaseCrownAssignmentChanged")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .otherwise()
        .to("direct:processUnknownStatus")
        .setProperty("kpi_component_route_name", simple("processUnknownStatus"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .end()
    ;
  }

  private void processChargeAssessmentChanged() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN
    // property: event_object
    // property: caseFound
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("event_key = ${header[event_key]}")
    .setHeader("number", simple("${header[event_key]}"))
    .to("http://ccm-lookup-service/getCourtCaseExists")
    .unmarshal().json()
    .setProperty("caseFound").simple("${body[id]}")
    .setProperty("autoCreateFlag").simple("{{dems.case.auto.creation}}")
    .choice()
      .when(simple("${exchangeProperty.autoCreateFlag} == 'true' || ${exchangeProperty.caseFound} != ''"))
        .process(new Processor() {
          @Override
          public void process(Exchange ex) {
            // KPI: Preserve original event properties
            ex.setProperty("kpi_event_object_orig", ex.getProperty("kpi_event_object"));
            ex.setProperty("kpi_event_topic_offset_orig", ex.getProperty("kpi_event_topic_offset"));
            ex.setProperty("kpi_event_topic_name_orig", ex.getProperty("kpi_event_topic_name"));
            ex.setProperty("kpi_status_orig", ex.getProperty("kpi_status"));
            ex.setProperty("kpi_component_route_name_orig", ex.getProperty("kpi_component_route_name"));

            ChargeAssessmentEvent original_event = (ChargeAssessmentEvent)ex.getProperty("kpi_event_object");
            ChargeAssessmentEvent derived_event = new ChargeAssessmentEvent(ChargeAssessmentEvent.SOURCE.JADE_CCM, original_event);

            boolean court_case_exists = ex.getProperty("caseFound").toString().length() > 0;

            if (court_case_exists) {
              derived_event.setEvent_status(ChargeAssessmentEvent.STATUS.UPDATED.toString());
            } else {
              derived_event.setEvent_status(ChargeAssessmentEvent.STATUS.CREATED.toString());
            }

            ex.getMessage().setBody(derived_event);

            // KPI: Set new event object
            ex.setProperty("kpi_event_object", derived_event);
          }
        })
        .marshal().json(JsonLibrary.Jackson, ChargeAssessmentEvent.class)
        .log("Generating derived court case event: ${body}")
        .to("kafka:{{kafka.topic.chargeassessmentcases.name}}") // only push on topic, if auto creation is true
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
        .setProperty("kpi_event_topic_name", simple("{{kafka.topic.chargeassessmentcases.name}}"))
        .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
        .setProperty("kpi_component_route_name", simple(routeId))
        .to("direct:preprocessAndPublishEventCreatedKPI")
        // KPI: restore previous values
        .setProperty("kpi_event_object", simple("${exchangeProperty.kpi_event_object_orig}"))
        .setProperty("kpi_event_topic_offset", simple("${exchangeProperty.kpi_event_topic_offset_orig}"))
        .setProperty("kpi_event_topic_name", simple("${exchangeProperty.kpi_event_topic_name_orig}"))
        .setProperty("kpi_status", simple("${exchangeProperty.kpi_status_orig}"))
        .setProperty("kpi_component_route_name", simple("${exchangeProperty.kpi_component_route_name_orig}"))
    .end()
    ;
  }

  private void processManualChargeAssessmentChanged() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN
    // property: event_object
    // property: caseFound
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("event_key = ${header[event_key]}")
    .setHeader("number", simple("${header[event_key]}"))
    .to("http://ccm-lookup-service/getCourtCaseExists")
    .unmarshal().json()
    .setProperty("caseFound").simple("${body[id]}")
    .process(new Processor() {
      @Override
      public void process(Exchange ex) {
        // KPI: Preserve original event properties
        ex.setProperty("kpi_event_object_orig", ex.getProperty("kpi_event_object"));
        ex.setProperty("kpi_event_topic_offset_orig", ex.getProperty("kpi_event_topic_offset"));
        ex.setProperty("kpi_event_topic_name_orig", ex.getProperty("kpi_event_topic_name"));
        ex.setProperty("kpi_status_orig", ex.getProperty("kpi_status"));
        ex.setProperty("kpi_component_route_name_orig", ex.getProperty("kpi_component_route_name"));

        ChargeAssessmentEvent original_event = (ChargeAssessmentEvent)ex.getProperty("kpi_event_object");
        ChargeAssessmentEvent derived_event = new ChargeAssessmentEvent(ChargeAssessmentEvent.SOURCE.JADE_CCM, original_event);

        boolean court_case_exists = ex.getProperty("caseFound").toString().length() > 0;

        if (court_case_exists) {
          derived_event.setEvent_status(ChargeAssessmentEvent.STATUS.UPDATED.toString());
        } else {
          derived_event.setEvent_status(ChargeAssessmentEvent.STATUS.CREATED.toString());
        }

        ex.getMessage().setBody(derived_event);

        // KPI: Set new event object
        ex.setProperty("kpi_event_object", derived_event);
      }
    })
    .marshal().json(JsonLibrary.Jackson, ChargeAssessmentEvent.class)
    .log("Generating derived court case event: ${body}")
    .to("kafka:{{kafka.topic.chargeassessmentcases.name}}") // only push on topic, if auto creation is true
    .log("Returned topic value = ${body}")
    .setProperty("kpi_event_topic_name", simple("{{kafka.topic.chargeassessmentcases.name}}"))
    .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
    .setProperty("kpi_component_route_name", simple(routeId))
    .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
    .to("direct:preprocessAndPublishEventCreatedKPI")
    // KPI: restore previous values
    .setProperty("kpi_event_object", simple("${exchangeProperty.kpi_event_object_orig}"))
    .setProperty("kpi_event_topic_offset", simple("${exchangeProperty.kpi_event_topic_offset_orig}"))
    .setProperty("kpi_event_topic_name", simple("${exchangeProperty.kpi_event_topic_name_orig}"))
    .setProperty("kpi_status", simple("${exchangeProperty.kpi_status_orig}"))
    .setProperty("kpi_component_route_name", simple("${exchangeProperty.kpi_component_route_name_orig}"))
    ;
  }

  private void processChargeAssessmentUpdated() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("event_key = ${header[event_key]}")
    .log("Retrieve latest court case details from JUSTIN.")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("number").simple("${header.event_key}")
    .to("http://ccm-lookup-service/getCourtCaseDetails")
    .log("Update court case in DEMS.  Court case data = ${body}.")
    .setProperty("courtcase_data", simple("${bodyAs(String)}"))
    //.to("http://ccm-dems-adapter/updateCourtCase?httpClient.connectTimeout=1&httpClient.connectionRequestTimeout=1&httpClient.socketTimeout=1")
    .setBody(simple("${exchangeProperty.courtcase_data}"))
    .to("http://ccm-dems-adapter/updateCourtCase")
    .log("Update court case auth list.")
    .to("direct:processCourtCaseAuthListChanged")
    ;
  }

  private void processCourtCaseAuthListChanged() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("event_key = ${header[event_key]}")
    .setHeader("number", simple("${header[event_key]}"))
    .to("http://ccm-lookup-service/getCourtCaseExists")
    .unmarshal().json()
    .setProperty("caseFound").simple("${body[id]}")
    .setProperty("autoCreateFlag").simple("{{dems.case.auto.creation}}")
    .choice()
      .when(simple("${exchangeProperty.caseFound} != ''"))
        .to("direct:processCourtCaseAuthListUpdated")
    .end()
    ;
  }

  private void processCourtCaseAuthListUpdated() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("event_key = ${header[event_key]}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("number").simple("${header.event_key}")
    .log("Retrieve court case auth list")
    .to("http://ccm-lookup-service/getCourtCaseAuthList")
    .log("Update court case auth list in DEMS.  Court case auth list = ${body}")
    // JADE-1489 work around #1 -- not sure why body doesn't make it into dems-adapter
    .setHeader("temp-body", simple("${body}"))
    .to("http://ccm-dems-adapter/syncCaseUserList")
    ;
  }
  
  private void processCaseUserEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //from("kafka:{{kafka.topic.chargeassessmentcases.name}}?groupId=ccm-notification-service")
    from("kafka:{{kafka.topic.caseusers.name}}?groupId=ccm-notification-service")
    .routeId(routeId)
    .log("Event from Kafka {{kafka.topic.chargeassessmentcases.name}} topic (offset=${headers[kafka.OFFSET]}): ${body}\n" + 
      "    on the topic ${headers[kafka.TOPIC]}\n" +
      "    on the partition ${headers[kafka.PARTITION]}\n" +
      "    with the offset ${headers[kafka.OFFSET]}\n" +
      "    with the key ${headers[kafka.KEY]}")
    .setHeader("event_key")
      .jsonpath("$.event_key")
    .setHeader("event_status")
      .jsonpath("$.event_status")
    .setHeader("event")
      .simple("${body}")
    .unmarshal().json(JsonLibrary.Jackson, CaseUserEvent.class)
    .setProperty("event_object", body())
    .setProperty("kpi_event_object", body())
    .setProperty("kpi_event_topic_name", simple("${headers[kafka.TOPIC]}"))
    .setProperty("kpi_event_topic_offset", simple("${headers[kafka.OFFSET]}"))
    .marshal().json(JsonLibrary.Jackson, CaseUserEvent.class)
    .choice()
      .when(header("event_status").isEqualTo(CaseUserEvent.STATUS.ACCESS_ADDED))
        .setProperty("kpi_component_route_name", simple("processCaseUserAccessAdded"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processCaseUserAccessAdded")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .when(header("event_status").isEqualTo(CaseUserEvent.STATUS.ACCESS_REMOVED))
        .setProperty("kpi_component_route_name", simple("processCaseUserAccessRemoved"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processCaseUserAccessRemoved")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .otherwise()
        .to("direct:processUnknownStatus")
        .setProperty("kpi_component_route_name", simple("processUnknownStatus"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .end();
    ;
  }

  private void processCaseUserAccessAdded() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN
    // property: event_object
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("event_key = ${header[event_key]}")
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        CaseUserEvent event = (CaseUserEvent)exchange.getProperty("event_object");
        exchange.getMessage().setHeader("event_key", event.getJustin_rcc_id());
      }
    })
    .log("Calling route processCourtCaseAuthListChanged( rcc_id = ${header[event_key]} ) ...")
    .to("direct:processCourtCaseAuthListChanged")
    .log("Returned from processCourtCaseAuthListChanged().")
    ;
  }

  private void processCaseUserAccessRemoved() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN
    // property: event_object
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("event_key = ${header[event_key]}")
    .setHeader("key", simple("${header[event_key]}"))
    .to("http://ccm-lookup-service/getCaseListByUserKey?throwExceptionOnFailure=false")
    .choice()
        .when(simple("${header.CamelHttpResponseCode} == 200"))
          .log("body = '${body}'.")
          .split()
            .jsonpathWriteAsString("$.case_list")
            .setProperty("rcc_id",jsonpath("$.rcc_id"))
            .log("Iterating through case list.  case ref = ${body}")
            .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentDataRef.class)
            .setHeader("event_key", jsonpath("$.rcc_id"))
            .log("Calling route processCourtCaseAuthListUpdated( rcc_id = ${header[event_key]} ) ...")
            .to("direct:processCourtCaseAuthListUpdated")
            .log("Returned from processCourtCaseAuthListUpdated().")
            .end()
          .endChoice()
          .when(simple("${header.CamelHttpResponseCode} == 404"))
            .log("User (key = ${header.event_key}) not found; Do nothing.")
          .endChoice()
      .end()
    ;
  }

  private void deprecated_processCaseUserAccessRemovedAsDerivedEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN
    // property: event_object
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("event_key = ${header[event_key]}")
    .setHeader("key", simple("${header[event_key]}"))
    .to("http://ccm-lookup-service/getCaseListByUserKey?throwExceptionOnFailure=false")
    .choice()
        .when(simple("${header.CamelHttpResponseCode} == 200"))
          .log("body = '${body}'.")
          .split()
            .jsonpathWriteAsString("$.case_list")
            .setProperty("rcc_id",jsonpath("$.rcc_id"))
            .log("Iterating through case list.  case ref = ${body}")
            .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentDataRef.class)
            .process(new Processor() {
              @Override
              public void process(Exchange exchange) throws Exception {
                ChargeAssessmentDataRef caseRef = (ChargeAssessmentDataRef)exchange.getIn().getBody();
                CaseUserEvent event = (CaseUserEvent)exchange.getProperty("event_object");
                ChargeAssessmentEvent authListEvent = new ChargeAssessmentEvent();
                authListEvent.setEvent_dtm(DateTimeUtils.generateCurrentDtm());
                authListEvent.setEvent_key(caseRef.getRcc_id());
                authListEvent.setEvent_source(ChargeAssessmentEvent.SOURCE.JADE_CCM.name());
                authListEvent.setEvent_status(ChargeAssessmentEvent.STATUS.AUTH_LIST_CHANGED.name());
                authListEvent.setJustin_event_dtm(event.getJustin_event_dtm());
                authListEvent.setJustin_event_message_id(event.getJustin_event_message_id());
                authListEvent.setJustin_fetched_date(event.getJustin_fetched_date());
                authListEvent.setJustin_guid(event.getJustin_guid());
                authListEvent.setJustin_message_event_type_cd(event.getJustin_message_event_type_cd());
                exchange.setProperty("derived_event_object", authListEvent);
                exchange.setProperty("derived_event_type", authListEvent.getEvent_type());
              }
            })
            .setBody(simple("${exchangeProperty.derived_event_object}"))
            .marshal().json(JsonLibrary.Jackson, ChargeAssessmentEvent.class)
            .log("Publishing derived event ${exchangeProperty.derived_event_type} (rcc_id = ${exchangeProperty.rcc_id}) ...")
            .log("body: ${body}")
            .to("kafka:{{kafka.topic.chargeassessmentcases.name}}")
            .setProperty("derived_event_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
            .setProperty("derived_event_topic", simple("{{kafka.topic.chargeassessmentcases.name}}"))
            .log("Derived event published.")
            .process(new Processor() {
              @Override
              public void process(Exchange exchange) throws Exception {
                ChargeAssessmentEvent derived_event = (ChargeAssessmentEvent)exchange.getProperty("derived_event_object");

                // https://kafka.apache.org/30/javadoc/org/apache/kafka/clients/producer/RecordMetadata.html
                // extract the offset from response header.  Example format: "[some-topic-0@301]"
                String derived_event_offset = KafkaComponentUtils.extractOffsetFromRecordMetadata(
                  exchange.getProperty("derived_event_recordmetadata"));
                  
                String derived_event_topic = (String)exchange.getProperty("derived_event_topic");

                EventKPI derived_event_kpi = new EventKPI(
                  derived_event, 
                  EventKPI.STATUS.EVENT_CREATED);

                derived_event_kpi.setComponent_route_name(routeId);
                derived_event_kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
                derived_event_kpi.setEvent_topic_name(derived_event_topic);
                derived_event_kpi.setEvent_topic_offset(derived_event_offset);

                exchange.getMessage().setBody(derived_event_kpi);
              }
            })
            .marshal().json(JsonLibrary.Jackson, EventKPI.class)
            .log("Publishing derived event KPI ...")
            .to("direct:publishBodyAsEventKPI")
            .log("Derived event KPI published.")
            .end()
          .endChoice()
          .when(simple("${header.CamelHttpResponseCode} == 404"))
            .log("User (key = ${header.event_key}) not found; Do nothing.")
          .endChoice()
      .end()
    ;
  }

  private void processCourtCaseChanged() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("event_key = ${header[event_key]}")
    .setHeader("number", simple("${header[event_key]}"))
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-lookup-service/getCourtCaseMetadata")
    .log("Retrieved Court Case Metadata from JUSTIN: ${body}")
    // JADE-1489 workaround #2 -- not sure why in this instance the value of ${body} as-is isn't 
    //   accessible in the split() block through exchange properties unless converted to String first.
    .setProperty("metadata_data", simple("${bodyAs(String)}"))
    .split()
      .jsonpathWriteAsString("$.related_agency_file")
      .setProperty("rcc_id", jsonpath("$.rcc_id"))
      .setProperty("event_key_orig", simple("${header[event_key]}"))
      .setHeader("number", jsonpath("$.rcc_id"))
      .setHeader("event_key", jsonpath("$.rcc_id"))
      .to("http://ccm-lookup-service/getCourtCaseExists")
      .unmarshal().json()
      .setProperty("caseFound").simple("${body[id]}")
      .setProperty("autoCreateFlag").simple("{{dems.case.auto.creation}}")
      .choice()
        .when(simple("${exchangeProperty.autoCreateFlag} == 'true' && ${exchangeProperty.caseFound} == ''"))
        .process(new Processor() {
          @Override
          public void process(Exchange ex) {
            // KPI: Preserve original event properties
            ex.setProperty("kpi_event_object_orig", ex.getProperty("kpi_event_object"));
            ex.setProperty("kpi_event_topic_offset_orig", ex.getProperty("kpi_event_topic_offset"));
            ex.setProperty("kpi_event_topic_name_orig", ex.getProperty("kpi_event_topic_name"));
            ex.setProperty("kpi_status_orig", ex.getProperty("kpi_status"));
            ex.setProperty("kpi_component_route_name_orig", ex.getProperty("kpi_component_route_name"));

            ChargeAssessmentEvent derived_event = new ChargeAssessmentEvent();
            derived_event.setEvent_status(ChargeAssessmentEvent.STATUS.CREATED.toString());
            derived_event.setEvent_source(ChargeAssessmentEvent.SOURCE.JADE_CCM.name());

            ex.getMessage().setBody(derived_event);

            // KPI: Set new event object
            ex.setProperty("kpi_event_object", derived_event);
          }
        })
        .marshal().json(JsonLibrary.Jackson, ChargeAssessmentEvent.class)
        .log("Generating derived court case event: ${body}")
        .to("direct:processChargeAssessmentCreated")
        // KPI: restore previous values
        .setProperty("kpi_event_object", simple("${exchangeProperty.kpi_event_object_orig}"))
        .setProperty("kpi_event_topic_offset", simple("${exchangeProperty.kpi_event_topic_offset_orig}"))
        .setProperty("kpi_event_topic_name", simple("${exchangeProperty.kpi_event_topic_name_orig}"))
        .setProperty("kpi_status", simple("${exchangeProperty.kpi_status_orig}"))
        .setProperty("kpi_component_route_name", simple("${exchangeProperty.kpi_component_route_name_orig}"))
      .end()

      // reset the original values
      .setHeader("number", simple("${exchangeProperty.event_key_orig}"))
      .setHeader("event_key", simple("${exchangeProperty.event_key_orig}"))
      .setHeader("rcc_id", simple("${exchangeProperty.rcc_id}"))
      .setHeader("caseFound", simple("${exchangeProperty.caseFound}"))
      .log("Found related court case. Rcc_id: ${header.rcc_id}")
      .setBody(simple("${exchangeProperty.metadata_data}"))
      .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .to("http://ccm-dems-adapter/updateCourtCaseWithMetadata")
    .end()
    ;
  }

  private void processManualCourtCaseChanged() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("event_key = ${header[event_key]}")
    .setHeader("number", simple("${header[event_key]}"))
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-lookup-service/getCourtCaseMetadata")
    .log("Retrieved Court Case Metadata from JUSTIN: ${body}")
    // JADE-1489 workaround #2 -- not sure why in this instance the value of ${body} as-is isn't 
    //   accessible in the split() block through exchange properties unless converted to String first.
    .setProperty("metadata_data", simple("${bodyAs(String)}"))
    .split()
      .jsonpathWriteAsString("$.related_agency_file")

      .setProperty("event_key_orig", simple("${header[event_key]}"))
      .setProperty("rcc_id", jsonpath("$.rcc_id"))

      .setHeader("number", jsonpath("$.rcc_id"))
      .setHeader("event_key", jsonpath("$.rcc_id"))
      .log("rcc_id event_key = ${header[event_key]}")
      .to("http://ccm-lookup-service/getCourtCaseExists")
      .unmarshal().json()
      .setProperty("caseFound").simple("${body[id]}")
      .setProperty("autoCreateFlag").simple("{{dems.case.auto.creation}}")
      .log("caseFound = ${exchangeProperty.caseFound}")
      .log("autoCreateFlag = ${exchangeProperty.autoCreateFlag}")
      .choice()
        .when(simple("${exchangeProperty.caseFound} == ''"))
        .process(new Processor() {
          @Override
          public void process(Exchange ex) {

            ChargeAssessmentEvent derived_event = new ChargeAssessmentEvent();
            derived_event.setEvent_status(ChargeAssessmentEvent.STATUS.CREATED.toString());
            derived_event.setEvent_source(ChargeAssessmentEvent.SOURCE.JADE_CCM.name());

            ex.getMessage().setBody(derived_event);
          }
        })
        .marshal().json(JsonLibrary.Jackson, ChargeAssessmentEvent.class)
        .log("Generating derived court case event: ${body}")
        .to("direct:processChargeAssessmentCreated")
      .end()
      // reset the original values
      .setHeader("number", simple("${exchangeProperty.event_key_orig}"))
      .setHeader("event_key", simple("${exchangeProperty.event_key_orig}"))
      .setHeader("rcc_id", simple("${exchangeProperty.rcc_id}"))
      .setHeader("caseFound", simple("${exchangeProperty.caseFound}"))
      .log("Found related court case. Rcc_id: ${header.rcc_id}")
      .setBody(simple("${exchangeProperty.metadata_data}"))
      .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .to("http://ccm-dems-adapter/updateCourtCaseWithMetadata")
    .end()


    .doTry()
      .log("Create new crown assignment changed event.")
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
          CourtCaseEvent origbe = (CourtCaseEvent)exchange.getProperty("kpi_event_object");
          CourtCaseEvent be = new CourtCaseEvent(CourtCaseEvent.SOURCE.JADE_CCM.toString(), origbe);
          be.setEvent_status(CourtCaseEvent.STATUS.CROWN_ASSIGNMENT_CHANGED.toString());
      
          exchange.getMessage().setBody(be, CourtCaseEvent.class);
          exchange.setProperty("derived_event_object", be);
          exchange.getMessage().setHeader("kafka.KEY", be.getEvent_key());
        }})
      .marshal().json(JsonLibrary.Jackson, CourtCaseEvent.class)
      .log("Generate converted business event: ${body}")
      .to("kafka:{{kafka.topic.approvedcourtcases.name}}")


      .setProperty("derived_event_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
      .setProperty("derived_event_topic", simple("{{kafka.topic.approvedcourtcases.name}}"))
      .log("Derived event published.")
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
          CourtCaseEvent derived_event = (CourtCaseEvent)exchange.getProperty("derived_event_object");

          // https://kafka.apache.org/30/javadoc/org/apache/kafka/clients/producer/RecordMetadata.html
          // extract the offset from response header.  Example format: "[some-topic-0@301]"
          String derived_event_offset = KafkaComponentUtils.extractOffsetFromRecordMetadata(
            exchange.getProperty("derived_event_recordmetadata"));
            
          String derived_event_topic = (String)exchange.getProperty("derived_event_topic");

          EventKPI derived_event_kpi = new EventKPI(
            derived_event, 
            EventKPI.STATUS.EVENT_CREATED);

          derived_event_kpi.setComponent_route_name(routeId);
          derived_event_kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
          derived_event_kpi.setEvent_topic_name(derived_event_topic);
          derived_event_kpi.setEvent_topic_offset(derived_event_offset);

          exchange.getMessage().setBody(derived_event_kpi);
        }
      })
      .marshal().json(JsonLibrary.Jackson, EventKPI.class)
      .log("Publishing derived event KPI ...")
      .to("direct:publishBodyAsEventKPI")
      .log("Derived event KPI published.")





      .log("Create new appearance summary changed event.")
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
          CourtCaseEvent origbe = (CourtCaseEvent)exchange.getProperty("kpi_event_object");
          CourtCaseEvent be = new CourtCaseEvent(CourtCaseEvent.SOURCE.JADE_CCM.toString(), origbe);
          be.setEvent_status(CourtCaseEvent.STATUS.APPEARANCE_CHANGED.toString());
      
          exchange.getMessage().setBody(be, CourtCaseEvent.class);
          exchange.setProperty("derived_event_object", be);
          exchange.getMessage().setHeader("kafka.KEY", be.getEvent_key());
        }})
      .marshal().json(JsonLibrary.Jackson, CourtCaseEvent.class)
      .log("Generate converted business event: ${body}")
      .to("kafka:{{kafka.topic.approvedcourtcases.name}}")


      .setProperty("derived_event_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
      .setProperty("derived_event_topic", simple("{{kafka.topic.approvedcourtcases.name}}"))
      .log("Derived event published.")
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
          CourtCaseEvent derived_event = (CourtCaseEvent)exchange.getProperty("derived_event_object");

          // https://kafka.apache.org/30/javadoc/org/apache/kafka/clients/producer/RecordMetadata.html
          // extract the offset from response header.  Example format: "[some-topic-0@301]"
          String derived_event_offset = KafkaComponentUtils.extractOffsetFromRecordMetadata(
            exchange.getProperty("derived_event_recordmetadata"));
            
          String derived_event_topic = (String)exchange.getProperty("derived_event_topic");

          EventKPI derived_event_kpi = new EventKPI(
            derived_event, 
            EventKPI.STATUS.EVENT_CREATED);

          derived_event_kpi.setComponent_route_name(routeId);
          derived_event_kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
          derived_event_kpi.setEvent_topic_name(derived_event_topic);
          derived_event_kpi.setEvent_topic_offset(derived_event_offset);

          exchange.getMessage().setBody(derived_event_kpi);
        }
      })
      .marshal().json(JsonLibrary.Jackson, EventKPI.class)
      .log("Publishing derived event KPI ...")
      .to("direct:publishBodyAsEventKPI")
      .log("Derived event KPI published.")



      .doCatch(Exception.class)
        .log("General Exception thrown.")
        .log("${exception}")
        .setProperty("error_event_object", body())
        .setProperty("kpi_event_topic_name",simple("{{kafka.topic.general-errors.name}}"))
        .to("direct:publishJustinEventKPIError")
        .process(new Processor() {
          public void process(Exchange exchange) throws Exception {
  
            throw exchange.getException();
          }
        })
    .end()
  

    ;
  }

  private void processCourtCaseAppearanceChanged() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("event_key = ${header[event_key]}")
    .setHeader("number", simple("${header[event_key]}"))
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-lookup-service/getCourtCaseAppearanceSummaryList")
    .log("Retrieved Court Case appearance summary list from JUSTIN: ${body}")
    // JADE-1489 workaround #2 -- not sure why in this instance the value of ${body} as-is isn't 
    //   accessible in the split() block through exchange properties unless converted to String first.
    .setProperty("business_data", simple("${bodyAs(String)}"))
    .to("http://ccm-lookup-service/getCourtCaseMetadata")
    .log("Retrieved Court Case Metadata from JUSTIN: ${body}")
    .setProperty("metadata_data", simple("${bodyAs(String)}"))
    .split()
      .jsonpathWriteAsString("$.related_agency_file")
      .setProperty("rcc_id", jsonpath("$.rcc_id"))
      .log("Check case (rcc_id ${exchangeProperty.rcc_id}) existence ...")
      .setHeader("number", simple("${exchangeProperty.rcc_id}"))
      .to("http://ccm-lookup-service/getCourtCaseExists")
      .unmarshal().json()
      .setProperty("caseId").simple("${body[id]}")
      .choice()
        .when(simple("${exchangeProperty.caseId} != ''"))
          .setHeader("rcc_id", simple("${exchangeProperty.rcc_id}"))
          .log("Found related court case. Rcc_id: ${header.rcc_id}")
          .setBody(simple("${exchangeProperty.business_data}"))
          .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
          .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
          .to("http://ccm-dems-adapter/updateCourtCaseWithAppearanceSummary")
          .endChoice()
        .otherwise()
          .log("Case (rcc_id ${exchangeProperty.rcc_id}) not found; do nothing.")
          .endChoice()
        .end()
    .end()
    ;
  }

  private void processCourtCaseCrownAssignmentChanged() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("event_key = ${header[event_key]}")
    .setHeader("number", simple("${header[event_key]}"))
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-lookup-service/getCourtCaseCrownAssignmentList")
    .log("Retrieved Court Case crown assignment list from JUSTIN: ${body}")
    // JADE-1489 workaround #2 -- not sure why in this instance the value of ${body} as-is isn't 
    //   accessible in the split() block through exchange properties unless converted to String first.
    .setProperty("business_data", simple("${bodyAs(String)}"))
    .to("http://ccm-lookup-service/getCourtCaseMetadata")
    .log("Retrieved Court Case Metadata from JUSTIN: ${body}")
    .setProperty("metadata_data", simple("${bodyAs(String)}"))
    .split()
      .jsonpathWriteAsString("$.related_agency_file")
      .setProperty("rcc_id", jsonpath("$.rcc_id"))
      .log("Check case (rcc_id ${exchangeProperty.rcc_id}) existence ...")
      .setHeader("number", simple("${exchangeProperty.rcc_id}"))
      .to("http://ccm-lookup-service/getCourtCaseExists")
      .unmarshal().json()
      .setProperty("caseId").simple("${body[id]}")
      .choice()
        .when(simple("${exchangeProperty.caseId} != ''"))
          .setHeader("rcc_id", simple("${exchangeProperty.rcc_id}"))
          .log("Found related court case. Rcc_id: ${header.rcc_id}")
          .setBody(simple("${exchangeProperty.business_data}"))
          .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
          .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
          .to("http://ccm-dems-adapter/updateCourtCaseWithCrownAssignmentData")
          .endChoice()
        .otherwise()
          .log("Case (rcc_id ${exchangeProperty.rcc_id}) not found; do nothing.")
          .endChoice()
        .end()
    .end()
    ;
  }

  private void processUnknownStatus() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("event_key = ${header[event_key]}")
    ;
  }

  private void preprocessAndPublishEventCreatedKPI() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: property = kpi_event_topic_recordmetadata
    //---------
    //IN: property = kpi_event_object
    //IN: property = kpi_event_topic_name
    //IN: property = kpi_status
    //IN: property = kpi_component_route_name
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    // extract kpi_event_topic_offset
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        // extract the offset from response header.  Example format: "[some-topic-0@301]"
        String expectedTopicName = (String)exchange.getProperty("kpi_event_topic_name");

        try {
          // https://kafka.apache.org/30/javadoc/org/apache/kafka/clients/producer/RecordMetadata.html
          Object o = (Object)exchange.getProperty("kpi_event_topic_recordmetadata");
          String recordMetadata = o.toString();

          StringTokenizer tokenizer = new StringTokenizer(recordMetadata, "[@]");

          if (tokenizer.countTokens() == 2) {
            // get first token
            String topicAndPartition = tokenizer.nextToken();

            if (topicAndPartition.startsWith(expectedTopicName)) {
              // this is the metadata we are looking for
              Long offset = Long.parseLong(tokenizer.nextToken());
              exchange.setProperty("kpi_event_topic_offset", offset);
            }
          }
        } catch (Exception e) {
          // failed to retrieve offset. Do nothing.
        }
      }})
    .to("direct:publishEventKPI")
    ;
  }

  private void publishEventKPI() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: property = kpi_event_object
    //IN: property = kpi_event_topic_name
    //IN: property = kpi_event_topic_offset
    //IN: property = kpi_status
    //IN: property = kpi_component_route_name
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {        
        BaseEvent event = (BaseEvent)exchange.getProperty("kpi_event_object");
        String kpi_status = (String) exchange.getProperty("kpi_status");

        // KPI
        EventKPI kpi = new EventKPI(event, kpi_status);
        kpi.setEvent_topic_name((String)exchange.getProperty("kpi_event_topic_name"));
        kpi.setEvent_topic_offset(exchange.getProperty("kpi_event_topic_offset"));
        kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
        kpi.setComponent_route_name((String)exchange.getProperty("kpi_component_route_name"));
        exchange.getMessage().setBody(kpi);
      }
    })
    .marshal().json(JsonLibrary.Jackson, EventKPI.class)
    .log("Event kpi: ${body}")
    .to("kafka:{{kafka.topic.kpis.name}}")
    ;
  }
  
  private void publishBodyAsEventKPI() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: body = EventKPI json
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Publishing Event KPI to Kafka ...")
    .log("body: ${body}")
    .to("kafka:{{kafka.topic.kpis.name}}")
    .log("Event KPI published.")
    ;
  }

  
  private void publishChargeAssessmentCaseKPIError() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: property = kpi_object
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setBody(simple("${exchangeProperty.error_event_object}"))
    .unmarshal().json(JsonLibrary.Jackson)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        Object je = (Object)exchange.getIn().getBody();
        Error error = new Error();
        error.setError_dtm(DateTimeUtils.generateCurrentDtm());
        error.setError_summary("Unable to process JUSTIN event.");
        error.setError_details(je);

        // KPI
        EventKPI kpi = new EventKPI(EventKPI.STATUS.EVENT_UNKNOWN);
        kpi.setError(error);
        kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
        kpi.setComponent_route_name((String)exchange.getProperty("kpi_component_route_name"));
        exchange.getMessage().setBody(kpi, EventKPI.class);
      }})

    .setProperty("kpi_object", body())
    .marshal().json(JsonLibrary.Jackson, EventKPI.class)
    .log("Generate kpi event: ${body}")
    // send to the chargeassessmentcase errors topic
    .to("kafka:{{kafka.topic.chargeassessmentcase-errors.name}}")
    .log("kpi event added to chargeassessmentcase errors topic")
    .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
    .setBody(simple("${exchangeProperty.kpi_object}"))
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        EventKPI kpi = exchange.getIn().getBody(EventKPI.class);
        // extract the offset from response header.  Example format: "[some-topic-0@301]"
        String expectedTopicName = (String)exchange.getProperty("kpi_event_topic_name");
        System.out.println(expectedTopicName);

        try {
          // https://kafka.apache.org/30/javadoc/org/apache/kafka/clients/producer/RecordMetadata.html
          Object o = (Object)exchange.getProperty("kpi_event_topic_recordmetadata");
          String recordMetadata = o.toString();
          System.out.println("recordMetadata:"+recordMetadata);

          StringTokenizer tokenizer = new StringTokenizer(recordMetadata, "[@]");

          if (tokenizer.countTokens() == 2) {
            // get first token
            String topicAndPartition = tokenizer.nextToken();

            if (topicAndPartition.startsWith(expectedTopicName)) {
              // this is the metadata we are looking for
              Long offset = Long.parseLong(tokenizer.nextToken());
              exchange.setProperty("kpi_event_topic_offset", offset);
              kpi.setEvent_topic_offset(offset);
              kpi.setEvent_topic_name(expectedTopicName);
            }
          }
        } catch (Exception e) {
          // failed to retrieve offset. Do nothing.
        }
        exchange.getMessage().setBody(kpi, EventKPI.class);
      }})
    .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
    .marshal().json(JsonLibrary.Jackson, EventKPI.class)
    .log("Event kpi: ${body}")
    .to("kafka:{{kafka.topic.kpis.name}}")
    ;
  }


}