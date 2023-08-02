package ccm;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Base64;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;

import org.apache.camel.CamelException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.http.base.HttpOperationFailedException;

// import org.apache.camel.component.http4.HttpOperationFailedException;
// import org.apache.camel.component.http4.HttpMethods;

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
// camel-k: dependency=mvn:org.apache.camel.camel-http4
// camel-k: dependency=mvn:org.apache.camel.camel-http-common

//import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import ccm.models.common.data.ChargeAssessmentData;
import ccm.models.common.data.ChargeAssessmentDataRef;
import ccm.models.common.data.CourtCaseData;
import ccm.models.common.event.CourtCaseEvent;
import ccm.models.common.event.BaseEvent;
import ccm.models.common.event.CaseUserEvent;
import ccm.models.common.event.ChargeAssessmentEvent;
import ccm.models.common.event.Error;
import ccm.models.common.event.EventKPI;
import ccm.models.common.event.ReportEvent;
import ccm.utils.DateTimeUtils;
import ccm.utils.KafkaComponentUtils;

public class CcmNotificationService extends RouteBuilder {
  @Override
  public void configure() throws Exception {

    attachExceptionHandlers();

    processChargeAssessmentEvents();
    processCourtCaseEvents();
    processChargeAssessmentChanged();
    processManualChargeAssessmentChanged();
    processChargeAssessmentCreated();
    processChargeAssessmentUpdated();
    processCourtCaseAuthListChanged();
    processCourtCaseAuthListUpdated();
    processCourtCaseChanged();
    processPrimaryCourtCaseChanged();
    processManualCourtCaseChanged();
    processCaseMerge();
    processCourtCaseAppearanceChanged();
    processCourtCaseCrownAssignmentChanged();
    processCaseUserEvents();
    processBulkCaseUserEvents();
    processCaseUserAccessAdded();
    processCaseUserAccessRemoved();
    processUnknownStatus();
    preprocessAndPublishEventCreatedKPI();
    publishEventKPI();
    publishBodyAsEventKPI();
  }

  private void attachExceptionHandlers() {

   // handle network connectivity errors
   onException(ConnectException.class, SocketTimeoutException.class)
     .backOffMultiplier(2)
     .log(LoggingLevel.ERROR,"onException(ConnectException, SocketTimeoutException) called.")
     .setBody(constant("An unexpected network error occurred"))
     .retryAttemptedLogLevel(LoggingLevel.ERROR)
     .handled(true)
     .end();

    // HttpOperation Failed
    onException(HttpOperationFailedException.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        BaseEvent event = (BaseEvent)exchange.getProperty("kpi_event_object");
        HttpOperationFailedException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);

        Error error = new Error();
        error.setError_dtm(DateTimeUtils.generateCurrentDtm());
        error.setError_code("HttpOperationFailed: " + cause.getStatusCode());
        error.setError_summary(cause.getMessage());
        
        if(cause != null && !cause.getResponseBody().isEmpty()) {
          error.setError_details(cause.getResponseBody());
        } else if(cause != null && cause.getResponseHeaders().get("CCMExceptionEncoded") != null) {
          byte[] decodedException = Base64.getDecoder().decode(cause.getResponseHeaders().get("CCMExceptionEncoded"));
          String decodedString = new String(decodedException);
          log.error(decodedString);
          error.setError_details(decodedString);
        } else if(cause != null && cause.getResponseHeaders().get("CCMException") != null) {
          log.error(cause.getResponseHeaders().get("CCMException"));
          error.setError_details(cause.getResponseHeaders().get("CCMException"));
        }

        log.error("HttpOperationFailed caught, exception message : " + cause.getMessage());
        //for(StackTraceElement trace : cause.getStackTrace())
        //{
        // log.error(trace.toString());
        //}
        log.error("Returned status code : " + cause.getStatusCode());
        log.error("HttpOperationFailed Exception event info : " + event.getEvent_source());
        exchange.setProperty("error_status_code", cause.getStatusCode());

        // KPI
        EventKPI kpi = new EventKPI(event, EventKPI.STATUS.EVENT_PROCESSING_FAILED);
        kpi.setEvent_topic_name((String)exchange.getProperty("kpi_event_topic_name"));
        kpi.setEvent_topic_offset(exchange.getProperty("kpi_event_topic_offset"));
        kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
        kpi.setComponent_route_name((String)exchange.getProperty("kpi_component_route_name"));
        kpi.setError(error);
        exchange.getMessage().setBody(kpi);
      }
    })
    .marshal().json(JsonLibrary.Jackson, EventKPI.class)
    .log(LoggingLevel.ERROR,"Publishing derived event KPI in Exception handler ...")
    .log(LoggingLevel.DEBUG,"Derived event KPI published.")
    .log(LoggingLevel.INFO,"Caught HttpOperationFailed exception")
    .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
    .setProperty("error_event_object", body())
    .handled(true)
    .to("kafka:{{kafka.topic.kpis.name}}")
    //re-queue based on the event type
    /*.choice()
      .when(simple("${exchangeProperty.error_status_code} == '503'"))
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            BaseEvent event = (BaseEvent)exchange.getProperty("kpi_event_object");
            log.error("toString event: " + event.toString());

            String kafkaTopic = getKafkaTopicByEventType(event.getEvent_type());
            exchange.setProperty("kafka_topic_name", kafkaTopic);
          }
        })
        .log(LoggingLevel.INFO, "retry the message")
        .log(LoggingLevel.INFO, "${exchangeProperty.kafka_topic_name}")
        .setBody(header("event"))
            .to("kafka:{{kafka.topic.caseusers.name}}")

      .end()*/

    .end();

    // Camel Exception
    onException(CamelException.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        BaseEvent event = (BaseEvent)exchange.getProperty("kpi_event_object");
        Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        Error error = new Error();
        error.setError_dtm(DateTimeUtils.generateCurrentDtm());
        error.setError_code("CamelException");
        error.setError_summary("Unable to process event, CamelException raised.");
        error.setError_details(cause.getMessage());

        log.error("CamelException caught, exception message : " + cause.getMessage() + " stack trace : " + cause.getStackTrace());
        log.error("CamelException Exception event info : " + event.getEvent_source());

        // KPI
        EventKPI kpi = new EventKPI(event, EventKPI.STATUS.EVENT_PROCESSING_FAILED);

        kpi.setEvent_topic_name((String)exchange.getProperty("kpi_event_topic_name"));
        kpi.setEvent_topic_offset(exchange.getProperty("kpi_event_topic_offset"));
        kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
        kpi.setComponent_route_name((String)exchange.getProperty("kpi_component_route_name"));
        kpi.setError(error);
        exchange.getMessage().setBody(kpi);
      }
    })
    .marshal().json(JsonLibrary.Jackson, EventKPI.class)
    .log(LoggingLevel.ERROR,"Publishing error event KPI in Exception handler ...")
    .log("Caught CamelException exception")
    .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
    .setProperty("error_event_object", body())
    .to("kafka:{{kafka.topic.kpis.name}}")
    .log(LoggingLevel.DEBUG,"Derived event KPI published.")
    .handled(true)
    .end();

    // General Exception
     onException(Exception.class)
     .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        BaseEvent event = (BaseEvent)exchange.getProperty("kpi_event_object");
        Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        Error error = new Error();
        log.error("General Exception body: " + exchange.getMessage().getBody());
        error.setError_dtm(DateTimeUtils.generateCurrentDtm());
        error.setError_summary("Unable to process event. General exception raised.");
        error.setError_code("General Exception");
        error.setError_details(cause.getMessage());

        log.error("General Exception class and local msg : " + cause.getClass().getName() + " message : " + cause.getLocalizedMessage());

        log.error("General Exception caught, exception message : " + cause.getMessage());
        log.error("General Exception event info : " + event.getEvent_source());
        //for(StackTraceElement trace : cause.getStackTrace())
        //{
        // log.error(trace.toString());
        //}
        // KPI
        EventKPI kpi = new EventKPI(event, EventKPI.STATUS.EVENT_PROCESSING_FAILED);

        kpi.setEvent_topic_name((String)exchange.getProperty("kpi_event_topic_name"));
        kpi.setEvent_topic_offset(exchange.getProperty("kpi_event_topic_offset"));
        kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
        kpi.setComponent_route_name((String)exchange.getProperty("kpi_component_route_name"));
        kpi.setError(error);
        exchange.getMessage().setBody(kpi);
      }
    })
    //.log(LoggingLevel.INFO, "Headers: ${headers}")
    .marshal().json(JsonLibrary.Jackson, EventKPI.class)
    .log(LoggingLevel.ERROR,"Publishing derived event KPI in Exception handler ...")
    .log(LoggingLevel.DEBUG,"Derived event KPI published.")
    .log("Caught General exception exception")
    .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
    .setProperty("error_event_object", body())
    .to("kafka:{{kafka.topic.kpis.name}}")
    .handled(true)
    .end();

  }

  private void processChargeAssessmentEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //from("kafka:{{kafka.topic.chargeassessments.name}}?groupId=ccm-notification-service")
    from("kafka:{{kafka.topic.chargeassessments.name}}?groupId=ccm-notification-service")
    .routeId(routeId)
    .log(LoggingLevel.INFO,"Event from Kafka {{kafka.topic.chargeassessments.name}} topic (offset=${headers[kafka.OFFSET]}): ${body}\n" +
      "    on the topic ${headers[kafka.TOPIC]}\n" +
      "    on the partition ${headers[kafka.PARTITION]}\n" +
      "    with the offset ${headers[kafka.OFFSET]}\n" +
      "    with the key ${headers[kafka.KEY]}")
    .setHeader("event_key")
      .jsonpath("$.event_key")
    .setHeader("event_status")
      .jsonpath("$.event_status")
    .setHeader("event_message_id")
      .jsonpath("$.justin_event_message_id")
    .setHeader("event")
      .simple("${body}")
    .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentEvent.class)
    .setProperty("kpi_event_object", body())
    .setProperty("kpi_event_topic_name", simple("${headers[kafka.TOPIC]}"))
    .setProperty("kpi_event_topic_offset", simple("${headers[kafka.OFFSET]}"))
    .marshal().json(JsonLibrary.Jackson, ChargeAssessmentEvent.class)
    .choice()
      .when(header("event_status").isEqualTo(ChargeAssessmentEvent.STATUS.CHANGED))
        .setProperty("kpi_component_route_name", simple("processChargeAssessmentChanged"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processChargeAssessmentChanged")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
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
    .log(LoggingLevel.INFO,"event_key = ${header[event_key]}")
    .log(LoggingLevel.INFO,"Retrieve latest court case details from JUSTIN.")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("number").simple("${header.event_key}")
    .to("http://ccm-lookup-service/getCourtCaseDetails")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .log(LoggingLevel.DEBUG,"Create court case in DEMS.  Court case data = ${body}.")
    .setProperty("courtcase_data", simple("${bodyAs(String)}"))
    .to("http://ccm-dems-adapter/createCourtCase")
    .log(LoggingLevel.DEBUG,"Update court case auth list.")
    .to("direct:processCourtCaseAuthListChanged")
    .log(LoggingLevel.INFO, "Create ReportEvent for Static reports")
    // create Report Event for static type reports.
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        String event_message_id = exchange.getMessage().getHeader("event_message_id", String.class);
        String rcc_id = exchange.getMessage().getHeader("event_key", String.class);
        StringBuilder reportTypesSb = new StringBuilder("");
        reportTypesSb.append(ReportEvent.REPORT_TYPES.NARRATIVE.name() + ",");
        reportTypesSb.append(ReportEvent.REPORT_TYPES.SYNOPSIS.name() + ",");
        reportTypesSb.append(ReportEvent.REPORT_TYPES.CPIC.name() + ",");
        reportTypesSb.append(ReportEvent.REPORT_TYPES.WITNESS_STATEMENT.name() + ",");
        reportTypesSb.append(ReportEvent.REPORT_TYPES.DV_IPV_RISK.name() + ",");
        reportTypesSb.append(ReportEvent.REPORT_TYPES.DM_ATTACHMENT.name() + ",");
        reportTypesSb.append(ReportEvent.REPORT_TYPES.SUPPLEMENTAL.name() + ",");
        reportTypesSb.append(ReportEvent.REPORT_TYPES.ACCUSED_INFO.name() + ",");
        reportTypesSb.append(ReportEvent.REPORT_TYPES.VEHICLE.name());

        ReportEvent re = new ReportEvent();
        re.setJustin_rcc_id(rcc_id);
        re.setEvent_status(ReportEvent.STATUS.REPORT.name());
        re.setEvent_source(ReportEvent.SOURCE.JADE_CCM.name());
        re.setJustin_event_message_id(Integer.parseInt(event_message_id));
        re.setJustin_message_event_type_cd(ReportEvent.STATUS.REPORT.name());
        re.setReport_type(reportTypesSb.toString());
        exchange.getMessage().setBody(re, ReportEvent.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, ReportEvent.class)
    .to("kafka:{{kafka.topic.reports.name}}")
    ;
  }

  private void processCourtCaseEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("kafka:{{kafka.topic.courtcases.name}}?groupId=ccm-notification-service")
    .routeId(routeId)
    .log(LoggingLevel.INFO,"Event from Kafka {{kafka.topic.courtcases.name}} topic (offset=${headers[kafka.OFFSET]}): ${body}\n" +
      "    on the topic ${headers[kafka.TOPIC]}\n" +
      "    on the partition ${headers[kafka.PARTITION]}\n" +
      "    with the offset ${headers[kafka.OFFSET]}\n" +
      "    with the key ${headers[kafka.KEY]}")
    .setHeader("event_key")
      .jsonpath("$.event_key")
    .setHeader("event_status")
      .jsonpath("$.event_status")
    .setHeader("event_message_id")
      .jsonpath("$.justin_event_message_id")
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

  private void processChargeAssessmentChanged() throws HttpOperationFailedException {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN
    // property: event_object
    // property: caseFound
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG,"event_key = ${header[event_key]}")
    .setHeader("number", simple("${header[event_key]}"))
    .to("http://ccm-lookup-service/getCourtCaseExists")
    .unmarshal().json()
    .setProperty("caseFound").simple("${body[id]}")
    .setProperty("autoCreateFlag").simple("{{dems.case.auto.creation}}")
    .log(LoggingLevel.DEBUG,"createOverrideFlag = ${exchangeProperty.createOverrideFlag}")
    .choice()
      .when(simple("${exchangeProperty.autoCreateFlag} == 'true' || ${exchangeProperty.caseFound} != '' || ${exchangeProperty.createOverrideFlag} == 'true'"))
        .process(new Processor() {
          @Override
          public void process(Exchange ex) throws HttpOperationFailedException {
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
        .log(LoggingLevel.DEBUG,"Generating derived court case event: ${body}")
        .to("kafka:{{kafka.topic.chargeassessments.name}}") // only push on topic, if auto creation is true
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
        .setProperty("kpi_event_topic_name", simple("{{kafka.topic.chargeassessments.name}}"))
        .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
        .setProperty("kpi_component_route_name", simple(routeId))
        .to("direct:preprocessAndPublishEventCreatedKPI")
        // KPI: restore previous values
        .setProperty("kpi_event_object", simple("${exchangeProperty.kpi_event_object_orig}"))
        .setProperty("kpi_event_topic_offset", simple("${exchangeProperty.kpi_event_topic_offset_orig}"))
        .setProperty("kpi_event_topic_name", simple("${exchangeProperty.kpi_event_topic_name_orig}"))
        .setProperty("kpi_status", simple("${exchangeProperty.kpi_status_orig}"))
        .setProperty("kpi_component_route_name", simple("${exchangeProperty.kpi_component_route_name_orig}"))
      .endChoice()
    .end()
    ;
    //throw new HttpOperationFailedException("testingCCMNotificationService",404,"Exception raised","CCMNotificationService",null, routeId);
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
    .setProperty("createOverrideFlag", simple("true"))
    .to("direct:processChargeAssessmentChanged")
    ;
  }

  private void processChargeAssessmentUpdated() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG,"event_key = ${header[event_key]}")
    .log(LoggingLevel.INFO,"Retrieve latest court case details from JUSTIN.")
    .setHeader("key").simple("${header.event_key}")
    .setHeader("event_key",simple("${header.event_key}"))
    .log(LoggingLevel.INFO,"Retrieve court case status first")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-lookup-service/getCourtCaseStatusExists")

    .unmarshal().json()
    .choice()
      .when(simple("${body[status]} == 'Active'"))
        .setHeader("number").simple("${header.event_key}")
        .to("http://ccm-lookup-service/getCourtCaseDetails")
        .log(LoggingLevel.DEBUG,"Update court case in DEMS.  Court case data = ${body}.")
        .setProperty("courtcase_data", simple("${bodyAs(String)}"))
        //.to("http://ccm-dems-adapter/updateCourtCase?httpClient.connectTimeout=1&httpClient.connectionRequestTimeout=1&httpClient.socketTimeout=1")
        .setBody(simple("${exchangeProperty.courtcase_data}"))
        .setHeader(Exchange.HTTP_METHOD, simple("POST"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .to("http://ccm-dems-adapter/updateCourtCase")
        .log(LoggingLevel.DEBUG,"Update court case auth list.")
        .to("direct:processCourtCaseAuthListChanged")
      .endChoice()
    .otherwise()
      .log(LoggingLevel.INFO, "DEMS Case is not in Active state, so skip.")
    .end()
    ;
  }

  private void processCourtCaseAuthListChanged() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"event_key = ${header[event_key]}")
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
    .log(LoggingLevel.DEBUG,"event_key = ${header[event_key]}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("number").simple("${header.event_key}")
    .log(LoggingLevel.INFO,"Retrieve court case auth list")
    .to("http://ccm-lookup-service/getCourtCaseAuthList")
    .log(LoggingLevel.DEBUG,"Update court case auth list in DEMS.  Court case auth list = ${body}")
    // JADE-1489 work around #1 -- not sure why body doesn't make it into dems-adapter
    //.log(LoggingLevel.INFO, "headers: ${headers}")
    //.setProperty("authlist_data", simple("${bodyAs(String)}"))
    .log(LoggingLevel.INFO,"start syncCaseUserList call")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-dems-adapter/syncCaseUserList")
    .log(LoggingLevel.INFO,"Completed processCourtCaseAuthListUpdated call")
    ;
  }

  private void processCaseUserEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //from("kafka:{{kafka.topic.chargeassessments.name}}?groupId=ccm-notification-service")
    from("kafka:{{kafka.topic.caseusers.name}}?groupId=ccm-notification-service")
    .routeId(routeId)
    .log(LoggingLevel.INFO,"Event from Kafka {{kafka.topic.caseusers.name}} topic (offset=${headers[kafka.OFFSET]}): ${body}\n" +
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
      .end();
    ;
  }

  private void processBulkCaseUserEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //from("kafka:{{kafka.topic.chargeassessments.name}}?groupId=ccm-notification-service")
    from("kafka:{{kafka.topic.bulk-caseusers.name}}?groupId=ccm-notification-service")
    .routeId(routeId)
    .log(LoggingLevel.INFO,"Event from Kafka {{kafka.topic.bulk-caseusers.name}} topic (offset=${headers[kafka.OFFSET]}): ${body}\n" +
      "    on the topic ${headers[kafka.TOPIC]}\n" +
      "    on the partition ${headers[kafka.PARTITION]}\n" +
      "    with the offset ${headers[kafka.OFFSET]}\n" +
      "    with the key ${headers[kafka.KEY]}")
    .setHeader("event_key")
      .jsonpath("$.event_key")
    .setHeader("event_status")
      .jsonpath("$.event_status")
    .setHeader("justin_rcc_id")
      .jsonpath("$.justin_rcc_id")
    .setHeader("event")
      .simple("${body}")
    .unmarshal().json(JsonLibrary.Jackson, CaseUserEvent.class)
    .setProperty("event_object", body())
    .setProperty("kpi_event_object", body())
    .setProperty("kpi_event_topic_name", simple("${headers[kafka.TOPIC]}"))
    .setProperty("kpi_event_topic_offset", simple("${headers[kafka.OFFSET]}"))
    .marshal().json(JsonLibrary.Jackson, CaseUserEvent.class)
    .choice()
      .when(header("justin_rcc_id").isEqualTo("0"))
        .log(LoggingLevel.INFO, "Ignore message, as this does not contain a valid rcc id")
        .endChoice()
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
    .log(LoggingLevel.INFO,"event_key = ${header[event_key]}")
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        CaseUserEvent event = (CaseUserEvent)exchange.getProperty("event_object");
        exchange.getMessage().setHeader("event_key", event.getJustin_rcc_id());
      }
    })
    .log(LoggingLevel.INFO,"Calling route processCourtCaseAuthListChanged( rcc_id = ${header[event_key]} ) ...")
    .to("direct:processCourtCaseAuthListChanged")
    .log(LoggingLevel.INFO,"Returned from processCourtCaseAuthListChanged().")
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
    .log(LoggingLevel.DEBUG,"event_key = ${header[event_key]}")
    .setHeader("key", simple("${header[event_key]}"))
    .to("http://ccm-lookup-service/getCaseListByUserKey?throwExceptionOnFailure=false")
    .choice()
      .when(simple("${header.CamelHttpResponseCode} == 200"))
        .log(LoggingLevel.DEBUG,"body = '${body}'.")
        .split()
          .jsonpathWriteAsString("$.case_list")
          //The route should not continue through the rest of the cases in the list after an exception has occurred.
          //Will now stop further processing if an exception or failure occurred during processing of an org.apache.camel.
          // The default behavior is to not stop but continue processing till the end.
          .stopOnException()
          .setHeader("rcc_id", jsonpath("$.rcc_id"))
          .choice()
            //only cases containing actual RCC_ID values (not null) should be processed.
            .when(simple("${header[rcc_id]} != null"))
              .setProperty("rcc_id",jsonpath("$.rcc_id"))
              .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentDataRef.class)
              .setHeader("event_key", jsonpath("$.rcc_id"))
              .log(LoggingLevel.DEBUG,"Calling route processCourtCaseAuthListUpdated( rcc_id = ${header[event_key]} ) ...")
              .to("direct:processCourtCaseAuthListUpdated")
              .log(LoggingLevel.DEBUG,"Returned from processCourtCaseAuthListUpdated().")
            .endChoice()
            .otherwise()
            //nothing to do here
            .endChoice()
          .end()
        .end()
      .endChoice()
      .when(simple("${header.CamelHttpResponseCode} == 404"))
          .log(LoggingLevel.INFO,"User (key = ${header.event_key}) not found; Do nothing.")
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
    .log(LoggingLevel.DEBUG,"event_key = ${header[event_key]}")
    .setHeader("key", simple("${header[event_key]}"))
    .to("http://ccm-lookup-service/getCaseListByUserKey?throwExceptionOnFailure=false")
    .choice()
        .when(simple("${header.CamelHttpResponseCode} == 200"))
          .log(LoggingLevel.DEBUG,"body = '${body}'.")
          .split()
            .jsonpathWriteAsString("$.case_list")
            .setProperty("rcc_id",jsonpath("$.rcc_id"))
            .log(LoggingLevel.DEBUG,"Iterating through case list.  case ref = ${body}")
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
            .log(LoggingLevel.DEBUG,"Publishing derived event ${exchangeProperty.derived_event_type} (rcc_id = ${exchangeProperty.rcc_id}) ...")
            .log(LoggingLevel.DEBUG,"body: ${body}")
            .to("kafka:{{kafka.topic.chargeassessments.name}}")
            .setProperty("derived_event_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
            .setProperty("derived_event_topic", simple("{{kafka.topic.chargeassessments.name}}"))
            .log(LoggingLevel.DEBUG,"Derived event published.")
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
            .log(LoggingLevel.DEBUG,"Publishing derived event KPI ...")
            .to("direct:publishBodyAsEventKPI")
            .log(LoggingLevel.DEBUG,"Derived event KPI published.")
            .end()
          .endChoice()
          .when(simple("${header.CamelHttpResponseCode} == 404"))
            .log(LoggingLevel.DEBUG,"User (key = ${header.event_key}) not found; Do nothing.")
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
    .log(LoggingLevel.INFO,"event_key = ${header[event_key]}")
    .setHeader("number", simple("${header[event_key]}"))
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    //.log(LoggingLevel.INFO, "headers: ${headers}")
    .to("http://ccm-lookup-service/getCourtCaseMetadata")
    .log(LoggingLevel.DEBUG,"Retrieved Court Case Metadata from JUSTIN: ${body}")
    // JADE-1489 workaround #2 -- not sure why in this instance the value of ${body} as-is isn't
    //   accessible in the split() block through exchange properties unless converted to String first.
    .setProperty("metadata_data", simple("${bodyAs(String)}"))
    .unmarshal().json(JsonLibrary.Jackson, CourtCaseData.class)
    .setProperty("metadata_object", body())
    .setBody(simple("${exchangeProperty.metadata_data}"))

    .setProperty("event_key_orig", simple("${header[event_key]}"))
    .split()
      .jsonpathWriteAsString("$.related_court_file")
      .setHeader("number", jsonpath("$.mdoc_justin_no"))
      .setHeader(Exchange.HTTP_METHOD, simple("GET"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .to("http://ccm-lookup-service/getCourtCaseMetadata")

      .log(LoggingLevel.DEBUG,"Retrieved related Court Case Metadata from JUSTIN: ${body}")
      .unmarshal().json(JsonLibrary.Jackson, CourtCaseData.class)
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) {
          CourtCaseData bcm = exchange.getIn().getBody(CourtCaseData.class);
          CourtCaseData metadata = (CourtCaseData)exchange.getProperty("metadata_object", CourtCaseData.class);
          List<CourtCaseData> relatedCf = metadata.getRelated_court_cases();
          if(relatedCf == null) {
            relatedCf = new ArrayList<CourtCaseData>();
          }
          relatedCf.add(bcm);
          metadata.setRelated_court_cases(relatedCf);
          exchange.setProperty("metadata_object", metadata);
        }
      })
    .end()
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        CourtCaseData metadata = (CourtCaseData)exchange.getProperty("metadata_object", CourtCaseData.class);

        exchange.getMessage().setBody(metadata, CourtCaseData.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, CourtCaseData.class)

    .setProperty("metadata_data", simple("${bodyAs(String)}"))
    .setBody(simple("${exchangeProperty.metadata_data}"))
    .log(LoggingLevel.DEBUG, "CourtCaseData: ${body}")
    // go through list of rcc_ids and check on the state of the rcc in dems
    // and copy into an array.
    .setProperty("length",jsonpath("$.related_agency_file.length()"))
    .choice()
      .when(simple("${exchangeProperty.length} > 1"))
        // potential merge, since there is > 1 related agencies
        // call merge method which will go through list of agencies,
        // merge the records and inactivate non-primary ones.
        .to("direct:processCaseMerge")
      .endChoice()
    .end()

    // re-set body to the metadata_data json.
    .setBody(simple("${exchangeProperty.metadata_data}"))
    .split()
      .jsonpathWriteAsString("$.related_agency_file")
      .setProperty("rcc_id", jsonpath("$.rcc_id"))
      .setProperty("primary_yn", jsonpath("$.primary_yn"))

      .setHeader("key").simple("${exchangeProperty.rcc_id}")
      .setHeader("event_key",simple("${exchangeProperty.rcc_id}"))
      .log(LoggingLevel.INFO,"Retrieve court case status first")
      .setHeader(Exchange.HTTP_METHOD, simple("GET"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .to("http://ccm-lookup-service/getCourtCaseStatusExists")
      .log(LoggingLevel.INFO, "Dems case status: ${body}")

      .choice()
        .when(simple(" ${exchangeProperty.primary_yn} == 'Y'"))
          // TODO: retrieve the list of court file ids from DEMS, and retrieve the court file objects for those as well, if they
          // are not already listed in the related_court_cases
          
          .setHeader("key", simple("${exchangeProperty.event_key_orig}"))
          .setHeader("event_key", simple("${exchangeProperty.event_key_orig}"))
          .to("direct:processPrimaryCourtCaseChanged")
        .endChoice()
        .otherwise()
          // go through other rccs and check if they exist in dems and is active, if they do, need to do a merge.

        .endChoice()
      .end()

    .end()

    .choice()
      .when(simple(" ${exchangeProperty.createCase} == 'true'"))
        .doTry()
          .log(LoggingLevel.INFO,"Create new crown assignment changed event.")
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
          .log(LoggingLevel.DEBUG,"Generate converted business event: ${body}")
          .to("kafka:{{kafka.topic.courtcases.name}}")

          .setProperty("derived_event_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
          .setProperty("derived_event_topic", simple("{{kafka.topic.courtcases.name}}"))
          .log(LoggingLevel.INFO,"Derived event published.")
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
          .log(LoggingLevel.INFO,"Publishing derived event KPI ...")
          .to("direct:publishBodyAsEventKPI")
          .log(LoggingLevel.INFO,"Derived event KPI published.")

          .log(LoggingLevel.INFO,"Create new appearance summary changed event.")
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
          .log(LoggingLevel.DEBUG,"Generate converted business event: ${body}")
          .to("kafka:{{kafka.topic.courtcases.name}}")

          .setProperty("derived_event_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
          .setProperty("derived_event_topic", simple("{{kafka.topic.courtcases.name}}"))
          .log(LoggingLevel.INFO,"Derived event published.")
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
          .log(LoggingLevel.INFO,"Publishing derived event KPI ...")
          .to("direct:publishBodyAsEventKPI")
          .log(LoggingLevel.INFO,"Derived event KPI published.")

        .doCatch(Exception.class)
          .log(LoggingLevel.INFO,"General Exception thrown.")
          .log(LoggingLevel.INFO,"${exception}")
          .setProperty("error_event_object", body())
          .setProperty("kpi_event_topic_name",simple("{{kafka.topic.general-errors.name}}"))
          .to("direct:publishJustinEventKPIError")
          .process(new Processor() {
            public void process(Exchange exchange) throws Exception {

              throw exchange.getException();
            }
          })
      .end()
      .endChoice()
    .end()

    .log(LoggingLevel.INFO, "Create ReportEvent for Information report")
    // create Report Event for an INFORMATION type report.
    .setBody(simple("${exchangeProperty.metadata_data}"))
    .unmarshal().json(JsonLibrary.Jackson, CourtCaseData.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        CourtCaseData bcm = exchange.getIn().getBody(CourtCaseData.class);
        String event_message_id = exchange.getMessage().getHeader("event_message_id", String.class);
        ReportEvent re = new ReportEvent();
        re.setEvent_status(ReportEvent.STATUS.REPORT.name());
        re.setEvent_key(bcm.getCourt_file_id());
        re.setEvent_source(ReportEvent.SOURCE.JADE_CCM.name());
        re.setJustin_event_message_id(Integer.parseInt(event_message_id));
        re.setJustin_message_event_type_cd(ReportEvent.STATUS.REPORT.name());
        re.setMdoc_justin_no(bcm.getCourt_file_id());
        re.setReport_type(ReportEvent.REPORT_TYPES.INFORMATION.name());
        exchange.getMessage().setBody(re, ReportEvent.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, ReportEvent.class)
    .to("kafka:{{kafka.topic.reports.name}}")
    ;
  }

  private void processPrimaryCourtCaseChanged() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("event_key_orig", simple("${header[event_key]}"))
    .setHeader("number", simple("${exchangeProperty.rcc_id}"))
    .setHeader("event_key", simple("${exchangeProperty.rcc_id}"))
    .to("http://ccm-lookup-service/getCourtCaseExists")
    .unmarshal().json()
    .setProperty("caseFound").simple("${body[id]}")
    .setProperty("autoCreateFlag").simple("{{dems.case.auto.creation}}")
    .log(LoggingLevel.DEBUG,"createOverrideFlag = ${exchangeProperty.createOverrideFlag}")
    .process(new Processor() {
      @Override
        public void process(Exchange ex) {
          String autocreateFlag = ex.getProperty("autoCreateFlag",String.class);
          String createOverrideFlag = ex.getProperty("createOverrideFlag",String.class);
          String caseFound = ex.getProperty("caseFound",String.class);
          Boolean autoCreateBoolean = Boolean.valueOf(autocreateFlag);
          Boolean createOverrideBoolean = Boolean.valueOf(createOverrideFlag);
          Boolean caseFoundBoolean = Boolean.valueOf(caseFound!="");
          log.info("caseFound:"+caseFoundBoolean);
          if((autoCreateBoolean || createOverrideBoolean ) && !caseFoundBoolean){
            ex.setProperty("createCase", "true");
          }else{
            ex.setProperty("createCase", "false");
          }
        }
    })
    .choice()
      .when(simple(" ${exchangeProperty.createCase} == 'true'"))
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
        .log(LoggingLevel.DEBUG,"Generating derived court case event: ${body}")
        .to("direct:processChargeAssessmentCreated")
        // KPI: restore previous values
        .setProperty("kpi_event_object", simple("${exchangeProperty.kpi_event_object_orig}"))
        .setProperty("kpi_event_topic_offset", simple("${exchangeProperty.kpi_event_topic_offset_orig}"))
        .setProperty("kpi_event_topic_name", simple("${exchangeProperty.kpi_event_topic_name_orig}"))
        .setProperty("kpi_status", simple("${exchangeProperty.kpi_status_orig}"))
        .setProperty("kpi_component_route_name", simple("${exchangeProperty.kpi_component_route_name_orig}"))
      .endChoice()
      .otherwise()
        .log(LoggingLevel.DEBUG,"Generating derived court case event: ${body}")
        .to("direct:processCourtCaseAuthListChanged")
      .endChoice()
    .end()
    .to("http://ccm-lookup-service/getCourtCaseExists")// requery if court case exists in DEMS, in case prev logic created the record.
    .unmarshal().json()
    .setProperty("caseFound").simple("${body[id]}")
    .choice()
      .when(simple("${exchangeProperty.caseFound} != ''"))
        .setHeader(Exchange.HTTP_METHOD, simple("GET"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        // grab the case mappings from justin, for overriding case flags.
        .to("http://ccm-lookup-service/getCourtCaseDetails")
        //.log(LoggingLevel.INFO,"Case Flag Mappings court case in DEMS.  Court case data = ${body}.")
        .setProperty("courtcase_data", simple("${bodyAs(String)}"))

        .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentData.class)
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) {
            ChargeAssessmentData b = exchange.getIn().getBody(ChargeAssessmentData.class);
            //log.debug(b.getCase_flags().toString());
            exchange.getMessage().setBody(b.getCase_flags());
          }
        })
        .log(LoggingLevel.DEBUG, "Case Flags: ${body}")
        .setHeader("caseFlags", simple("${body}"))
        // reset the original values
        .setHeader("number", simple("${exchangeProperty.event_key_orig}"))
        .setHeader("event_key", simple("${exchangeProperty.event_key_orig}"))
        .setHeader("rcc_id", simple("${exchangeProperty.rcc_id}"))
        .setHeader("caseFound", simple("${exchangeProperty.caseFound}"))
        .log(LoggingLevel.DEBUG,"Found related court case. Rcc_id: ${header.rcc_id}")
        .setBody(simple("${exchangeProperty.metadata_data}"))
        .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .to("http://ccm-dems-adapter/updateCourtCaseWithMetadata")
        .endChoice()
      .otherwise()
        .log(LoggingLevel.INFO,"Case (rcc_id ${exchangeProperty.rcc_id}) not found; do nothing.")
        .endChoice()
    .end()
    ;
  }

  private void processCaseMerge() {
    // use method name as route id
    //IN: property = metadata_data
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html

    .removeProperties("primary_courtcase_object")
    .setProperty("primary_rcc_id", simple(""))
    
    .setBody(simple("${exchangeProperty.metadata_data}"))
    
    .unmarshal().json(JsonLibrary.Jackson, CourtCaseData.class)

    // look for the primary rcc id in the related agency file list.
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        CourtCaseData ccd = exchange.getIn().getBody(CourtCaseData.class);
        if(ccd != null && ccd.getRelated_agency_file() != null && ccd.getRelated_agency_file().size() > 0) {
          for(ChargeAssessmentDataRef cadr : ccd.getRelated_agency_file()) {
            if(cadr.getPrimary_yn().equalsIgnoreCase("Y")) {
              exchange.setProperty("primary_rcc_id", cadr.getRcc_id());
              break;
            }
          }
        }
      }
    })

    // Grab the justin court case details for the primary rcc id as well as its dems status info.
    .choice()
      .when(simple(" ${exchangeProperty.primary_rcc_id} != ''"))

        .setHeader("number").simple("${exchangeProperty.primary_rcc_id}")
        .setHeader(Exchange.HTTP_METHOD, simple("GET"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .to("http://ccm-lookup-service/getCourtCaseDetails")

        .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentData.class)
        .setProperty("primary_courtcase_object", body())

        .log(LoggingLevel.INFO,"Retrieve dems case id for the primary")

        .setHeader("key").simple("${exchangeProperty.primary_rcc_id}")
        .setHeader("event_key",simple("${exchangeProperty.primary_rcc_id}"))
        .setHeader(Exchange.HTTP_METHOD, simple("GET"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        // make sure that this primary record is not inactivated.
        .to("http://ccm-lookup-service/getCourtCaseStatusExists")
        .log(LoggingLevel.INFO, "primary rcc status lookup: ${body}")
        .unmarshal().json()
        .setProperty("destinationCaseId").simple("${body[id]}")
        .setProperty("destinationCasesStatus").simple("${body[status]}")

      .endChoice()
    .end()

    .setBody(simple("${exchangeProperty.metadata_data}"))
    // go through list of non-primary records and add to array list 
    .split()
      .jsonpathWriteAsString("$.related_agency_file")
      .setProperty("rcc_id", jsonpath("$.rcc_id"))
      .setProperty("primary_yn", jsonpath("$.primary_yn"))

      .choice()
        .when(simple("${exchangeProperty.primary_yn} != 'Y'"))
          .setHeader("number").simple("${exchangeProperty.rcc_id}")
          .setHeader(Exchange.HTTP_METHOD, simple("GET"))
          .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
          .to("http://ccm-lookup-service/getCourtCaseDetails")
          .setProperty("courtcase_data", simple("${bodyAs(String)}"))

          .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentData.class)
          .setProperty("courtcase_object", body())

          .process(new Processor() {
            @Override
            public void process(Exchange exchange) {
              ChargeAssessmentData cad = exchange.getIn().getBody(ChargeAssessmentData.class);
              ChargeAssessmentData assessmentData = (ChargeAssessmentData)exchange.getProperty("primary_courtcase_object", ChargeAssessmentData.class);
              // determine the prefix value for merging the records documents
              String agencyFileName = cad.getAgency_file();
              // remove all special chars from the agency file name
              agencyFileName = agencyFileName.replaceAll(":", "");
              exchange.setProperty("merge_prefix", agencyFileName);

              if(assessmentData != null) {
                List<ChargeAssessmentData> relatedCa = assessmentData.getRelated_charge_assessments();
                if(relatedCa == null) {
                  relatedCa = new ArrayList<ChargeAssessmentData>();
                }
                relatedCa.add(cad);
                assessmentData.setRelated_charge_assessments(relatedCa);

              }
              else {
                // just set the first one in the list as primary, if none found in first iteration.
                assessmentData = cad;
                exchange.setProperty("primary_rcc_id", (String)exchange.getProperty("rcc_id", String.class));
              }
              exchange.setProperty("primary_courtcase_object", assessmentData);
            }
          })

        .endChoice()
        .otherwise()
          .setProperty("primary_rcc_id", simple("${exchangeProperty.rcc_id}"))
        .endChoice()
      .end()


      // Grab the dems status of the non-primary dems case
      .setHeader("key").simple("${exchangeProperty.rcc_id}")
      .setHeader("event_key",simple("${exchangeProperty.rcc_id}"))
      .log(LoggingLevel.INFO,"Retrieve court case status first")
      .setHeader(Exchange.HTTP_METHOD, simple("GET"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      // make sure that this primary record is not inactivated.
      .to("http://ccm-lookup-service/getCourtCaseStatusExists")
      .log(LoggingLevel.INFO, "rcc status lookup: ${body}")
      .unmarshal().json()
      // if the non-primary case is active, then merge it into the primary.
      .setProperty("sourceCaseId").simple("${body[id]}")
      .log(LoggingLevel.INFO, "Source Case Id: ${exchangeProperty.sourceCaseId}")
      .log(LoggingLevel.INFO, "primary vs current: ${exchangeProperty.primary_rcc_id} vs ${exchangeProperty.rcc_id}")

      // if the non primary dems case is still active, make call which will export the records over to the primary rcc
      // and then set the non primary case to no longer be active.
      .choice()
        .when(simple("${exchangeProperty.primary_rcc_id} != ${exchangeProperty.rcc_id} && ${body[status]} == 'Active'"))
          // make call to merge docs and inactivate the non primary one
          .setHeader("sourceCaseId").simple("${exchangeProperty.sourceCaseId}")
          .setHeader("destinationCaseId").simple("${exchangeProperty.destinationCaseId}")
          .setHeader("prefixName").simple("${exchangeProperty.merge_prefix}")
          .to("http://ccm-dems-adapter/mergeCaseRecordsAndInactivateCase")
        .endChoice()
      .end()
    .end()

    // set the updated ChargeAssessmentData object to be the body to make a call to dems to update the court case with the merged data.
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        ChargeAssessmentData metadata = (ChargeAssessmentData)exchange.getProperty("primary_courtcase_object", ChargeAssessmentData.class);

        exchange.getMessage().setBody(metadata, ChargeAssessmentData.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, ChargeAssessmentData.class)

    .setProperty("primary_courtcase", simple("${bodyAs(String)}"))
    .setBody(simple("${exchangeProperty.primary_courtcase}"))
    // proceed to update the agency file info in the case.
    //.log(LoggingLevel.INFO, "Agency File: ${body}")
    .choice()
      .when(simple("${exchangeProperty.destinationCaseId} != '' && ${exchangeProperty.destinationCasesStatus} == 'Active'"))
        .log(LoggingLevel.INFO, "Updating dems case with latest merge data")
        .setHeader("number").simple("${exchangeProperty.primary_rcc_id}")
        .setHeader(Exchange.HTTP_METHOD, simple("POST"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .to("http://ccm-dems-adapter/updateCourtCase")
      .endChoice()
    .end()
    .log(LoggingLevel.INFO, "Completed court case update.")

    ;
  }

  private void processManualCourtCaseChanged() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG, "Inside processManualCourtCaseChanged")
    .setProperty("createOverrideFlag", simple("true"))
    .to("direct:processCourtCaseChanged")
    ;
  }

  private void processCourtCaseAppearanceChanged() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG,"event_key = ${header[event_key]}")
    .setHeader("number", simple("${header[event_key]}"))
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-lookup-service/getCourtCaseAppearanceSummaryList")
    .log(LoggingLevel.DEBUG,"Retrieved Court Case appearance summary list from JUSTIN: ${body}")
    // JADE-1489 workaround #2 -- not sure why in this instance the value of ${body} as-is isn't
    //   accessible in the split() block through exchange properties unless converted to String first.
    .setProperty("business_data", simple("${bodyAs(String)}"))
    .to("http://ccm-lookup-service/getCourtCaseMetadata")
    .log(LoggingLevel.DEBUG,"Retrieved Court Case Metadata from JUSTIN: ${body}")
    .setProperty("metadata_data", simple("${bodyAs(String)}"))
    .split()
      .jsonpathWriteAsString("$.related_agency_file")
      .setProperty("rcc_id", jsonpath("$.rcc_id"))
      .log(LoggingLevel.DEBUG,"Check case (rcc_id ${exchangeProperty.rcc_id}) existence ...")
      .setHeader("number", simple("${exchangeProperty.rcc_id}"))
      .to("http://ccm-lookup-service/getCourtCaseExists")
      .unmarshal().json()
      .setProperty("caseId").simple("${body[id]}")
      .choice()
        .when(simple("${exchangeProperty.caseId} != ''"))
          .setHeader("rcc_id", simple("${exchangeProperty.rcc_id}"))
          .log(LoggingLevel.DEBUG,"Found related court case. Rcc_id: ${header.rcc_id}")
          .setBody(simple("${exchangeProperty.business_data}"))
          .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
          .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
          .to("http://ccm-dems-adapter/updateCourtCaseWithAppearanceSummary")
          .endChoice()
        .otherwise()
          .log(LoggingLevel.INFO,"Case (rcc_id ${exchangeProperty.rcc_id}) not found; do nothing.")
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
    .log(LoggingLevel.DEBUG,"event_key = ${header[event_key]}")
    .setHeader("number", simple("${header[event_key]}"))
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-lookup-service/getCourtCaseCrownAssignmentList")
    .log(LoggingLevel.DEBUG,"Retrieved Court Case crown assignment list from JUSTIN: ${body}")
    // JADE-1489 workaround #2 -- not sure why in this instance the value of ${body} as-is isn't
    //   accessible in the split() block through exchange properties unless converted to String first.
    .setProperty("business_data", simple("${bodyAs(String)}"))
    .to("http://ccm-lookup-service/getCourtCaseMetadata")
    .log(LoggingLevel.DEBUG,"Retrieved Court Case Metadata from JUSTIN: ${body}")
    .setProperty("metadata_data", simple("${bodyAs(String)}"))
    .split()
      .jsonpathWriteAsString("$.related_agency_file")
      .setProperty("rcc_id", jsonpath("$.rcc_id"))
      .log(LoggingLevel.DEBUG,"Check case (rcc_id ${exchangeProperty.rcc_id}) existence ...")
      .setHeader("number", simple("${exchangeProperty.rcc_id}"))
      .to("http://ccm-lookup-service/getCourtCaseExists")
      .unmarshal().json()
      .setProperty("caseId").simple("${body[id]}")
      .choice()
        .when(simple("${exchangeProperty.caseId} != ''"))
          .setHeader("rcc_id", simple("${exchangeProperty.rcc_id}"))
          .log(LoggingLevel.DEBUG,"Found related court case. Rcc_id: ${header.rcc_id}")
          .setBody(simple("${exchangeProperty.business_data}"))
          .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
          .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
          .to("http://ccm-dems-adapter/updateCourtCaseWithCrownAssignmentData")
          .endChoice()
        .otherwise()
          .log(LoggingLevel.DEBUG,"Case (rcc_id ${exchangeProperty.rcc_id}) not found; do nothing.")
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
    .log(LoggingLevel.DEBUG,"event_key = ${header[event_key]}")
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
    .log(LoggingLevel.DEBUG,"Event kpi: ${body}")
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
    .log(LoggingLevel.DEBUG,"Publishing Event KPI to Kafka ...")
    .log(LoggingLevel.DEBUG,"body: ${body}")
    .to("kafka:{{kafka.topic.kpis.name}}")
    .log(LoggingLevel.DEBUG,"Event KPI published.")
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
    .log(LoggingLevel.DEBUG,"Generate kpi event: ${body}")
    // send to the chargeassessmentcase errors topic
    .to("kafka:{{kafka.topic.chargeassessment-errors.name}}")
    .log(LoggingLevel.INFO,"kpi event added to chargeassessmentcase errors topic")
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
    .log(LoggingLevel.DEBUG,"Event kpi: ${body}")
    .to("kafka:{{kafka.topic.kpis.name}}")
    ;
  }


}