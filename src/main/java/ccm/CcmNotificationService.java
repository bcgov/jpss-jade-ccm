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

import ccm.models.common.event.ApprovedCourtCaseEvent;
import ccm.models.common.event.BaseEvent;
import ccm.models.common.event.ChargeAssessmentCaseEvent;
import ccm.models.common.event.Error;
import ccm.models.common.event.EventKPI;
import ccm.utils.DateTimeUtils;

public class CcmNotificationService extends RouteBuilder {
  @Override
  public void configure() throws Exception {

    processChargeAssessmentCaseEvents();
    processApprovedCourtCaseEvents();
    processChargeAssessmentCaseChanged();
    processManualChargeAssessmentCaseChanged();
    processChargeAssessmentCaseCreated();
    processChargeAssessmentCaseUpdated();
    processCourtCaseAuthListChanged();
    processCourtCaseAuthListUpdated();
    processApprovedCourtCaseChanged();
    processManualApprovedCourtCaseChanged();
    processCourtCaseAppearanceChanged();
    processCourtCaseCrownAssignmentChanged();
    processUnknownStatus();
    preprocessAndPublishEventCreatedKPI();
    publishEventKPI();
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
    .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentCaseEvent.class)
    .setProperty("kpi_event_object", body())
    .setProperty("kpi_event_topic_name", simple("${headers[kafka.TOPIC]}"))
    .setProperty("kpi_event_topic_offset", simple("${headers[kafka.OFFSET]}"))
    .marshal().json(JsonLibrary.Jackson, ChargeAssessmentCaseEvent.class)
    .choice()
      .when(header("event_status").isEqualTo(ChargeAssessmentCaseEvent.STATUS.CHANGED))
        .setProperty("kpi_component_route_name", simple("processChargeAssessmentCaseChanged"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processChargeAssessmentCaseChanged")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .when(header("event_status").isEqualTo(ChargeAssessmentCaseEvent.STATUS.MANUALLY_CHANGED))
        .setProperty("kpi_component_route_name", simple("processManualChargeAssessmentCaseChanged"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processManualChargeAssessmentCaseChanged")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .when(header("event_status").isEqualTo(ChargeAssessmentCaseEvent.STATUS.CREATED))
        .setProperty("kpi_component_route_name", simple("processChargeAssessmentCaseCreated"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processChargeAssessmentCaseCreated")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .when(header("event_status").isEqualTo(ChargeAssessmentCaseEvent.STATUS.UPDATED))
        .setProperty("kpi_component_route_name", simple("processChargeAssessmentCaseUpdated"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processChargeAssessmentCaseUpdated")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .when(header("event_status").isEqualTo(ChargeAssessmentCaseEvent.STATUS.AUTH_LIST_CHANGED))
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

  private void processChargeAssessmentCaseCreated() {
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
    .unmarshal().json(JsonLibrary.Jackson, ApprovedCourtCaseEvent.class)
    .setProperty("kpi_event_object", body())
    .setProperty("kpi_event_topic_name", simple("${headers[kafka.TOPIC]}"))
    .setProperty("kpi_event_topic_offset", simple("${headers[kafka.OFFSET]}"))
    .marshal().json(JsonLibrary.Jackson, ApprovedCourtCaseEvent.class)
    .choice()
      .when(header("event_status").isEqualTo(ApprovedCourtCaseEvent.STATUS.CHANGED))
        .setProperty("kpi_component_route_name", simple("processApprovedCourtCaseChanged"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processApprovedCourtCaseChanged")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .when(header("event_status").isEqualTo(ApprovedCourtCaseEvent.STATUS.MANUALLY_CHANGED))
        .setProperty("kpi_component_route_name", simple("processManualApprovedCourtCaseChanged"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processManualApprovedCourtCaseChanged")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .when(header("event_status").isEqualTo(ApprovedCourtCaseEvent.STATUS.APPEARANCE_CHANGED))
        .setProperty("kpi_component_route_name", simple("processCourtCaseAppearanceChanged"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processCourtCaseAppearanceChanged")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .when(header("event_status").isEqualTo(ApprovedCourtCaseEvent.STATUS.CROWN_ASSIGNMENT_CHANGED))
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

  private void processChargeAssessmentCaseChanged() {
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

            ChargeAssessmentCaseEvent original_event = (ChargeAssessmentCaseEvent)ex.getProperty("kpi_event_object");
            ChargeAssessmentCaseEvent derived_event = new ChargeAssessmentCaseEvent(ChargeAssessmentCaseEvent.SOURCE.JADE_CCM, original_event);

            boolean court_case_exists = ex.getProperty("caseFound").toString().length() > 0;

            if (court_case_exists) {
              derived_event.setEvent_status(ChargeAssessmentCaseEvent.STATUS.UPDATED.toString());
            } else {
              derived_event.setEvent_status(ChargeAssessmentCaseEvent.STATUS.CREATED.toString());
            }

            ex.getMessage().setBody(derived_event);

            // KPI: Set new event object
            ex.setProperty("kpi_event_object", derived_event);
          }
        })
        .marshal().json(JsonLibrary.Jackson, ChargeAssessmentCaseEvent.class)
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

  private void processManualChargeAssessmentCaseChanged() {
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

        ChargeAssessmentCaseEvent original_event = (ChargeAssessmentCaseEvent)ex.getProperty("kpi_event_object");
        ChargeAssessmentCaseEvent derived_event = new ChargeAssessmentCaseEvent(ChargeAssessmentCaseEvent.SOURCE.JADE_CCM, original_event);

        boolean court_case_exists = ex.getProperty("caseFound").toString().length() > 0;

        if (court_case_exists) {
          derived_event.setEvent_status(ChargeAssessmentCaseEvent.STATUS.UPDATED.toString());
        } else {
          derived_event.setEvent_status(ChargeAssessmentCaseEvent.STATUS.CREATED.toString());
        }

        ex.getMessage().setBody(derived_event);

        // KPI: Set new event object
        ex.setProperty("kpi_event_object", derived_event);
      }
    })
    .marshal().json(JsonLibrary.Jackson, ChargeAssessmentCaseEvent.class)
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

  private void processChargeAssessmentCaseUpdated() {
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

  private void processApprovedCourtCaseChanged() {
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

            ChargeAssessmentCaseEvent derived_event = new ChargeAssessmentCaseEvent();
            derived_event.setEvent_status(ChargeAssessmentCaseEvent.STATUS.CREATED.toString());
            derived_event.setEvent_source(ChargeAssessmentCaseEvent.SOURCE.JADE_CCM.name());

            ex.getMessage().setBody(derived_event);

            // KPI: Set new event object
            ex.setProperty("kpi_event_object", derived_event);
          }
        })
        .marshal().json(JsonLibrary.Jackson, ChargeAssessmentCaseEvent.class)
        .log("Generating derived court case event: ${body}")
        .to("direct:processChargeAssessmentCaseCreated")
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
      .setHeader("rcc_id", jsonpath("$.rcc_id"))
      .log("Found related court case. Rcc_id: ${header.rcc_id}")
      .setBody(simple("${exchangeProperty.metadata_data}"))
      .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .to("http://ccm-dems-adapter/updateCourtCaseWithMetadata")
    .end()
    ;
  }

  private void processManualApprovedCourtCaseChanged() {
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

            ChargeAssessmentCaseEvent derived_event = new ChargeAssessmentCaseEvent();
            derived_event.setEvent_status(ChargeAssessmentCaseEvent.STATUS.CREATED.toString());
            derived_event.setEvent_source(ChargeAssessmentCaseEvent.SOURCE.JADE_CCM.name());

            ex.getMessage().setBody(derived_event);
          }
        })
        .marshal().json(JsonLibrary.Jackson, ChargeAssessmentCaseEvent.class)
        .log("Generating derived court case event: ${body}")
        .to("direct:processChargeAssessmentCaseCreated")
      .end()

      // reset the original values
      .setHeader("number", simple("${exchangeProperty.event_key_orig}"))
      .setHeader("event_key", simple("${exchangeProperty.event_key_orig}"))

      .setHeader("rcc_id", simple("${exchangeProperty.rcc_id}"))
      .log("Found related court case. Rcc_id: ${header.rcc_id}")
      .setBody(simple("${exchangeProperty.metadata_data}"))
      .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .to("http://ccm-dems-adapter/updateCourtCaseWithMetadata")
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
      .setHeader("rcc_id", jsonpath("$.rcc_id"))
      .log("Found related court case. Rcc_id: ${header.rcc_id}")
      .setBody(simple("${exchangeProperty.business_data}"))
      .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .to("http://ccm-dems-adapter/updateCourtCaseWithAppearanceSummary")
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
      .setHeader("rcc_id", jsonpath("$.rcc_id"))
      .log("Found related court case. Rcc_id: ${header.rcc_id}")
      .setBody(simple("${exchangeProperty.business_data}"))
      .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .to("http://ccm-dems-adapter/updateCourtCaseWithCrownAssignmentData")
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
        EventKPI kpi = new EventKPI(EventKPI.STATUS.UNKNOWN);
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