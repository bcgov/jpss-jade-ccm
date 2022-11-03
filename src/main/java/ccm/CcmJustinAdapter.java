package ccm;
// To run this integration use:
// kamel run CcmJustinAdapter.java --property file:application.properties --profile openshift
// 
// recover the service location. If you're running on minikube, minikube service platform-http-server --url=true
// curl -d '{}' http://ccm-justin-adapter/courtFileCreated
//

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
import ccm.models.common.event.EventError;
import ccm.models.common.event.EventKPI;
import ccm.models.common.event.SplunkEvent;
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
    logSplunkEvent();
    publishEventKPI();
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
    .to("kafka:{{kafka.topic.courtcases.name}}");

    
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
      .to("https://dev.jag.gov.bc.ca/ords/devj/justinords/dems/v1/health");

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
    //.to("{{justin.host}}/requeueEventById?id=2045")
    //.to("{{justin.host}}/requeueEventById?id=2060")
    //.to("{{justin.host}}/requeueEventById?id=2307") // AGEN_FILE 50431.0734
    //.to("{{justin.host}}/requeueEventById?id=2309") // AUTH_LIST 50431.0734
    //.to("{{justin.host}}/requeueEventById?id=2367") // AGEN_FILE 50433.0734
    //.to("{{justin.host}}/requeueEventById?id=2368") // AUTH_LIST 50433.0734
    //.to("{{justin.host}}/requeueEventById?id=2451") // COURT_FILE 39857

    // JSIT Sep 8
    //.to("{{justin.host}}/requeueEventById?id=2581") // AGEN_FILE 49408.0734 (case name: YOYO, Yammy; SOSO, Yolando ...)
    //.to("{{justin.host}}/requeueEventById?id=2590") // AGEN_FILE 50448.0734 (case name: VADER, Darth)
    //.to("{{justin.host}}/requeueEventById?id=2592") // COURT_FILE 39861 (court file for Vader agency file)

    //.to("{{justin.host}}/requeueEventById?id=2362") // AGEN_FILE 50431.0734
    //.to("{{justin.host}}/requeueEventById?id=2320") // COURT_FILE 39849 (RCC_ID 50431.0734)
    //.to("{{justin.host}}/requeueEventById?id=2327") // APPR (mdoc no 39849; RCC_ID = 50431.0734)
    //.to("{{justin.host}}/requeueEventById?id=2321") // CRN_ASSIGN (mdoc no 39849; RCC_ID 50431.0734)

    // JSIT Sep 29
    //.to("{{justin.host}}/requeueEventById?id=2753") // AGEN_FILE (RCC_ID = 50454.0734)
    //.to("{{justin.host}}/requeueEventById?id=2759") // APPR (mdoc no 39869; RCC_ID = 50444.0734)

    // JSIT Oct 27
    .to("{{justin.host}}/requeueEventById?id=2003") // AGEN_FILE (RCC_ID = 50414.0734)
    ;

  }

  private void requeueJustinEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header = id
    from("platform-http:/" + routeId + "?httpMethodRestrict=PUT")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Re-queueing JUSTIN event: id = ${header.id}")
    .setProperty("id", header("id"))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .toD("https://dev.jag.gov.bc.ca/ords/devj/justinords/dems/v1/requeueEventById?id=${exchangeProperty.id}")
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
    .to("https://dev.jag.gov.bc.ca/ords/devj/justinords/dems/v1/newEventsBatch") // mark all new events as "in progres"
    //.log("Marking all new events in JUSTIN as 'in progress': ${body}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .to("https://dev.jag.gov.bc.ca/ords/devj/justinords/dems/v1/inProgressEvents") // retrieve all "in progress" events
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
    // .to("https://dev.jag.gov.bc.ca/ords/devj/justinords/dems/v1/inProgressEvents")
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
        exchange.getMessage().setHeader("kafka.KEY", be.getEvent_object_id());
      }})
    .setProperty("kpi_event_object", body())
    .marshal().json(JsonLibrary.Jackson, ChargeAssessmentCaseEvent.class)
    .log("Generate converted business event: ${body}")
    .to("kafka:{{kafka.topic.courtcases.name}}")
    .setBody(simple("${exchangeProperty.justin_event}"))
    .setProperty("event_message_id")
      .jsonpath("$.event_message_id")
    .to("direct:confirmEventProcessed")
    .setBody().simple("${routeId}")
    .to("direct:logSplunkEvent")
    .setProperty("kpi_component_route_name", simple(routeId))
    .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
    .to("direct:publishEventKPI")
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
        exchange.getMessage().setHeader("kafka.KEY", be.getEvent_object_id());
      }})
    .setProperty("kpi_event_object", body())
    .marshal().json(JsonLibrary.Jackson, ChargeAssessmentCaseEvent.class)
    .setProperty("business_event", body())
    .log("Generate converted business event: ${body}")
    .to("kafka:{{kafka.topic.courtcases.name}}")
    .setBody(simple("${exchangeProperty.justin_event}"))
    .setProperty("event_message_id")
      .jsonpath("$.event_message_id")
    .to("direct:confirmEventProcessed")
    .setBody().simple("${routeId}")
    .to("direct:logSplunkEvent")
    .setProperty("kpi_component_route_name", simple(routeId))
    .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
    .to("direct:publishEventKPI")
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
        exchange.getMessage().setHeader("kafka.KEY", be.getEvent_object_id());
      }})
    .setProperty("kpi_event_object", body())
    .marshal().json(JsonLibrary.Jackson, ApprovedCourtCaseEvent.class)
    .log("Generate converted business event: ${body}")
    .to("kafka:{{kafka.topic.courtcase-metadatas.name}}")
    .setBody(simple("${exchangeProperty.justin_event}"))
    .setProperty("event_message_id")
      .jsonpath("$.event_message_id")
    .to("direct:confirmEventProcessed")
    .setBody().simple("${routeId}")
    .to("direct:logSplunkEvent")
    .setProperty("kpi_component_route_name", simple(routeId))
    .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
    .to("direct:publishEventKPI")
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
        exchange.getMessage().setHeader("kafka.KEY", be.getEvent_object_id());
      }})
    .setProperty("kpi_event_object", body())
    .marshal().json(JsonLibrary.Jackson, ApprovedCourtCaseEvent.class)
    .log("Generate converted business event: ${body}")
    .to("kafka:{{kafka.topic.courtcase-metadatas.name}}")
    .setBody(simple("${exchangeProperty.justin_event}"))
    .setProperty("event_message_id")
      .jsonpath("$.event_message_id")
    .to("direct:confirmEventProcessed")
    .setBody().simple("${routeId}")
    .to("direct:logSplunkEvent")
    .setProperty("kpi_component_route_name", simple(routeId))
    .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
    .to("direct:publishEventKPI")
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
        exchange.getMessage().setHeader("kafka.KEY", be.getEvent_object_id());
      }})
    .setProperty("kpi_event_object", body())
    .marshal().json(JsonLibrary.Jackson, ApprovedCourtCaseEvent.class)
    .log("Generate converted business event: ${body}")
    .to("kafka:{{kafka.topic.courtcase-metadatas.name}}")
    .setBody(simple("${exchangeProperty.justin_event}"))
    .setProperty("event_message_id")
      .jsonpath("$.event_message_id")
    .to("direct:confirmEventProcessed")
    .setBody().simple("${routeId}")
    .to("direct:logSplunkEvent")
    .setProperty("kpi_component_route_name", simple(routeId))
    .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
    .to("direct:publishEventKPI")
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

        EventError error = new EventError();
        error.setError_dtm(DateTimeUtils.generateCurrentDtm());
        error.setError_details((String)exchange.getProperty("justin_event"));

        BaseEvent event_error = new BaseEvent();
        event_error.setEvent_dtm(je.getEvent_dtm());
        event_error.setEvent_source(ChargeAssessmentCaseEvent.SOURCE.JUSTIN.name());
        event_error.setEvent_error(error);

        exchange.getMessage().setBody(event_error, BaseEvent.class);
      }
    })
    .setProperty("kpi_event_object", body())
    .setBody(simple("${exchangeProperty.justin_event}"))
    .setProperty("event_message_id")
      .jsonpath("$.event_message_id")
    .to("direct:confirmEventProcessed")
    .setProperty("kpi_component_route_name", simple(routeId))
    .setProperty("kpi_status", simple(EventKPI.STATUS.UNKNOWN.name()))
    .to("direct:publishEventKPI")
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
    //.toD("{{justin.host}}/eventStatus?event_message_id=${header.custom_event_message_id}&is_success=T")
    //.to("{{justin.host}}/eventStatus")
    .doTry()
      //.to("{{justin.host}}/eventStatus")
      .toD("{{justin.host}}/eventStatus?event_message_id=${exchangeProperty.event_message_id}&is_success=T")
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
    .toD("{{justin.host}}/agencyFile?rcc_id=${header[number]}")
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
    .toD("{{justin.host}}/authUsers?rcc_id=${header.number}")
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
    .toD("{{justin.host}}/apprSummary?mdoc_justin_no=${header.number}")
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
    .toD("{{justin.host}}/courtFile?mdoc_justin_no=${header.number}")
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
    .toD("{{justin.host}}/crownAssignments?mdoc_justin_no=${header.number}")
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

  private void logSplunkEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId + "-orig")
    .routeId(routeId + "-orig")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .process(new Processor() {
      @Override
      public void process(Exchange ex) {
        SplunkEvent be = new SplunkEvent(ex.getProperty("splunk_event").toString());
        be.setSource("ccm-justin-adapter");
        be.setEvent_object_id(ex.getProperty("event_message_id").toString());

        ex.getMessage().setBody(be, SplunkEvent.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, SplunkEvent.class)
    .log("Logging event to splunk body: ${body}")
    //.to("kafka:{{kafka.topic.kpis.name}}")
    ;

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Route activated - need to replace this")
    ;
  }

  private void publishEventKPI() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: property = kpi_event_object
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
        kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
        kpi.setComponent_route_name((String)exchange.getProperty("kpi_component_route_name"));
        exchange.getMessage().setBody(kpi);
      }
    })
    .marshal().json(JsonLibrary.Jackson, EventKPI.class)
    .log("Event kpi: ${body}")
    //.setBody(simple("{\"kpi_dtm\":\"2022-11-01 17:39:11\",\"kpi_version\":\"1.0\",\"kpi_status\":\"CREATED\",\"kpi_key\":\"CommonChargeAssessmentCaseEvent-50414.0734-CHANGED\",\"application_component_name\":\"CcmJustinAdapter\",\"component_route_name\":\"publishEventKPI\",\"event\":{\"event_dtm\":\"2022-11-01 17:39:10\",\"event_version\":\"1.0\",\"event_type\":\"CommonChargeAssessmentCaseEvent\",\"event_status\":\"CHANGED\",\"event_source\":\"JUSTIN\",\"event_object_id\":\"50414.0734\",\"event_error\":null,\"justin_event_message_id\":2003,\"justin_message_event_type_cd\":\"AGEN_FILE\",\"justin_event_dtm\":\"2022-11-01 17:39\",\"justin_fetched_date\":\"NULL\",\"justin_guid\":\"DF51EA80E2C064E8E05400144FFBC109\",\"justin_rcc_id\":\"50414.0734\"}}"))
    .to("kafka:{{kafka.topic.kpis.name}}")
    ;
  }

  // private void logCommonCourtCaseKPIEvent() {
  //   // use method name as route id
  //   String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

  //   from("direct:" + routeId)
  //   .routeId(routeId)
  //   .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
  //   .setProperty("splunk_event", simple("${bodyAs(String)}"))
  //   .log("Processing Splunk event for message: ${exchangeProperty.splunk_event}")
  //   .log("Message event ${exchangeProperty.event_message_id}")
  //   .process(new Processor() {
  //     @Override
  //     public void process(Exchange ex) {
  //       CommonCourtCaseEvent event = (CommonCourtCaseEvent)ex.getProperty("business_event_object");
  //       CommonKPIEvent kpiEvent = new CommonKPIEvent(event);
  //       kpiEvent.setApplication_component_name("ccm-justin-adapter");
  //       kpiEvent.setComponent_route_id(routeId);

  //       //ex.getMessage().setBody(be, CommonSplunkEvent.class);
  //     }
  //   })
  //   .marshal().json(JsonLibrary.Jackson, CommonSplunkEvent.class)
  //   .log("Logging event to splunk body: ${body}")
  //   .to("kafka:{{kafka.topic.kpis.name}}")
  //   ;
  // }
}