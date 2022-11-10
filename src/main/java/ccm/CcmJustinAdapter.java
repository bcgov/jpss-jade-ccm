package ccm;
// To run this integration use:
// kamel run CcmJustinAdapter.java --property file:application.properties --profile openshift
// 
// recover the service location. If you're running on minikube, minikube service platform-http-server --url=true
// curl -d '{}' http://ccm-justin-adapter/courtFileCreated
//

import java.util.List;
import java.util.StringTokenizer;

// camel-k: language=java
// camel-k: dependency=mvn:org.apache.camel.quarkus
// camel-k: dependency=mvn:org.apache.camel.component.kafka
// camel-k: dependency=mvn:org.apache.camel.camel-quarkus-kafka
// camel-k: dependency=mvn:org.apache.camel.camel-quarkus-jsonpath
// camel-k: dependency=mvn:org.apache.camel.camel-jackson
// camel-k: dependency=mvn:org.apache.camel.camel-splunk-hec
// camel-k: dependency=mvn:org.apache.camel.camel-splunk
// camel-k: dependency=mvn:org.apache.camel.camel-http
// camel-k: dependency=mvn:org.apache.camel.camel-http-common

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
//import org.apache.camel.component.kafka.KafkaConstants;
//import org.apache.camel.model.;

import ccm.models.common.data.ApprovedCourtCaseData;
import ccm.models.common.data.AuthUsersList;
import ccm.models.common.data.CaseAppearanceSummaryList;
import ccm.models.common.data.CaseCrownAssignmentList;
import ccm.models.common.data.ChargeAssessmentCaseData;
import ccm.models.common.event.ApprovedCourtCaseEvent;
import ccm.models.common.event.BaseEvent;
import ccm.models.common.event.ChargeAssessmentCaseEvent;
import ccm.models.common.event.Error;
import ccm.models.common.event.EventKPI;
import ccm.models.system.justin.*;
import ccm.utils.DateTimeUtils;

public class CcmJustinAdapter extends RouteBuilder {
  @Override
  public void configure() throws Exception {
    courtFileCreated();
    healthCheck();
    //readRCCFileSystem();
    requeueJustinEvent();
    
    processTimer();
    processJustinEventBatch();
    processNewJUSTINEvents();
    processAgenFileEvent();
    processAuthListEvent();
    processCourtFileEvent();
    processApprEvent();
    processCrnAssignEvent();
    processUnknownEvent();

    confirmEventProcessed();
    getCourtCaseDetails();
    getCourtCaseAuthList();
    getCourtCaseMetadata();
    getCourtCaseAppearanceSummaryList();
    getCourtCaseCrownAssignmentList();
    preprocessAndPublishEventCreatedKPI();
    publishEventKPI();
    publishUnknownEventKPIError();
  }

  private void courtFileCreated() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId + "?httpMethodRestrict=POST")
    .routeId(routeId)
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .log("body (before unmarshalling): '${body}'")
    .unmarshal().json()
    .transform(simple("{\"number\": \"${body[number]}\", \"status\": \"created\", \"sensitive_content\": \"${body[sensitive_content]}\", \"public_content\": \"${body[public_content]}\", \"created_datetime\": \"${body[created_datetime]}\"}"))
    .log("body (after unmarshalling): '${body}'")
    .to("kafka:{{kafka.topic.chargeassessmentcases.name}}");

    
  }

  private void healthCheck() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/v1/health?httpMethodRestrict=GET")
      .routeId(routeId)
      .removeHeaders("CamelHttp*")
      .log("/v1/health request received")
      .setHeader(Exchange.HTTP_METHOD, simple("GET"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .to("https://{{justin.host}}/health");

  }

  private void readRCCFileSystem() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("file:/tmp/?fileName=eventBatch-oneRCC.json&exchangePattern=InOnly")
    .routeId(routeId)
    //.log("Processing file with content: ${body}")
    //.to("direct:processJustinEventBatch")
    .log("Re-queueing event(s)...")
    //.removeHeaders("*")
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    //.to("https://{{justin.host}}/requeueEventById?id=2045")
    //.to("https://{{justin.host}}/requeueEventById?id=2060")
    //.to("https://{{justin.host}}/requeueEventById?id=2307") // AGEN_FILE 50431.0734
    //.to("https://{{justin.host}}/requeueEventById?id=2309") // AUTH_LIST 50431.0734
    //.to("https://{{justin.host}}/requeueEventById?id=2367") // AGEN_FILE 50433.0734
    //.to("https://{{justin.host}}/requeueEventById?id=2368") // AUTH_LIST 50433.0734
    //.to("https://{{justin.host}}/requeueEventById?id=2451") // COURT_FILE 39857

    // JSIT Sep 8
    //.to("https://{{justin.host}}/requeueEventById?id=2581") // AGEN_FILE 49408.0734 (case name: YOYO, Yammy; SOSO, Yolando ...)
    //.to("https://{{justin.host}}/requeueEventById?id=2590") // AGEN_FILE 50448.0734 (case name: VADER, Darth)
    //.to("https://{{justin.host}}/requeueEventById?id=2592") // COURT_FILE 39861 (court file for Vader agency file)

    //.to("https://{{justin.host}}/requeueEventById?id=2362") // AGEN_FILE 50431.0734
    //.to("https://{{justin.host}}/requeueEventById?id=2320") // COURT_FILE 39849 (RCC_ID 50431.0734)
    //.to("https://{{justin.host}}/requeueEventById?id=2327") // APPR (mdoc no 39849; RCC_ID = 50431.0734)
    //.to("https://{{justin.host}}/requeueEventById?id=2321") // CRN_ASSIGN (mdoc no 39849; RCC_ID 50431.0734)

    // JSIT Sep 29
    //.to("https://{{justin.host}}/requeueEventById?id=2753") // AGEN_FILE (RCC_ID = 50454.0734)
    //.to("https://{{justin.host}}/requeueEventById?id=2759") // APPR (mdoc no 39869; RCC_ID = 50444.0734)

    // JSIT Oct 27
    .to("https://{{justin.host}}/requeueEventById?id=2003") // AGEN_FILE (RCC_ID = 50414.0734)
    ;

  }

  private void requeueJustinEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header = id
    from("platform-http:/" + routeId + "?httpMethodRestrict=PUT")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Re-queueing JUSTIN event: id = ${header.id} ...")
    .setProperty("id", header("id"))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .toD("https://{{justin.host}}/requeueEventById?id=${exchangeProperty.id}")
    .log("Event re-queued.")
    ;
  }

  private void processTimer() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

  from("timer://simpleTimer?period={{notification.check.frequency}}")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("https://{{justin.host}}/newEventsBatch") // mark all new events as "in progres"
    //.log("Marking all new events in JUSTIN as 'in progress': ${body}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .to("https://{{justin.host}}/inProgressEvents") // retrieve all "in progress" events
    //.log("Processing in progress events from JUSTIN: ${body}")
    .to("direct:processJustinEventBatch")
    ;
  }

  private void processJustinEventBatch() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    /* 
     * To kick off processing, execute the following on the 'service/ccm-justin-adapter' pod:
     *    cp /etc/camel/resources/eventBatch-oneRCC.json /tmp
     */
    //from("timer://simpleTimer?period={{notification.check.frequency}}")
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    //from("file:/etc/camel/resources/?fileName=eventBatch-oneRCC.json&noop=true&exchangePattern=InOnly&readLock=none")
    //from("file:/etc/camel/resources/?fileName=eventBatch-empty.json&noop=true&exchangePattern=InOnly&readLock=none")
    //from("file:/etc/camel/resources/?fileName=eventBatch.json&noop=true&exchangePattern=InOnly&readLock=none")
    //.to("splunk-hec://hec.monitoring.ag.gov.bc.ca:8088/services/collector/f38b6861-1947-474b-bf6c-a743f2c6a413?")
    // .to("https://{{justin.host}}/inProgressEvents")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    //.to("direct:processNewJUSTINEvents");
    //.log("Processing new JUSTIN events: ${body}")
    //.unmarshal().json(JsonLibrary.Jackson, JustinEventBatch.class)
    .setProperty("numOfEvents")
      .jsonpath("$.events.length()")
    .choice()
      .when(simple("${exchangeProperty.numOfEvents} > 0"))
        .log("Event batch count: ${exchangeProperty.numOfEvents}")
        .endChoice()
      .end()
    .setProperty("justin_events")
      .jsonpath("$.events")
    .split()
      .jsonpathWriteAsString("$.events")  // https://stackoverflow.com/questions/51124978/splitting-a-json-array-with-camel
      .setProperty("message_event_type_cd")
        .jsonpath("$.message_event_type_cd")
      .setProperty("event_message_id")
        .jsonpath("$.event_message_id")
      .log("Event batch record: (id=${exchangeProperty.event_message_id}, type=${exchangeProperty.message_event_type_cd})")
      .choice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.AGEN_FILE))
          .to("direct:processAgenFileEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.AUTH_LIST))
          .to("direct:processAuthListEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.COURT_FILE))
          .to("direct:processCourtFileEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.APPR))
          .to("direct:processApprEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.CRN_ASSIGN))
          .to("direct:processCrnAssignEvent")
          .endChoice()
        .otherwise()
          .log("message_event_type_cd = ${exchangeProperty.message_event_type_cd}")
          .to("direct:processUnknownEvent")
          .endChoice()
        .end()
    ;
  }

  private void processNewJUSTINEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //.to("direct:processOneJUSTINEvent");
    // https://github.com/json-path/JsonPath
    //JustinEventBatchProcessor jp = new JustinEventBatchProcessor();

    from("direct:" + routeId)
    .routeId(routeId)
    .log("Processing new JUSTIN events: ${body}")
    .unmarshal().json(JsonLibrary.Jackson, JustinEventBatch.class)
    .process(new Processor() {
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

            if (e.isAgenFileEvent()) {
              // court case changed.  generate new business event.
              ChargeAssessmentCaseEvent bce = new ChargeAssessmentCaseEvent(e);
              System.out.println(" Generating 'Court Case Changed' event (RCC_ID = '" + bce.getJustin_rcc_id() + "')..");
            } else if (e.isAuthListEvent()) {
              // auth list changed.  Generate new business event.
              ChargeAssessmentCaseEvent bce = new ChargeAssessmentCaseEvent(e);
              System.out.println(" Generating 'Court Case Auth List Changed' event (RCC_ID = '" + bce.getJustin_rcc_id() + "')..");
            } else if (e.isCourtFileEvent()) {
              // court file changed.  Generate new business event.
              ApprovedCourtCaseEvent bcme = new ApprovedCourtCaseEvent(e);
              System.out.println(" Generating 'Court Case Metadata Changed' event (MDOC_NO = '" + bcme.getJustin_mdoc_no() + "')..");
            } else {
              System.out.println(" Unknown JUSTIN event type; Do nothing.");
            }
          }
        }
        exchange.getMessage().setBody("OK");
      }
    })
    .log("Getting ready to send to Kafka: ${body}")
    ;
  }

  private void processAgenFileEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("justin_event").body()
    .log("Processing AGEN_FILE event: ${exchangeProperty.justin_event}")
    .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        // Insert code that gets executed *before* delegating
        // to the next processor in the chain.
    
        JustinEvent je = exchange.getIn().getBody(JustinEvent.class);
    
        ChargeAssessmentCaseEvent be = new ChargeAssessmentCaseEvent(je);
    
        exchange.getMessage().setBody(be, ChargeAssessmentCaseEvent.class);
        exchange.getMessage().setHeader("kafka.KEY", be.getEvent_key());
      }})
    .setProperty("kpi_event_object", body())
    .marshal().json(JsonLibrary.Jackson, ChargeAssessmentCaseEvent.class)
    .log("Generate converted business event: ${body}")
    .to("kafka:{{kafka.topic.chargeassessmentcases.name}}")
    .setProperty("kpi_event_topic_name", simple("{{kafka.topic.chargeassessmentcases.name}}"))
    .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
    .setBody(simple("${exchangeProperty.justin_event}"))
    .setProperty("event_message_id")
      .jsonpath("$.event_message_id")
    .to("direct:confirmEventProcessed")
    .setProperty("kpi_component_route_name", simple(routeId))
    .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
    .to("direct:preprocessAndPublishEventCreatedKPI")
    ;
  }

  private void processAuthListEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("justin_event").body()
    .log("Processing AUTH_LIST event: ${exchangeProperty.justin_event}")
    .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        // Insert code that gets executed *before* delegating
        // to the next processor in the chain.
    
        JustinEvent je = exchange.getIn().getBody(JustinEvent.class);
    
        ChargeAssessmentCaseEvent be = new ChargeAssessmentCaseEvent(je);
    
        exchange.getMessage().setBody(be, ChargeAssessmentCaseEvent.class);
        exchange.getMessage().setHeader("kafka.KEY", be.getEvent_key());
      }})
    .setProperty("kpi_event_object", body())
    .marshal().json(JsonLibrary.Jackson, ChargeAssessmentCaseEvent.class)
    .setProperty("business_event", body())
    .log("Generate converted business event: ${body}")
    .to("kafka:{{kafka.topic.chargeassessmentcases.name}}")
    .setProperty("kpi_event_topic_name", simple("{{kafka.topic.chargeassessmentcases.name}}"))
    .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
    .setBody(simple("${exchangeProperty.justin_event}"))
    .setProperty("event_message_id")
      .jsonpath("$.event_message_id")
    .to("direct:confirmEventProcessed")
    .setProperty("kpi_component_route_name", simple(routeId))
    .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
    .to("direct:preprocessAndPublishEventCreatedKPI")
    ;
  }

  private void processCourtFileEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("justin_event").body()
    .log("Processing COURT_FILE event: ${exchangeProperty.justin_event}")
    .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        // Insert code that gets executed *before* delegating
        // to the next processor in the chain.
    
        JustinEvent je = exchange.getIn().getBody(JustinEvent.class);
    
        ApprovedCourtCaseEvent be = new ApprovedCourtCaseEvent(je);
    
        exchange.getMessage().setBody(be, ApprovedCourtCaseEvent.class);
        exchange.getMessage().setHeader("kafka.KEY", be.getEvent_key());
      }})
    .setProperty("kpi_event_object", body())
    .marshal().json(JsonLibrary.Jackson, ApprovedCourtCaseEvent.class)
    .log("Generate converted business event: ${body}")
    .to("kafka:{{kafka.topic.approvedcourtcases.name}}") 
    .setProperty("kpi_event_topic_name", simple("{{kafka.topic.approvedcourtcases.name}}"))
    .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
    .setBody(simple("${exchangeProperty.justin_event}"))
    .setProperty("event_message_id")
      .jsonpath("$.event_message_id")
    .to("direct:confirmEventProcessed")
    .setProperty("kpi_component_route_name", simple(routeId))
    .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
    .to("direct:preprocessAndPublishEventCreatedKPI")
    ;
  }

  private void processApprEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("justin_event").body()
    .log("Processing APPR event: ${body}")
    .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        // Insert code that gets executed *before* delegating
        // to the next processor in the chain.
    
        JustinEvent je = exchange.getIn().getBody(JustinEvent.class);
    
        ApprovedCourtCaseEvent be = new ApprovedCourtCaseEvent(je);
    
        exchange.getMessage().setBody(be, ApprovedCourtCaseEvent.class);
        exchange.getMessage().setHeader("kafka.KEY", be.getEvent_key());
      }})
    .setProperty("kpi_event_object", body())
    .marshal().json(JsonLibrary.Jackson, ApprovedCourtCaseEvent.class)
    .log("Generate converted business event: ${body}")
    .to("kafka:{{kafka.topic.approvedcourtcases.name}}")
    .setProperty("kpi_event_topic_name", simple("{{kafka.topic.approvedcourtcases.name}}"))
    .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
    .setBody(simple("${exchangeProperty.justin_event}"))
    .setProperty("event_message_id")
      .jsonpath("$.event_message_id")
    .to("direct:confirmEventProcessed")
    .setProperty("kpi_component_route_name", simple(routeId))
    .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
    .to("direct:preprocessAndPublishEventCreatedKPI")
    ;
  }

  private void processCrnAssignEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("justin_event").body()
    .log("Processing CRN_ASSIGN event: ${body}")
    .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        // Insert code that gets executed *before* delegating
        // to the next processor in the chain.
    
        JustinEvent je = exchange.getIn().getBody(JustinEvent.class);
    
        ApprovedCourtCaseEvent be = new ApprovedCourtCaseEvent(je);
    
        exchange.getMessage().setBody(be, ApprovedCourtCaseEvent.class);
        exchange.getMessage().setHeader("kafka.KEY", be.getEvent_key());
      }})
    .setProperty("kpi_event_object", body())
    .marshal().json(JsonLibrary.Jackson, ApprovedCourtCaseEvent.class)
    .log("Generate converted business event: ${body}")
    .to("kafka:{{kafka.topic.approvedcourtcases.name}}")
    .setProperty("kpi_event_topic_name", simple("{{kafka.topic.approvedcourtcases.name}}"))
    .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
    .setBody(simple("${exchangeProperty.justin_event}"))
    .setProperty("event_message_id")
      .jsonpath("$.event_message_id")
    .to("direct:confirmEventProcessed")
    .setProperty("kpi_component_route_name", simple(routeId))
    .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
    .to("direct:preprocessAndPublishEventCreatedKPI")
    ;
  }

  private void processUnknownEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Ignoring unknown event: ${body}")
    .setProperty("justin_event", body())
    .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        JustinEvent je = exchange.getIn().getBody(JustinEvent.class);

        Error error = new Error();
        error.setError_dtm(DateTimeUtils.generateCurrentDtm());
        error.setError_summary("Unable to process unknown JUSTIN event.");
        error.setError_details(je);

        exchange.getMessage().setBody(error, Error.class);
      }
    })
    .setProperty("kpi_error_object", body())
    .setBody(simple("${exchangeProperty.justin_event}"))
    .setProperty("event_message_id")
      .jsonpath("$.event_message_id")
    .to("direct:confirmEventProcessed")
    .setProperty("kpi_component_route_name", simple(routeId))
    .to("direct:publishUnknownEventKPIError")
    ;
  }

  private void confirmEventProcessed() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeaders("*")
    .setProperty("event_message_id")
      .jsonpath("$.event_message_id")
    .setProperty("message_event_type_cd")
      .jsonpath("$.message_event_type_cd")
    .log("Marking event ${exchangeProperty.event_message_id} (${exchangeProperty.message_event_type_cd}) as processed.")
    //.removeHeader("message_event_type_cd")
    //.removeHeader("event_message_id")
    //.removeHeader("is_success")
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    //.toD("https://{{justin.host}}/eventStatus?event_message_id=${header.custom_event_message_id}&is_success=T")
    //.to("https://{{justin.host}}/eventStatus")
    .doTry()
      //.to("https://{{justin.host}}/eventStatus")
      .toD("https://{{justin.host}}/eventStatus?event_message_id=${exchangeProperty.event_message_id}&is_success=T")
    .doCatch(Exception.class)
      .log("Exception: ${exception}")
      .log("Exchange Context: ${exchange.context}")
      .choice()
        //.when(header(Exchange.HTTP_RESPONSE_CODE).isEqualTo("404"))
        .when().simple("${exception.statusCode} == 400")
          .log(LoggingLevel.INFO,"Bad request.  HTTP response code = ${exception.statusCode}")
          .log("Exception: '${exception}'")
          .log("Headers: '${headers}'")
        .endChoice()
        .otherwise()
          .log(LoggingLevel.ERROR,"Unknown error.  HTTP response code = ${exception.statusCode}")
          .log("Headers: '${headers}'")
        .endChoice()
      .end()
    .end()
    ;
  }

  private void getCourtCaseDetails() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId + "?httpMethodRestrict=GET")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("getCourtCaseDetails request received. number = ${header[number]}")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .removeHeader("rcc_id")
    .toD("https://{{justin.host}}/agencyFile?rcc_id=${header[number]}")
    .log("Received response from JUSTIN: '${body}'")
    .unmarshal().json(JsonLibrary.Jackson, JustinAgencyFile.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        JustinAgencyFile j = exchange.getIn().getBody(JustinAgencyFile.class);
        ChargeAssessmentCaseData b = new ChargeAssessmentCaseData(j);
        exchange.getMessage().setBody(b, ChargeAssessmentCaseData.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, ChargeAssessmentCaseData.class)
    .log("Converted response (from JUSTIN to Business model): '${body}'")
    ;
  }

  private void getCourtCaseAuthList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId + "?httpMethodRestrict=GET")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("getCourtCaseAuthList request received. rcc_id = ${header.number}")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .toD("https://{{justin.host}}/authUsers?rcc_id=${header.number}")
    .log("Received response from JUSTIN: '${body}'")
    .unmarshal().json(JsonLibrary.Jackson, JustinAuthUsersList.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        JustinAuthUsersList j = exchange.getIn().getBody(JustinAuthUsersList.class);
        AuthUsersList b = new AuthUsersList(j);
        exchange.getMessage().setBody(b, AuthUsersList.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, AuthUsersList.class)
    .log("Converted response (from JUSTIN to Business model): '${body}'")
    ;
  }

  private void getCourtCaseAppearanceSummaryList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("getCourtCaseAppearanceSummaryList request received. mdoc_no = ${header.number}")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .toD("https://{{justin.host}}/apprSummary?mdoc_justin_no=${header.number}")
    .log("Received response from JUSTIN: '${body}'")
    .unmarshal().json(JsonLibrary.Jackson, JustinCourtAppearanceSummaryList.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        JustinCourtAppearanceSummaryList j = exchange.getIn().getBody(JustinCourtAppearanceSummaryList.class);
        CaseAppearanceSummaryList b = new CaseAppearanceSummaryList(j);
        exchange.getMessage().setBody(b, CaseAppearanceSummaryList.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, CaseAppearanceSummaryList.class)
    .log("Converted response (from JUSTIN to Business model): '${body}'")
    ;
  }

  private void getCourtCaseMetadata() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("getCourtCaseMetadata request received. mdoc_no = ${header.number}")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .toD("https://{{justin.host}}/courtFile?mdoc_justin_no=${header.number}")
    .log("Received response from JUSTIN: '${body}'")
    .unmarshal().json(JsonLibrary.Jackson, JustinCourtFile.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        JustinCourtFile j = exchange.getIn().getBody(JustinCourtFile.class);
        ApprovedCourtCaseData b = new ApprovedCourtCaseData(j);
        exchange.getMessage().setBody(b, ApprovedCourtCaseData.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, ApprovedCourtCaseData.class)
    .log("Converted response (from JUSTIN to Business model): '${body}'")
    ;
  }

  private void getCourtCaseCrownAssignmentList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("getCourtCaseCrownAssignmentList request received. mdoc_no = ${header.number}")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .toD("https://{{justin.host}}/crownAssignments?mdoc_justin_no=${header.number}")
    .log("Received response from JUSTIN: '${body}'")
    .unmarshal().json(JsonLibrary.Jackson, JustinCrownAssignmentList.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        JustinCrownAssignmentList j = exchange.getIn().getBody(JustinCrownAssignmentList.class);
        CaseCrownAssignmentList b = new CaseCrownAssignmentList(j);
        exchange.getMessage().setBody(b, CaseCrownAssignmentList.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, CaseCrownAssignmentList.class)
    .log("Converted response (from JUSTIN to Business model): '${body}'")
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

  private void publishUnknownEventKPIError() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: property = kpi_error_object
    //IN: property = kpi_component_route_name
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {        
        Error error = (Error)exchange.getProperty("kpi_error_object");

        // KPI
        EventKPI kpi = new EventKPI(EventKPI.STATUS.UNKNOWN);
        kpi.setError(error);
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