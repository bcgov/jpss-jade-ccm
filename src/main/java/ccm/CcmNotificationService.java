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
import ccm.models.common.event.EventKPI;

public class CcmNotificationService extends RouteBuilder {
  @Override
  public void configure() throws Exception {

    processCourtcaseEvents();
    processCourtcaseMetadataEvents();
    processChargeAssessmentCaseChanged();
    processCourtCaseCreated();
    processCourtCaseUpdated();
    processCourtCaseAuthListChanged();
    processApprovedCourtCaseChanged();
    processCourtCaseAppearanceChanged();
    processCourtCaseCrownAssignmentChanged();
    processUnknownStatus();
    preprocessAndPublishEventCreatedKPI();
    publishEventKPI();
  }

  private void processCourtcaseEvents() {
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
      .when(header("event_status").isEqualTo(ChargeAssessmentCaseEvent.STATUS.CREATED))
        .setProperty("kpi_component_route_name", simple("processCourtCaseCreated"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processCourtCaseCreated")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .when(header("event_status").isEqualTo(ChargeAssessmentCaseEvent.STATUS.UPDATED))
        .setProperty("kpi_component_route_name", simple("processCourtCaseUpdated"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processCourtCaseUpdated")
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

  private void processCourtCaseCreated() {
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

  private void processCourtcaseMetadataEvents() {
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
        ChargeAssessmentCaseEvent be = new ChargeAssessmentCaseEvent();

        // hardcoding boolean to false for first implementation
        //boolean court_case_exists = ex.getIn().getBody() != null && ex.getIn().getBody().toString().length() > 0;
        boolean court_case_exists = ex.getProperty("caseFound").toString().length() > 0;

        String event_key = ex.getIn().getHeader("event_key").toString();

        be.setEvent_source(ChargeAssessmentCaseEvent.SOURCE.JADE_CCM.toString());
        be.setEvent_key(event_key);
        be.setJustin_rcc_id(event_key);

        if (court_case_exists) {
          be.setEvent_status(ChargeAssessmentCaseEvent.STATUS.UPDATED.toString());
        } else {
          be.setEvent_status(ChargeAssessmentCaseEvent.STATUS.CREATED.toString());
        }

        ex.getMessage().setBody(be);

        // KPI
        ex.setProperty("kpi_event_object", be);
      }
    })
    .marshal().json(JsonLibrary.Jackson, ChargeAssessmentCaseEvent.class)
    .log("Generating derived court case event: ${body}")
    .to("kafka:{{kafka.topic.chargeassessmentcases.name}}")
    .setProperty("kpi_event_topic_name", simple("{{kafka.topic.chargeassessmentcases.name}}"))
    .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
    .setProperty("kpi_component_route_name", simple(routeId))
    .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
    .to("direct:preprocessAndPublishEventCreatedKPI")
    ;
  }

  private void processCourtCaseUpdated() {
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
      .setHeader("rcc_id", jsonpath("$.rcc_id"))
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
        kpi.setEvent_topic_offset(exchange.getProperty("kpi_event_topic_offset").toString());
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
}