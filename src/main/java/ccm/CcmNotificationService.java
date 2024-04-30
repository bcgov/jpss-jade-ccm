package ccm;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;

import java.util.Base64;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.camel.CamelException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.http.NoHttpResponseException;

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

import ccm.models.common.data.AuthUser;
import ccm.models.common.data.AuthUserList;
import ccm.models.common.data.CaseAppearanceSummaryList;
import ccm.models.common.data.CaseCrownAssignmentList;
import ccm.models.common.data.ChargeAssessmentData;
import ccm.models.common.data.ChargeAssessmentDataRef;
import ccm.models.common.data.CourtCaseData;
import ccm.models.common.event.CourtCaseEvent;
import ccm.models.common.event.BaseEvent;
import ccm.models.common.event.CaseUserEvent;
import ccm.models.common.event.ChargeAssessmentEvent;
import ccm.models.common.event.Error;
import ccm.models.common.event.EventKPI;
import ccm.models.common.event.ParticipantMergeEvent;
import ccm.models.common.event.ReportEvent;
import ccm.utils.DateTimeUtils;
import ccm.utils.KafkaComponentUtils;

public class CcmNotificationService extends RouteBuilder {
  @Override
  public void configure() throws Exception {

    attachExceptionHandlers();

    processChargeAssessmentEvents();
    processBulkChargeAssessmentEvents();
    createPartIdProvisionCompleted();
    processCourtCaseEvents();
    processChargeAssessmentChanged();
    processManualChargeAssessmentChanged();
    processChargeAssessmentCreated();
    generateStaticReportEvent();
    processChargeAssessmentUpdated();
    updateChargeAssessment();
    processCourtCaseAuthListChanged();
    processCourtCaseAuthListUpdated();
    compileRelatedChargeAssessments();
    compileRelatedCourtFiles();
    processCourtCaseChanged();
    generateInformationReportEvent();
    processPrimaryCourtCaseChanged();
    processManualCourtCaseChanged();
    processCaseMerge();
    processCourtCaseAppearanceChanged();
    processCourtCaseCrownAssignmentChanged();
    processCaseUserEvents();
    processCaseUserAccessRemovedNoDetails();
    http_createBatchEndEvent();
    createBatchEndEventPaused();
    createBatchEndEvent();

    processUnknownStatus();
    preprocessAndPublishEventCreatedKPI();
    publishEventKPI();
    publishBodyAsEventKPI();
    processParticipantMerge();
    processParticipantMergeEvents();
    processAccusedPersons();
  }

  private void attachExceptionHandlers() {

   // handle network connectivity errors
    onException(ConnectException.class, SocketTimeoutException.class)
     .maximumRedeliveries(10).redeliveryDelay(45000)
     .log(LoggingLevel.ERROR,"onException(ConnectException, SocketTimeoutException) called.")
     .setBody(constant("An unexpected network error occurred"))
     .retryAttemptedLogLevel(LoggingLevel.ERROR)
     .handled(true)
    .end();

    onException(NoHttpResponseException.class, NoRouteToHostException.class, UnknownHostException.class)
      .maximumRedeliveries(10).redeliveryDelay(60000)
      .log(LoggingLevel.ERROR,"onException(NoHttpResponseException, NoRouteToHostException) called.")
      .setBody(constant("An unexpected network error occurred"))
      .retryAttemptedLogLevel(LoggingLevel.ERROR)
      .handled(true)
    .end();

    // HttpOperation Failed
    onException(HttpOperationFailedException.class)
    .log(LoggingLevel.ERROR,"onException(HttpOperationFailedException) called.")
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
        kpi.setEvent_topic_partition(exchange.getProperty("kpi_event_topic_partition"));
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
    .log(LoggingLevel.ERROR,"onException(CamelException) called.")
    .maximumRedeliveries(3).redeliveryDelay(30000)
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

        log.error("CamelException caught, exception message : " + cause.getMessage());
        log.error("CamelException Exception event info : " + event.getEvent_source());
        for(StackTraceElement trace : cause.getStackTrace())
        {
         log.error(trace.toString());
        }

        // KPI
        EventKPI kpi = new EventKPI(event, EventKPI.STATUS.EVENT_PROCESSING_FAILED);

        kpi.setEvent_topic_name((String)exchange.getProperty("kpi_event_topic_name"));
        kpi.setEvent_topic_offset(exchange.getProperty("kpi_event_topic_offset"));
        kpi.setEvent_topic_partition(exchange.getProperty("kpi_event_topic_partition"));
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
     .log(LoggingLevel.ERROR,"onException(Exception) called.")
     .maximumRedeliveries(3).redeliveryDelay(30000)
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
        kpi.setEvent_topic_partition(exchange.getProperty("kpi_event_topic_partition"));
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
    from("kafka:{{kafka.topic.chargeassessments.name}}?groupId=ccm-notification-service&consumersCount={{kafka.topic.chargeassessment.consumer.count}}&maxPollRecords=3&maxPollIntervalMs=4800000")
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
    .setHeader("event_message_type")
      .jsonpath("$.justin_message_event_type_cd")
    .setHeader("event")
      .simple("${body}")
    .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentEvent.class)
    .setProperty("kpi_event_object", body())
    .setProperty("kpi_event_topic_name", simple("${headers[kafka.TOPIC]}"))
    .setProperty("kpi_event_topic_offset", simple("${headers[kafka.OFFSET]}"))
    .setProperty("kpi_event_topic_partition", simple("${headers[kafka.PARTITION]}"))
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

  private void processBulkChargeAssessmentEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("kafka:{{kafka.topic.bulk-chargeassessments.name}}?groupId=ccm-notification-service&consumersCount={{kafka.topic.bulk.chargeassessment.consumer.count}}&maxPollRecords=5&maxPollIntervalMs=2400000")
    .routeId(routeId)
    .log(LoggingLevel.INFO,"Event from Kafka {{kafka.topic.bulk-chargeassessments.name}} topic (offset=${headers[kafka.OFFSET]}): ${body}\n" +
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
    .setHeader("event_message_type")
      .jsonpath("$.justin_message_event_type_cd")
    .setHeader("event")
      .simple("${body}")
    .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentEvent.class)
    .setProperty("kpi_event_object", body())
    .setProperty("kpi_event_topic_name", simple("${headers[kafka.TOPIC]}"))
    .setProperty("kpi_event_topic_offset", simple("${headers[kafka.OFFSET]}"))
    .setProperty("kpi_event_topic_partition", simple("${headers[kafka.PARTITION]}"))
    .marshal().json(JsonLibrary.Jackson, ChargeAssessmentEvent.class)
    .choice()
      .when(header("event_status").isEqualTo(ChargeAssessmentEvent.STATUS.INFERRED_AUTH_LIST_CHANGED))
        .setProperty("kpi_component_route_name", simple("processCourtCaseAuthListChanged"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processCourtCaseAuthListChanged")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .when(header("event_status").isEqualTo(ChargeAssessmentEvent.STATUS.INFERRED_PART_ID_PROVISIONED))
        .setProperty("kpi_component_route_name", simple("createPartIdProvisionCompleted"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:createPartIdProvisionCompleted")
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

  private void createPartIdProvisionCompleted() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("kpi_event_object_orig", simple("${exchangeProperty.kpi_event_object}"))
    .setProperty("kpi_event_topic_offset_orig", simple("${exchangeProperty.kpi_event_topic_offset}"))
    .setProperty("kpi_event_topic_partition_orig", simple("${exchangeProperty.kpi_event_topic_partition}"))
    .setProperty("kpi_event_topic_name_orig", simple("${exchangeProperty.kpi_event_topic_name}"))
    .setProperty("kpi_status_orig", simple("${exchangeProperty.kpi_status}"))
    .setProperty("kpi_component_route_name_orig", simple("${exchangeProperty.kpi_component_route_name}"))

    .setHeader("event_key")
    .jsonpath("$.justin_part_id")
    // generate the batch-ended event
    .log(LoggingLevel.INFO,"Creating case user 'provision completed' event")
    .process(exchange -> {
        CaseUserEvent event = new CaseUserEvent();
        event.setEvent_status(CaseUserEvent.STATUS.PROVISION_COMPLETED.name());
        event.setEvent_source(CaseUserEvent.SOURCE.JADE_CCM.name());
        String eventKey = (String)exchange.getMessage().getHeader("event_key");
        event.setEvent_key(eventKey);
        event.setJustin_part_id(eventKey);

        exchange.getMessage().setBody(event, CaseUserEvent.class);
        exchange.getMessage().setHeader("kafka.KEY", event.getEvent_key());
      })
    .setProperty("kpi_event_object", body())
    .marshal().json(JsonLibrary.Jackson, CaseUserEvent.class)
    .setProperty("business_event", body())
    .log(LoggingLevel.DEBUG,"Generate converted business event: ${body}")
    .to("kafka:{{kafka.topic.bulk-caseusers.name}}")

    // generate event-created KPI
    .setProperty("kpi_event_topic_name", simple("{{kafka.topic.bulk-caseusers.name}}"))
    .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
    .setProperty("kpi_component_route_name", simple(routeId))
    .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
    .to("direct:preprocessAndPublishEventCreatedKPI")

    // KPI: restore previous values
    .setProperty("kpi_event_object", simple("${exchangeProperty.kpi_event_object_orig}"))
    .setProperty("kpi_event_topic_offset", simple("${exchangeProperty.kpi_event_topic_offset_orig}"))
    .setProperty("kpi_event_topic_partition", simple("${exchangeProperty.kpi_event_topic_partition_orig}"))
    .setProperty("kpi_event_topic_name", simple("${exchangeProperty.kpi_event_topic_name_orig}"))
    .setProperty("kpi_status", simple("${exchangeProperty.kpi_status_orig}"))
    .setProperty("kpi_component_route_name", simple("${exchangeProperty.kpi_component_route_name_orig}"))
    ;
  }

  private void processParticipantMergeEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("kafka:{{kafka.topic.participant.name}}?groupId=ccm-notification-service")
    .routeId(routeId)
    .log(LoggingLevel.INFO,"Event from Kafka {{kafka.topic.participant.name}} topic (offset=${headers[kafka.OFFSET]}): ${body}\n" +
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
    .unmarshal().json(JsonLibrary.Jackson, ParticipantMergeEvent.class)
    .setProperty("kpi_event_object", body())
    .setProperty("kpi_event_topic_name", simple("${headers[kafka.TOPIC]}"))
    .setProperty("kpi_event_topic_offset", simple("${headers[kafka.OFFSET]}"))
    .setProperty("kpi_event_topic_partition", simple("${headers[kafka.PARTITION]}"))
    .marshal().json(JsonLibrary.Jackson, ParticipantMergeEvent.class)
    .choice()
      .when(header("event_status").isEqualTo(ParticipantMergeEvent.STATUS.PART_MERGE))
        .setProperty("kpi_component_route_name", simple("processParticipantMerge"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processParticipantMerge")
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
    // double check that case had not been already created since.
    .setHeader("number", simple("${header[event_key]}"))
    .to("http://ccm-lookup-service/getCourtCaseExists")
    .unmarshal().json()
    .setProperty("caseFound").simple("${body[id]}")
    .choice()
      .when(simple("${exchangeProperty.caseFound} == ''"))
        .log(LoggingLevel.DEBUG,"Retrieve latest court case details from JUSTIN.")
        .setHeader(Exchange.HTTP_METHOD, simple("GET"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .setHeader("number").simple("${header.event_key}")
        .removeHeader(Exchange.CONTENT_ENCODING)
        .to("http://ccm-lookup-service/getCourtCaseDetails")
        .setProperty("courtcase_data", simple("${bodyAs(String)}"))
        .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentData.class)
        .setProperty("courtcase_object", body())

        .setProperty("allowCreateCase").simple("true")
        .setProperty("autoCreateMaxDays").simple("{{dems.case.auto.creation.submit.date.cutoff}}")
        // BCPSDEMS-1543 - Check to make sure rcc submit date is not before dems.case.auto.creation.submit.date.cutoff
        .process(new Processor() {
          @Override
          public void process(Exchange ex) throws HttpOperationFailedException {
            ChargeAssessmentData chargeAssessmentdata = (ChargeAssessmentData)ex.getProperty("courtcase_object", ChargeAssessmentData.class);
            log.info("rcc submit date: "+chargeAssessmentdata.getRcc_submit_date());
            ex.setProperty("accusedList", chargeAssessmentdata.getAccused_persons());
            ex.setProperty("courtNumber", chargeAssessmentdata.getRcc_id());

            log.info("accused_persons: "+chargeAssessmentdata.getAccused_persons().size());
            // based on the autoCreateMaxDays value, and the rcc's submit date and
            // whether or not it is a manu_file or manu_cfile
            String event_message_type = (String)ex.getMessage().getHeader("event_message_type");
            if(event_message_type != null
              && !event_message_type.equalsIgnoreCase("MANU_CFILE")
              && !event_message_type.equalsIgnoreCase("MANU_FILE")) {
              // Make sure that the message type isn't a manual creation first.
              try {
                Integer autoCreateMaxDays = (Integer)ex.getProperty("autoCreateMaxDays", Integer.class);
                if(autoCreateMaxDays != null && autoCreateMaxDays >= 1) {
                  // If no submit date, then don't create!
                  ZonedDateTime submitDateTime = DateTimeUtils.convertToZonedDateTimeFromBCDateTimeString(chargeAssessmentdata.getRcc_submit_date());
                  ZonedDateTime currentDateTime = DateTimeUtils.convertToZonedDateTimeFromBCDateTimeString(DateTimeUtils.generateCurrentDtm());
                  ZonedDateTime maxSubmitDateTime = currentDateTime.minusDays(autoCreateMaxDays);
                  // jade 2770 fix
                  if(chargeAssessmentdata.getAccused_persons().size() == 0) {
                    ex.setProperty("allowCreateCase", "false");
                    log.info("No accused associated with the rcc.");
                  }
                  if(submitDateTime == null || submitDateTime.isBefore(maxSubmitDateTime)) {
                    ex.setProperty("allowCreateCase", "false");
                    log.info("Submit date is beyond "+autoCreateMaxDays+" days ago.");
                  }
                }
              } catch(Exception error) {
                error.printStackTrace();
              }
            }
          }
        })
      .endChoice()
    .otherwise()
      .log(LoggingLevel.WARN, "RCC: ${header.event_key} already exists in DEMS.")
      .setProperty("allowCreateCase").simple("false")
    .end()
    
    .choice()
      .when(simple("${exchangeProperty.allowCreateCase} == 'true'"))

        .doTry()
          .setBody(simple("${exchangeProperty.courtcase_data}"))
          .log(LoggingLevel.DEBUG,"Create court case in DEMS.  Court case data = ${body}.")
          .setHeader(Exchange.HTTP_METHOD, simple("POST"))
          .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        
          .to("http://ccm-dems-adapter/createCourtCase")

        .endDoTry()
        .doCatch(HttpOperationFailedException.class)
          .log(LoggingLevel.ERROR,"Exception in createCourtCase call")
          .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${exception.statusCode}"))
          .setHeader("CCMException", simple("${exception.statusCode}"))

          .process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
              try {
                HttpOperationFailedException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);
                exchange.getMessage().setBody(cause.getResponseBody());

                log.error("HttpOperationFailedException returned body : " + exchange.getMessage().getBody(String.class));

                exchange.setProperty("exception", cause);

                if(exchange != null && exchange.getMessage() != null && exchange.getMessage().getBody() != null) {
                  String body = Base64.getEncoder().encodeToString(exchange.getMessage().getBody(String.class).getBytes());
                  exchange.getIn().setHeader("CCMExceptionEncoded", body);
                }
              } catch(Exception ex) {
                ex.printStackTrace();
              }
            }
          })

          .log(LoggingLevel.WARN, "Failed Case Creation: ${exchangeProperty.exception}")
          .log(LoggingLevel.ERROR,"CCMException: ${header.CCMException}")
        .end()


        .log(LoggingLevel.WARN, "Created DEMS Case: ${header.event_key}")

        .setHeader("number",simple("${exchangeProperty.courtNumber}"))
        .log(LoggingLevel.DEBUG, "Number value: ${exchangeProperty.courtNumber}")

        // set the updated accusedList object to be the body to use it to retrieve all the accused
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) {
            ArrayList<String> accusedList = (ArrayList<String>)exchange.getProperty("accusedList", ArrayList.class);

            exchange.getMessage().setBody(accusedList, ArrayList.class);
          }
        })
        .marshal().json(JsonLibrary.Jackson, ArrayList.class)
        .log(LoggingLevel.DEBUG, "calling processAccused ${body}")
        .to("direct:processAccusedPersons")

        .log(LoggingLevel.DEBUG,"Update court case auth list.")
        .to("direct:processCourtCaseAuthListChanged")

        // wireTap makes an call and immediate return without waiting for the process to complete
        // the direct call will wait for a certain time before creating the Report End event.
        .wireTap("direct:generateStaticReportEvent")

        .log(LoggingLevel.INFO, "Checking for exceptions")
        .choice()
          .when(simple("${exchangeProperty.exception} != null"))
            .log(LoggingLevel.INFO, "There is an exception")
    
            .process(new Processor() {
              public void process(Exchange exchange) throws Exception {
    
                Exception ex = (Exception)exchange.getProperty("exception");
                throw ex;
              }
            })
          .otherwise()
            .log(LoggingLevel.INFO, "No exception")
            .log(LoggingLevel.ERROR, "Exception: ${exchangeProperty.exception}")
        .end()

        .log(LoggingLevel.INFO, "Completed processChargeAssessmentCreated")

      .endChoice()
    .end();
  }

  private void generateStaticReportEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"event_key = ${header[event_key]}")
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
        re.setEvent_key(rcc_id);
        re.setEvent_status(ReportEvent.STATUS.REPORT.name());
        re.setEvent_source(ReportEvent.SOURCE.JADE_CCM.name());
        re.setJustin_event_message_id(Integer.parseInt(event_message_id));
        re.setJustin_message_event_type_cd(ReportEvent.STATUS.REPORT.name());
        re.setReport_type(reportTypesSb.toString());
        exchange.getMessage().setBody(re, ReportEvent.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, ReportEvent.class)
    .delay(30000)
    .to("kafka:{{kafka.topic.reports.name}}")
    ;
  }

  private void processCourtCaseEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("kafka:{{kafka.topic.courtcases.name}}?groupId=ccm-notification-service&consumersCount={{kafka.topic.courtcase.consumer.count}}&maxPollRecords=3&maxPollIntervalMs=4800000")
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
    .setHeader("event_message_type")
      .jsonpath("$.justin_message_event_type_cd")
    .setHeader("event")
      .simple("${body}")
    .unmarshal().json(JsonLibrary.Jackson, CourtCaseEvent.class)
    .setProperty("kpi_event_object", body())
    .setProperty("kpi_event_topic_name", simple("${headers[kafka.TOPIC]}"))
    .setProperty("kpi_event_topic_offset", simple("${headers[kafka.OFFSET]}"))
    .setProperty("kpi_event_topic_partition", simple("${headers[kafka.PARTITION]}"))
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
            ex.setProperty("kpi_event_topic_partition_orig", ex.getProperty("kpi_event_topic_partition"));
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
        .setProperty("kpi_event_topic_partition", simple("${exchangeProperty.kpi_event_topic_partition_orig}"))
        .setProperty("kpi_event_topic_name", simple("${exchangeProperty.kpi_event_topic_name_orig}"))
        .setProperty("kpi_status", simple("${exchangeProperty.kpi_status_orig}"))
        .setProperty("kpi_component_route_name", simple("${exchangeProperty.kpi_component_route_name_orig}"))
      .endChoice()
      .otherwise()
        .log(LoggingLevel.WARN, "Case does not exist in DEMS, skipped.")
      .endChoice()
    .end()
    ;
    //throw new HttpOperationFailedException("testingCCMNotificationService",404,"Exception raised","CCMNotificationService",null, routeId);
  }

  //as part of jade 1750
  private void processParticipantMerge() throws HttpOperationFailedException {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO," event_key = ${header[event_key]}")
    //.setHeader("number", simple("${header[event_key]}"))
    .process(new Processor() {
      @Override
      public void process(Exchange ex) throws HttpOperationFailedException {
        // KPI: Preserve original event properties
        ex.setProperty("kpi_event_object_orig", ex.getProperty("kpi_event_object"));

        ParticipantMergeEvent original_event = (ParticipantMergeEvent)ex.getProperty("kpi_event_object");

        //log.info("fromPartId : "+ original_event.getJustin_from_part_id());
        //log.info("toPartId : "+ original_event.getJustin_to_part_id());
        String fromPartId = original_event.getJustin_from_part_id();
        String toPartId = original_event.getJustin_to_part_id();

        ex.setProperty("fromPartId", fromPartId);
        ex.setProperty("toPartId", toPartId);
      }
    })
    .marshal().json(JsonLibrary.Jackson, ParticipantMergeEvent.class)
    .log(LoggingLevel.INFO,"Generating derived event: ${body}")
    .log(LoggingLevel.DEBUG,"fromPartId : ${exchangeProperty.fromPartId} & toPartID : ${exchangeProperty.toPartId}")
    //search if from part id exist in the dems system
    .choice()
      .when(simple(" ${exchangeProperty.fromPartId} != ''"))
      .removeHeader("CamelHttpUri")
      .removeHeader("CamelHttpBaseUri")
      .removeHeaders("CamelHttp*")
      .setHeader(Exchange.HTTP_METHOD, simple("GET"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .setHeader("key", simple("${exchangeProperty.fromPartId}"))
      .to("http://ccm-dems-adapter/checkPersonExist")
      .log(LoggingLevel.DEBUG,"Lookup response = '${body}'")
      .unmarshal().json()
      .setProperty("frompartId").simple("${body[id]}")
      .endChoice()
    .end()
    //search if to part id exist in the dems system
    .choice()
      .when(simple(" ${exchangeProperty.toPartId} != ''"))
      .removeHeader("CamelHttpUri")
      .removeHeader("CamelHttpBaseUri")
      .removeHeaders("CamelHttp*")
      .setHeader(Exchange.HTTP_METHOD, simple("GET"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .setHeader("key", simple("${exchangeProperty.toPartId}"))
      .to("http://ccm-dems-adapter/checkPersonExist")
      .log(LoggingLevel.DEBUG,"Lookup response = '${body}'")
      .unmarshal().json()
      .setProperty("topartId").simple("${body[id]}")
      .endChoice()
    .end()
    //.log(LoggingLevel.INFO,"from part id :${exchangeProperty.frompartId} & to part id:${exchangeProperty.topartId}")
    //if both exist calling the merge api
    .choice()
        .when(simple("${exchangeProperty.frompartId} != '' && ${exchangeProperty.topartId} != ''"))
          .setHeader("fromPartid", simple("${exchangeProperty.frompartId}"))
          .setHeader("toPartid", simple("${exchangeProperty.topartId}"))
          .to("http://ccm-dems-adapter/reassignParticipantCases")
          .log(LoggingLevel.INFO,"Received response: '${body}'")
        .endChoice()
        .otherwise()
          .log(LoggingLevel.INFO,"Participant not found")
        .endChoice()
      .end()
      .log(LoggingLevel.INFO, "end of processParticipantMerge.")
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
    .log(LoggingLevel.INFO,"event_key = ${header[event_key]}")

    .choice() // force a retrigger of static reports, if this is a manual event.
      .when(simple("${header[event_message_type]} == 'MANU_FILE'"))
        .setProperty("triggerStaticReports", simple("true"))
      .endChoice()
      .otherwise()
        .setProperty("triggerStaticReports", simple("false"))
    .end()

    .setHeader("key").simple("${header.event_key}")
    .setHeader("event_key",simple("${header.event_key}"))
    .setHeader("number",simple("${header.event_key}"))
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-lookup-service/getCourtCaseStatusExists")
    .unmarshal().json()

    //JADE-2671 - look-up primary rcc for update.
    .choice() // If this is an inactive case, look for the primary, if it exists.  That one should have all agency files listed.
      .when(simple("${body[status]} == 'Inactive' && ${body[primaryAgencyFileId]} != ${header.event_key}"))
        .setHeader("key").simple("${body[primaryAgencyFileId]}")
        .setHeader("event_key",simple("${body[primaryAgencyFileId]}"))
        .setHeader("number",simple("${body[primaryAgencyFileId]}"))
        .setHeader(Exchange.HTTP_METHOD, simple("GET"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .to("http://ccm-lookup-service/getCourtCaseStatusExists")
        .log(LoggingLevel.DEBUG, "Dems case status: ${body}")
        .unmarshal().json()
      .endChoice()
    .end()

    .setProperty("caseId", simple("${body[id]}"))
    .choice()
      .when(simple("${body[status]} == 'Active'"))
        .setHeader("number").simple("${header.event_key}")
        .setHeader(Exchange.HTTP_METHOD, simple("GET"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .removeHeader(Exchange.CONTENT_ENCODING)
        .to("http://ccm-lookup-service/getCourtCaseDetails")
        .log(LoggingLevel.DEBUG,"Update court case in DEMS.  Court case data = ${body}.")
        .setProperty("courtcase_data", simple("${bodyAs(String)}"))
        //jade 2770 fix
        .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentData.class)
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) {
            ChargeAssessmentData courtfiledata = exchange.getIn().getBody(ChargeAssessmentData.class);
            exchange.setProperty("accused_person", courtfiledata.getAccused_persons().size());
            exchange.setProperty("accusedList", courtfiledata.getAccused_persons());
            exchange.setProperty("courtNumber", courtfiledata.getRcc_id());
            exchange.setProperty("courtcase_data", courtfiledata);
          }}
        ).marshal().json()
        .log(LoggingLevel.DEBUG,"Accused_person : ${exchangeProperty.accused_person}" )
        .to("direct:updateChargeAssessment")
        .log(LoggingLevel.DEBUG,"Returned body: ${body}")
      .endChoice()
      .when(simple("${body[status]} == 'Inactive' && ${body[primaryAgencyFileId]} == ${body[key]}"))
        // BCPSDEMS-1519, JADE-2712 If the DEMS case is inactive and not disabled due to a merge, then
        // check if this is a scenario of an rcc being re-submitted.
        .choice()
          .when(simple("${body[rccStatus]} == 'Return'"))
            .setHeader("number").simple("${header.event_key}")
            .setHeader(Exchange.HTTP_METHOD, simple("GET"))
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .removeHeader(Exchange.CONTENT_ENCODING)
            .to("http://ccm-lookup-service/getCourtCaseDetails")
            .log(LoggingLevel.DEBUG,"Retrieved Court Case from JUSTIN: ${body}")
            .setProperty("courtcase_data", simple("${bodyAs(String)}"))
            .log(LoggingLevel.DEBUG,"courtcase_data : ${bodyAs(String)}")
            .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentData.class)
            .process(new Processor() {
              @Override
              public void process(Exchange exchange) {
                ChargeAssessmentData b = exchange.getIn().getBody(ChargeAssessmentData.class);
                exchange.setProperty("justinCourtCaseStatus", b.getRcc_status_code());
                exchange.setProperty("bodytest", b);
                exchange.setProperty("accused_person", b.getAccused_persons().size());
                //System.out.println("justinCourtCaseStatus:" +  b.getRcc_status_code());
                //exchange.getMessage().setBody(b, ChargeAssessmentData.class);
                //System.out.println("body : "+ b.toString());
              }
            }) .marshal().json()
            .log(LoggingLevel.INFO, "justinCourtCaseStatus: ${exchangeProperty.justinCourtCaseStatus}")
            .choice()
              .when(simple("${exchangeProperty.justinCourtCaseStatus} != 'Return' && ${exchangeProperty.accused_person} != '0'"))
                .log(LoggingLevel.INFO,"Ready for reactivating the case")
                .log(LoggingLevel.DEBUG,"courtcase_data : ${bodyAs(String)}")
                .setProperty("courtcase_data", simple("${bodyAs(String)}"))
                .setBody(simple("${exchangeProperty.courtcase_data}"))
                .setHeader(Exchange.HTTP_METHOD, simple("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to("http://ccm-dems-adapter/updateCourtCase")
                .log(LoggingLevel.INFO,"Update court case auth list.")
                .to("direct:processCourtCaseAuthListChanged")
                .setProperty("triggerStaticReports", simple("true"))
            .endChoice()
            //jade 2770 fix
            .when(simple("${exchangeProperty.accused_person} == '0'"))
              .log(LoggingLevel.WARN, "There is no accused person")
            .endChoice()
        .endChoice()
      .endChoice()
      .otherwise()
        .log(LoggingLevel.INFO, "DEMS Case is not in Active or RET state, so skip.")
    .end()

    .log(LoggingLevel.INFO, "check if triggering static reports")

    .choice()
      .when(simple("${exchangeProperty.triggerStaticReports} == 'true'"))
        // wireTap makes an call and immediate return without waiting for the process to complete
        // the direct call will wait for a certain time before creating the Batch End event.
        .wireTap("direct:generateStaticReportEvent")
      .endChoice()
      .otherwise()
        .log(LoggingLevel.INFO, "Do not trigger static reports")

    .log(LoggingLevel.INFO, "Checking for exceptions")
    .choice()
      .when(simple("${exchangeProperty.exception} != null"))
        .log(LoggingLevel.INFO, "There is an exception")

        .process(new Processor() {
          public void process(Exchange exchange) throws Exception {

            Exception ex = (Exception)exchange.getProperty("exception");
            throw ex;
          }
        })
      .otherwise()
        .log(LoggingLevel.INFO, "No exceptions")
    .end()
    .log(LoggingLevel.INFO, "Completed processChargeAssessmentUpdated")
    ;
  }

  private void updateChargeAssessment() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .choice()
      .when(simple("${exchangeProperty.accused_person} != '0'"))
        .log(LoggingLevel.INFO, "There is at least 1 accused person")

        .setHeader("number").simple("${header.event_key}")
        .setHeader(Exchange.HTTP_METHOD, simple("GET"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .removeHeader(Exchange.CONTENT_ENCODING)
        .to("http://ccm-lookup-service/getCourtCaseDetails")
        .log(LoggingLevel.DEBUG,"Update court case in DEMS.  Court case data = ${body}.")
        .setProperty("courtcase_data", simple("${bodyAs(String)}"))
        //jade 2770 fix
        .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentData.class)
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) {
            ChargeAssessmentData courtfiledata = exchange.getIn().getBody(ChargeAssessmentData.class);
            exchange.setProperty("accused_person", courtfiledata.getAccused_persons().size());
            exchange.setProperty("accusedList", courtfiledata.getAccused_persons());
            exchange.setProperty("courtNumber", courtfiledata.getRcc_id());
            exchange.setProperty("courtcase_data", courtfiledata);
          }}
        ).marshal().json()
        .log(LoggingLevel.DEBUG,"Accused_person : ${exchangeProperty.accused_person}" )
        .log(LoggingLevel.DEBUG,"Body: ${exchangeProperty.courtcase_data}")
        .choice()
          .when(simple("${exchangeProperty.accused_person} != '0'"))
            .log(LoggingLevel.INFO, "There is at least 1 accused person")
    
            .doTry()
              // add-on any additional rccs from the dems side.
              //.setProperty("courtcase_data", simple("${bodyAs(String)}"))
              .to("direct:compileRelatedChargeAssessments")
              .log(LoggingLevel.DEBUG,"Compiled court case in DEMS.  Court case data = ${body}.")
        
              .setProperty("courtcase_data", simple("${bodyAs(String)}"))
              .setBody(simple("${exchangeProperty.courtcase_data}"))
              .setHeader(Exchange.HTTP_METHOD, simple("POST"))
              .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            
              .to("http://ccm-dems-adapter/updateCourtCase")
              .log(LoggingLevel.INFO,"Update court case auth list.")
            .doCatch(HttpOperationFailedException.class)
              .log(LoggingLevel.ERROR,"Exception in updateCourtCase call")
              .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${exception.statusCode}"))
              .setHeader("CCMException", simple("${exception.statusCode}"))
        
              .process(new Processor() {
                @Override
                public void process(Exchange exchange) throws Exception {
                  try {
                    HttpOperationFailedException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);
                    exchange.getMessage().setBody(cause.getResponseBody());
        
                    log.error("HttpOperationFailedException returned body : " + exchange.getMessage().getBody(String.class));
        
                    exchange.setProperty("exception", cause);
        
                    if(exchange != null && exchange.getMessage() != null && exchange.getMessage().getBody() != null) {
                      String body = Base64.getEncoder().encodeToString(exchange.getMessage().getBody(String.class).getBytes());
                      exchange.getIn().setHeader("CCMExceptionEncoded", body);
                    }
                  } catch(Exception ex) {
                    ex.printStackTrace();
                  }
                }
              })
        
              .log(LoggingLevel.WARN, "Failed Case Update: ${exchangeProperty.exception}")
              .log(LoggingLevel.ERROR,"CCMException: ${header.CCMException}")
            .end()
            .log(LoggingLevel.INFO, "End of do try catch call")
        
            // set the updated accusedList object to be the body to use it to retrieve all the accused
            .process(new Processor() {
              @Override
              public void process(Exchange exchange) {
                ArrayList<String> accusedList = (ArrayList<String>)exchange.getProperty("accusedList", ArrayList.class);
        
                exchange.getMessage().setBody(accusedList, ArrayList.class);
              }
            })
            .marshal().json(JsonLibrary.Jackson, ArrayList.class)
            .setHeader("number",simple("${exchangeProperty.courtNumber}"))
            .log(LoggingLevel.DEBUG, "calling processAccused persons ${body}")
            .to("direct:processAccusedPersons")
        
            .to("direct:processCourtCaseAuthListChanged")
            .process(new Processor() {
              @Override
              public void process(Exchange exchange) {
                ChargeAssessmentData courtfiledata = (ChargeAssessmentData)exchange.getProperty("courtcase_object", ChargeAssessmentData.class);
                exchange.setProperty("justinCourtCaseStatus", courtfiledata.getRcc_status_code());
              }}
            )
            .log(LoggingLevel.INFO, "This is checking for return.")
            //BCPSDEMS-1518, JADE-1751
            .choice()
              .when(simple("${exchangeProperty.justinCourtCaseStatus} == 'Return'"))
                .setHeader("case_id").simple("${exchangeProperty.caseId}")
                .to("http://ccm-dems-adapter/inactivateCase")
                .log(LoggingLevel.INFO,"Inactivated Returned or No Charge case")
            .end()
            .log(LoggingLevel.INFO, "Court case updated")
          .endChoice()
          .when(simple("${exchangeProperty.accused_person} == '0'"))
            .log(LoggingLevel.WARN,"There is no accused person")
          .endChoice()
      .endChoice()
      .when(simple("${exchangeProperty.accused_person} == '0'"))
        .log(LoggingLevel.WARN,"There is no accused person")
      .endChoice()
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
    .log(LoggingLevel.INFO,"event_key = ${header[event_key]}")

    .setHeader("number", simple("${header[event_key]}"))
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-lookup-service/getCourtCaseStatusExists")
    .unmarshal().json()

    .choice() // If this is an inactive cast, look for the primary, if it exists.  That one should have all court files listed.
      .when(simple("${body[status]} == 'Inactive' && ${body[primaryAgencyFileId]} != ''"))
        .setHeader("key").simple("${body[primaryAgencyFileId]}")
        .setHeader("event_key",simple("${body[primaryAgencyFileId]}"))
        .setHeader("number",simple("${body[primaryAgencyFileId]}"))
        .setHeader(Exchange.HTTP_METHOD, simple("GET"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .to("http://ccm-lookup-service/getCourtCaseStatusExists")
        .log(LoggingLevel.DEBUG, "Dems case status: ${body}")
        .unmarshal().json()
      .endChoice()
    .end()
    .setProperty("dems_agency_files").simple("${body[agencyFileId]}")

    // Get list from the primary case.
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("number").simple("${header.event_key}")
    .to("http://ccm-lookup-service/getCourtCaseAuthList")
    .log(LoggingLevel.DEBUG, "Initial auth list for rcc: ${header.number}: ${body}")

    .unmarshal().json(JsonLibrary.Jackson, AuthUserList.class)
    .setProperty("authlist_object", body())

    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        String demsAgencyFiles = (String)exchange.getProperty("dems_agency_files", String.class);
        String primaryRccId = (String)exchange.getMessage().getHeader("number");
        //log.info("agencyFileIds: "+demsAgencyFiles);
        String[] demsAgencyFileList = demsAgencyFiles.split(";");
        ArrayList<String> agencyFileList = new ArrayList<String>();
        if(demsAgencyFileList != null && demsAgencyFileList.length > 0) {
          //log.info("Primary rcc: "+primaryRccId);
          for(String demsAgencyFileId : demsAgencyFileList) {
            demsAgencyFileId = demsAgencyFileId.trim();
            //log.info("Comparing rcc: "+demsAgencyFileId);
            if(demsAgencyFileId.equalsIgnoreCase(primaryRccId)) {
              continue;
            } else {
              agencyFileList.add(demsAgencyFileId);
            }
          }
        }
        exchange.getMessage().setBody(agencyFileList);
      }
    })
    .log(LoggingLevel.DEBUG, "Unprocessed agency file list: ${body}")
    .split().jsonpathWriteAsString("$.*")
      .setProperty("agencyFileId", simple("${body}"))
      //.log(LoggingLevel.INFO, "agency file: ${exchangeProperty.agencyFileId}")
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) {
          String agencyFileId = exchange.getProperty("agencyFileId", String.class);
          //log.info("agencyFileId:"+agencyFileId);
          exchange.setProperty("agencyFileId", agencyFileId.replaceAll("\"", ""));
        }
      })

      .choice()
        .when(simple("${exchangeProperty.agencyFileId} != ''"))
          .log(LoggingLevel.DEBUG, "agency file updated: ${exchangeProperty.agencyFileId}")
          .setHeader("number").simple("${exchangeProperty.agencyFileId}")
          .setHeader(Exchange.HTTP_METHOD, simple("GET"))
          .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
          .removeHeader(Exchange.CONTENT_ENCODING)
          .log(LoggingLevel.INFO,"Retrieve court case auth list of ${exchangeProperty.agencyFileId}")
          .to("http://ccm-lookup-service/getCourtCaseAuthList")
          .log(LoggingLevel.DEBUG, "auth list for rcc: ${header.number}: ${body}")

          .unmarshal().json(JsonLibrary.Jackson, AuthUserList.class)
          .process(new Processor() {
            @Override
            public void process(Exchange exchange) {
              AuthUserList aul = exchange.getIn().getBody(AuthUserList.class);
              AuthUserList paul = (AuthUserList)exchange.getProperty("authlist_object", AuthUserList.class);

              log.info("original authlist size: "+paul.getAuth_user_list().size());
              log.info("comparing authlist size: "+aul.getAuth_user_list().size());
              List<AuthUser> intersectingAuthList = new ArrayList<AuthUser>();
              List<AuthUser> primaryAuthList = paul.getAuth_user_list();
              if(primaryAuthList == null) {
                primaryAuthList = new ArrayList<AuthUser>();
              }
              // go through list of intersectingAuthList, and if it is not found in aul.getAuth_user_list() then remove it
              List<AuthUser> nonPrimaryAuthList = aul.getAuth_user_list();
              if(nonPrimaryAuthList != null) {
                for(AuthUser pAuthUser : primaryAuthList) {
                  if(nonPrimaryAuthList.stream().filter(o -> o.getKey().equals(pAuthUser.getKey())).findFirst().isPresent()) {
                    //log.info("found intersecting part id: "+pAuthUser.getKey());
                    intersectingAuthList.add(pAuthUser);
                  }
                }
              }
              paul.setAuth_user_list(intersectingAuthList);
              exchange.setProperty("authlist_object", paul);
            }
          })
        .endChoice()
      .end()

    .end()


    // set the updated metadata object to be the body
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        AuthUserList metadata = (AuthUserList)exchange.getProperty("authlist_object", AuthUserList.class);
        log.info("final authlist size: "+metadata.getAuth_user_list().size());

        exchange.getMessage().setBody(metadata, AuthUserList.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, AuthUserList.class)

    .setProperty("authlist_data", simple("${bodyAs(String)}"))
    .setBody(simple("${exchangeProperty.authlist_data}"))

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
    from("kafka:{{kafka.topic.bulk-caseusers.name}}?groupId=ccm-notification-service&maxPollRecords=1&maxPollIntervalMs=4800000")
    .routeId(routeId)
    .log(LoggingLevel.INFO,"Event from Kafka {{kafka.topic.bulk-caseusers.name}} topic (offset=${headers[kafka.OFFSET]}): ${body}\n" +
      "    on the topic ${headers[kafka.TOPIC]}\n" +
      "    on the partition ${headers[kafka.PARTITION]}\n" +
      "    with the offset ${headers[kafka.OFFSET]}\n" +
      "    with the key ${headers[kafka.KEY]}")
    //.log(LoggingLevel.INFO, "headers: ${headers}")
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
    .setProperty("kpi_event_topic_partition", simple("${headers[kafka.PARTITION]}"))
    .marshal().json(JsonLibrary.Jackson, CaseUserEvent.class)
    .choice()
      .when(header("event_status").isEqualTo(CaseUserEvent.STATUS.ACCESS_REMOVED_NO_DETAILS))
        .setProperty("kpi_component_route_name", simple("processCaseUserAccessRemovedNoDetails"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))
        .to("direct:processCaseUserAccessRemovedNoDetails")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
        .to("direct:publishEventKPI")
        .endChoice()
      .end();
    ;
  }

  private void processCaseUserAccessRemovedNoDetails() {
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
        .setProperty("caseEvent", simple("${exchangeProperty.kpi_event_object}"))
        .setProperty("caseLength", jsonpath("$.case_list.length()"))
        .setProperty("caseList", body())

        // generate the batch-started event
        .log(LoggingLevel.DEBUG,"Creating case user 'batch-started' event")
        .process(exchange -> {
            CaseUserEvent event = new CaseUserEvent();
            event.setEvent_status(CaseUserEvent.STATUS.EVENT_BATCH_STARTED.name());
            event.setEvent_source(CaseUserEvent.SOURCE.JADE_CCM.name());

            exchange.getMessage().setBody(event, CaseUserEvent.class);
            exchange.getMessage().setHeader("kafka.KEY", event.getEvent_key());
          })
        .setProperty("kpi_event_object", body())
        .marshal().json(JsonLibrary.Jackson, CaseUserEvent.class)
        .setProperty("business_event", body())
        .log(LoggingLevel.DEBUG,"Generate converted business event: ${body}")
        .to("kafka:{{kafka.topic.bulk-caseusers.name}}")

        // generate event-created KPI
        .setProperty("kpi_event_topic_name", simple("{{kafka.topic.bulk-caseusers.name}}"))
        .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
        .setProperty("kpi_component_route_name", simple(routeId))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
        .to("direct:publishEventKPI")
        .setBody(simple("${exchangeProperty.caseList}"))

        // process each case
        .split()
          .jsonpathWriteAsString("$.case_list")
          //The route should not continue through the rest of the cases in the list after an exception has occurred.
          //Will now stop further processing if an exception or failure occurred during processing of an org.apache.camel.
          // The default behavior is to not stop but continue processing till the end.
          .stopOnException()

          .setHeader("rcc_id", jsonpath("$.rcc_id"))
          .log(LoggingLevel.INFO, "Updating for rcc:${header[rcc_id]} case ${exchangeProperty.CamelSplitIndex} of ${exchangeProperty.caseLength}")
          .choice()
            //only cases containing actual RCC_ID values (not null) should be processed.
            .when(simple("${header[rcc_id]} != null"))
              .setProperty("rcc_id",jsonpath("$.rcc_id"))
              .process(new Processor() {
                @Override
                public void process(Exchange exchange) throws Exception {
                  // Insert code that gets executed *before* delegating
                  // to the next processor in the chain.
                  CaseUserEvent originalEvent = (CaseUserEvent)exchange.getProperty("caseEvent", CaseUserEvent.class);
                  CaseUserEvent event = new CaseUserEvent(CaseUserEvent.SOURCE.JADE_CCM, originalEvent);
                  event.setEvent_status(CaseUserEvent.STATUS.ACCESS_REMOVED.toString());

                  String rccId = (String)exchange.getProperty("rcc_id", String.class);
                  event.setJustin_rcc_id(rccId);

                  exchange.getMessage().setBody(event, CaseUserEvent.class);
                  exchange.getMessage().setHeader("kafka.KEY", event.getEvent_key());
                }
              })
              .setProperty("kpi_event_object", body())
              .marshal().json(JsonLibrary.Jackson, CaseUserEvent.class)
              .log(LoggingLevel.INFO,"Creating an enriched " + CaseUserEvent.class.getSimpleName() + "." + CaseUserEvent.STATUS.ACCESS_REMOVED.name() + " event for ( part_id = ${header[event_key]} rcc_id = ${exchangeProperty.rcc_id} ) ...")
              .log(LoggingLevel.DEBUG,"${body}")
              .to("kafka:{{kafka.topic.bulk-caseusers.name}}")

              // generate event-created KPI
              .setProperty("kpi_event_topic_name", simple("{{kafka.topic.bulk-caseusers.name}}"))
              .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
              .setProperty("kpi_component_route_name", simple(routeId))
              .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
              .to("direct:publishEventKPI")

              .endChoice()
            .otherwise()
              //nothing to do here
              .endChoice()
          .end()
        .end()

        // wireTap makes an call and immediate return without waiting for the process to complete
        // the direct call will wait for a certain time before creating the Batch End event.
        .wireTap("direct:createBatchEndEventPaused")

        .setProperty("kpi_event_object", simple("${exchangeProperty.caseEvent}"))
        .endChoice()
      .when(simple("${header.CamelHttpResponseCode} == 404"))
          .log(LoggingLevel.INFO,"User (key = ${header.event_key}) not found; Do nothing.")
      .endChoice()
    .end()
    ;
  }

  private void http_createBatchEndEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/createBatchEndEvent")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .to("direct:createBatchEndEvent");
  }

  private void createBatchEndEventPaused() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .delay(35000)
    .to("direct:createBatchEndEvent");
  }

  private void createBatchEndEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html

    // generate the batch-ended event
    .log(LoggingLevel.INFO,"Creating case user 'batch-ended' event")
    .process(exchange -> {
        CaseUserEvent event = new CaseUserEvent();
        event.setEvent_status(CaseUserEvent.STATUS.EVENT_BATCH_ENDED.name());
        event.setEvent_source(CaseUserEvent.SOURCE.JADE_CCM.name());

        exchange.getMessage().setBody(event, CaseUserEvent.class);
        exchange.getMessage().setHeader("kafka.KEY", event.getEvent_key());
      })
    .setProperty("kpi_event_object", body())
    .marshal().json(JsonLibrary.Jackson, CaseUserEvent.class)
    .setProperty("business_event", body())
    .log(LoggingLevel.DEBUG,"Generate converted business event: ${body}")
    .to("kafka:{{kafka.topic.bulk-caseusers.name}}")

    // generate event-created KPI
    .setProperty("kpi_event_topic_name", simple("{{kafka.topic.bulk-caseusers.name}}"))
    .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
    .setProperty("kpi_component_route_name", simple(routeId))
    .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
    .to("direct:publishEventKPI")
    ;
  }

  private void compileRelatedChargeAssessments() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN
    // header: event_key (rcc_id)
    // property: courtcase_data
    // OUT
    // property: courtcase_data
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    // based off of rcc, look-up the DEMS case record.
    // If it exists, go through list of rccs in the
    .log(LoggingLevel.INFO,"event_key = ${header[event_key]}")
    //.setProperty("courtcase_data", simple("${bodyAs(String)}"))
    .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentData.class)
    .setProperty("courtcase_object", body())
    // go through each rcc in courtcase_data, starting with the primary rcc, then the related
    // ones, and if it is active in dems, then get list of rccs from dems

    .setHeader("number", simple("${header[event_key]}"))
    .log(LoggingLevel.INFO,"Retrieve court case status first")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-lookup-service/getCourtCaseStatusExists")
    .log(LoggingLevel.DEBUG, "Dems case status: ${body}")
    .unmarshal().json()

    .choice()
      .when(simple("${body[status]} != 'Inactive'"))
        .setProperty("dems_agency_files").simple("${body[agencyFileId]}")
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) {
            ChargeAssessmentData courtfiledata = (ChargeAssessmentData)exchange.getProperty("courtcase_object", ChargeAssessmentData.class);
            String demsAgencyFiles = (String)exchange.getProperty("dems_agency_files", String.class);
            //log.info("agencyFileIds: "+demsAgencyFiles);
            String[] demsAgencyFileList = demsAgencyFiles.split(";");
            ArrayList<String> agencyFileList = new ArrayList<String>();
            if(demsAgencyFileList != null && demsAgencyFileList.length > 0) {
              //log.info("Primary rcc: "+courtfiledata.getRcc_id());
              for(String demsAgencyFileId : demsAgencyFileList) {
                demsAgencyFileId = demsAgencyFileId.trim();
                //log.info("Comparing rcc: "+demsAgencyFileId);
                if(demsAgencyFileId.equalsIgnoreCase(courtfiledata.getRcc_id())) {
                  //log.info("Matching, so ignore");
                  continue;
                } else {
                  boolean matchFound = false;
                  for(ChargeAssessmentData ccd : courtfiledata.getRelated_charge_assessments()) {
                    String agencyFileId = ccd.getRcc_id();
                    if(demsAgencyFileId.equalsIgnoreCase(agencyFileId)) {
                      //log.info("Record found in related charge assessments.");
                      matchFound = true;
                      break;
                    }
                  }
                  if(!matchFound) {
                    agencyFileList.add(demsAgencyFileId);
                  }
                }
              }
            }
            exchange.getMessage().setBody(agencyFileList);
          }
        })
        .log(LoggingLevel.INFO, "Unprocessed agency file list: ${body}")
        .split().jsonpathWriteAsString("$.*")
          .setProperty("agencyFileId", simple("${body}"))
          .log(LoggingLevel.INFO, "agency file: ${exchangeProperty.agencyFileId}")
          .process(new Processor() {
            @Override
            public void process(Exchange exchange) {
              String agencyFileId = exchange.getProperty("agencyFileId", String.class);
              log.info("agencyFileId:"+agencyFileId);
              exchange.setProperty("agencyFileId", agencyFileId.replaceAll("\"", ""));
            }
          })

          .choice()
            .when(simple("${exchangeProperty.agencyFileId} != ''"))
              .log(LoggingLevel.INFO, "agency file updated: ${exchangeProperty.agencyFileId}")
              .setHeader("number").simple("${exchangeProperty.agencyFileId}")
              .setHeader(Exchange.HTTP_METHOD, simple("GET"))
              .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
              .removeHeader(Exchange.CONTENT_ENCODING)
              .to("http://ccm-lookup-service/getCourtCaseDetails")

              .log(LoggingLevel.DEBUG,"Retrieved related Court Case from JUSTIN: ${body}")
              .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentData.class)
              .process(new Processor() {
                @Override
                public void process(Exchange exchange) {
                  ChargeAssessmentData bcm = exchange.getIn().getBody(ChargeAssessmentData.class);
                  ChargeAssessmentData metadata = (ChargeAssessmentData)exchange.getProperty("courtcase_object", ChargeAssessmentData.class);
                  List<ChargeAssessmentData> relatedCf = metadata.getRelated_charge_assessments();
                  if(relatedCf == null) {
                    relatedCf = new ArrayList<ChargeAssessmentData>();
                  }
                  if(bcm.getRcc_id() != null && !bcm.getRcc_id().isEmpty()) {
                    // Only add if JUSTIN returned agency file info.
                    relatedCf.add(bcm);
                  }
                  log.info("Added new court file to metadata object.");
                  metadata.setRelated_charge_assessments(relatedCf);
                  exchange.setProperty("courtcase_object", metadata);
                }
              })
            .endChoice()
          .end()
        .end()
      .endChoice()
      .otherwise()
        // go through other rccs and check if they exist in dems and is active, if they do, need to do a merge.
        .log(LoggingLevel.INFO, "Not an active primary case")

      .endChoice()
    .end()

    // go through related rccs to make sure we don't miss any
    .setBody(simple("${exchangeProperty.courtcase_data}"))
    //.log(LoggingLevel.DEBUG, "Courtcase data: ${body}")
    .split()
      .jsonpathWriteAsString("$.related_charge_assessments")
      .setHeader("number", jsonpath("$.rcc_id"))
      //.log(LoggingLevel.INFO,"Retrieve dems case status of related rcc")
      .setHeader(Exchange.HTTP_METHOD, simple("GET"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .to("http://ccm-lookup-service/getCourtCaseStatusExists")
      .log(LoggingLevel.DEBUG, "Dems case status: ${body}")
      .unmarshal().json()

      .choice()
        .when(simple("${body[id]} != ''"))
          .setProperty("dems_agency_files").simple("${body[agencyFileId]}")
          .setProperty("related_primary_file").simple("${body[primaryAgencyFileId]}")
          .process(new Processor() {
            @Override
            public void process(Exchange exchange) {
              ChargeAssessmentData courtfiledata = (ChargeAssessmentData)exchange.getProperty("courtcase_object", ChargeAssessmentData.class);
              String demsAgencyFiles = (String)exchange.getProperty("dems_agency_files", String.class);
              String primaryAgencyFile = (String)exchange.getProperty("related_primary_file", String.class);
              log.info("agencyFileIds: "+demsAgencyFiles);
              String[] demsAgencyFileArray = demsAgencyFiles.split(";");
              ArrayList<String> agencyFileList = new ArrayList<String>();
              if(demsAgencyFileArray != null && demsAgencyFileArray.length > 0) {
                //log.info("Primary rcc: "+courtfiledata.getRcc_id());
                ArrayList<String> demsAgencyFileList = new ArrayList<String>(Arrays.asList(demsAgencyFileArray));
                demsAgencyFileList.add(primaryAgencyFile);
                for(String demsAgencyFileId : demsAgencyFileList) {
                  demsAgencyFileId = demsAgencyFileId.trim();
                  //log.info("Comparing rcc: "+demsAgencyFileId);
                  if(demsAgencyFileId.equalsIgnoreCase(courtfiledata.getRcc_id())) {
                    //log.info("Matching, so ignore");
                    continue;
                  } else {
                    boolean matchFound = false;
                    for(ChargeAssessmentData ccd : courtfiledata.getRelated_charge_assessments()) {
                      String agencyFileId = ccd.getRcc_id();
                      if(demsAgencyFileId.equalsIgnoreCase(agencyFileId)) {
                        //log.info("Record found in related charge assessments.");
                        matchFound = true;
                        break;
                      }
                    }
                    if(!matchFound) {
                      agencyFileList.add(demsAgencyFileId);
                    }
                  }
                }
              }
              exchange.getMessage().setBody(agencyFileList);
            }
          })
          .log(LoggingLevel.INFO, "Unprocessed agency file list: ${body}")
          .split().jsonpathWriteAsString("$.*")
            .setProperty("agencyFileId", simple("${body}"))
            //.log(LoggingLevel.INFO, "agency file: ${exchangeProperty.agencyFileId}")
            .process(new Processor() {
              @Override
              public void process(Exchange exchange) {
                String agencyFileId = exchange.getProperty("agencyFileId", String.class);
                //log.info("agencyFileId:"+agencyFileId);
                exchange.setProperty("agencyFileId", agencyFileId.replaceAll("\"", ""));
              }
            })

            .choice()
              .when(simple("${exchangeProperty.agencyFileId} != ''"))
                //.log(LoggingLevel.INFO, "agency file updated: ${exchangeProperty.agencyFileId}")
                .setHeader("number").simple("${exchangeProperty.agencyFileId}")
                .setHeader(Exchange.HTTP_METHOD, simple("GET"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .removeHeader(Exchange.CONTENT_ENCODING)
                .to("http://ccm-lookup-service/getCourtCaseDetails")

                .log(LoggingLevel.DEBUG,"Retrieved related Court Case from JUSTIN: ${body}")
                .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentData.class)
                .process(new Processor() {
                  @Override
                  public void process(Exchange exchange) {
                    ChargeAssessmentData bcm = exchange.getIn().getBody(ChargeAssessmentData.class);
                    ChargeAssessmentData metadata = (ChargeAssessmentData)exchange.getProperty("courtcase_object", ChargeAssessmentData.class);
                    List<ChargeAssessmentData> relatedCf = metadata.getRelated_charge_assessments();
                    if(relatedCf == null) {
                      relatedCf = new ArrayList<ChargeAssessmentData>();
                    }
                    if(bcm.getRcc_id() != null && !bcm.getRcc_id().isEmpty()) {
                      // Only add if JUSTIN returned agency file info.
                      relatedCf.add(bcm);
                    }
                    //log.info("Added new court file to metadata object.");
                    metadata.setRelated_charge_assessments(relatedCf);
                    exchange.setProperty("courtcase_object", metadata);
                  }
                })
              .endChoice()
            .end()
          .end()
        .endChoice()
        .otherwise()
          // go through other rccs and check if they exist in dems and is active, if they do, need to do a merge.
          .log(LoggingLevel.INFO, "Not an active primary case")

        .endChoice()
      .end()
    .end()

    // set the updated metadata object to be the body
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        ChargeAssessmentData metadata = (ChargeAssessmentData)exchange.getProperty("courtcase_object", ChargeAssessmentData.class);

        exchange.getMessage().setBody(metadata, ChargeAssessmentData.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, ChargeAssessmentData.class)

    .setProperty("courtcase_data", simple("${bodyAs(String)}"))
    .setBody(simple("${exchangeProperty.courtcase_data}"))
    .log(LoggingLevel.DEBUG, "Final list of merged agency files: ${body}")
    ;
  }

  private void compileRelatedCourtFiles() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN
    // header: event_key (mdoc_no)
    // OUT
    // property: metadata_data
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
          if(bcm.getCourt_file_id() != null && !bcm.getCourt_file_id().isEmpty()) {
            // Only add if JUSTIN returned court file info.
            relatedCf.add(bcm);
          }
          metadata.setRelated_court_cases(relatedCf);
          exchange.setProperty("metadata_object", metadata);
        }
      })
    .end()

    .setBody(simple("${exchangeProperty.metadata_data}"))
    .split()
      .jsonpathWriteAsString("$.related_agency_file")
      .setProperty("rcc_id", jsonpath("$.rcc_id"))
      .setProperty("primary_yn", jsonpath("$.primary_yn"))

      .setHeader("key").simple("${exchangeProperty.rcc_id}")
      .setHeader("event_key",simple("${exchangeProperty.rcc_id}"))
      .setHeader("number",simple("${exchangeProperty.rcc_id}"))
      //.log(LoggingLevel.INFO,"Retrieve court case status first")
      .setHeader(Exchange.HTTP_METHOD, simple("GET"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .to("http://ccm-lookup-service/getCourtCaseStatusExists")
      .log(LoggingLevel.DEBUG, "Dems case status: ${body}")
      .unmarshal().json()

      .choice() // If this is an inactive cast, look for the primary, if it exists.  That one should have all court files listed.
        .when(simple("${body[status]} == 'Inactive' && ${body[primaryAgencyFileId]} != ''"))
          .setHeader("key").simple("${body[primaryAgencyFileId]}")
          .setHeader("event_key",simple("${body[primaryAgencyFileId]}"))
          .setHeader("number",simple("${body[primaryAgencyFileId]}"))
          //.log(LoggingLevel.INFO,"Retrieve court case status first")
          .setHeader(Exchange.HTTP_METHOD, simple("GET"))
          .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
          .to("http://ccm-lookup-service/getCourtCaseStatusExists")
          .log(LoggingLevel.DEBUG, "Dems case status: ${body}")
          .unmarshal().json()
        .endChoice()
      .end()

      .choice() // TODO: EW need to verify with business if we should be limiting to primary only or go through all rccs.
        .when(simple("${body[status]} != 'Inactive'"))
          .setProperty("dems_court_files").simple("${body[courtFileId]}")
          .process(new Processor() {
            @Override
            public void process(Exchange exchange) {
              CourtCaseData metadata = (CourtCaseData)exchange.getProperty("metadata_object", CourtCaseData.class);
              String demsCourtFiles = (String)exchange.getProperty("dems_court_files", String.class);
              String[] demsCourtFileList = demsCourtFiles.split("; ");
              ArrayList<String> courtFileList = new ArrayList<String>();
              if(demsCourtFileList != null && demsCourtFileList.length > 0) {
                //log.info("metaMainCourtFile:"+metadata.getCourt_file_id());
                for(String demsCourtFileId : demsCourtFileList) {
                  //log.info("Comparing court file:"+demsCourtFileId);
                  if(demsCourtFileId.equalsIgnoreCase(metadata.getCourt_file_id())) {
                    continue;
                  } else {
                    //log.info("related length:"+metadata.getRelated_court_cases().size());
                    boolean matchFound = false;
                    for(CourtCaseData ccd : metadata.getRelated_court_cases()) {
                      String courtFileId = ccd.getCourt_file_id();
                      //log.info("related court file:"+courtFileId);
                      if(demsCourtFileId.equalsIgnoreCase(courtFileId)) {
                        matchFound = true;
                        break;
                      }
                    }
                    if(!matchFound) {
                      courtFileList.add(demsCourtFileId);
                    }
                  }
                }
              }
              exchange.getMessage().setBody(courtFileList);
            }
          })
          .log(LoggingLevel.INFO, "Unprocessed court file list: ${body}")
          .split().jsonpathWriteAsString("$.*")
            .setProperty("courtFileId", simple("${body}"))
            .log(LoggingLevel.DEBUG, "court file: ${exchangeProperty.courtFileId}")
            .process(new Processor() {
              @Override
              public void process(Exchange exchange) {
                String courtFileId = exchange.getProperty("courtFileId", String.class);
                //log.info("courtFileId:"+courtFileId);
                exchange.setProperty("courtFileId", courtFileId.replaceAll("\"", ""));
              }
            })

            .choice()
              .when(simple("${exchangeProperty.courtFileId} != ''"))
                .log(LoggingLevel.DEBUG, "court file updated: ${exchangeProperty.courtFileId}")
                .setHeader("number").simple("${exchangeProperty.courtFileId}")
                .setHeader(Exchange.HTTP_METHOD, simple("GET"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .removeHeader(Exchange.CONTENT_ENCODING)
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
                    if(bcm.getCourt_file_id() != null && !bcm.getCourt_file_id().isEmpty()) {
                      // Only add if JUSTIN returned court file info.
                      relatedCf.add(bcm);
                    }
                    //log.info("Added new court file to metadata object.");
                    metadata.setRelated_court_cases(relatedCf);
                    exchange.setProperty("metadata_object", metadata);
                  }
                })
              .endChoice()
            .end()
          .end()
        .endChoice()
        .otherwise()
          // go through other rccs and check if they exist in dems and is active, if they do, need to do a merge.
          .log(LoggingLevel.INFO, "Not an active primary case")
        .endChoice()
      .end()

    .end()

    // set the updated metadata object to be the body
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
    .log(LoggingLevel.DEBUG, "Final merged court files: ${body}")
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
    .to("direct:compileRelatedCourtFiles")

    .log(LoggingLevel.DEBUG, "CourtCaseData: ${body}")
    // get list of associated rcc_ids?

    // re-set body to the metadata_data json.
    .setBody(simple("${exchangeProperty.metadata_data}"))
    .unmarshal().json(JsonLibrary.Jackson, CourtCaseData.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        CourtCaseData ccd = exchange.getIn().getBody(CourtCaseData.class);
        exchange.getMessage().setBody(ccd.getPrimary_agency_file(), ChargeAssessmentDataRef.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, ChargeAssessmentDataRef.class)
    .setBody(simple("${bodyAs(String)}"))


    .log(LoggingLevel.DEBUG, "Court File Primary Rcc: ${body}")
    .setProperty("rcc_id", jsonpath("$.rcc_id"))
    .setProperty("primary_yn", jsonpath("$.primary_yn"))

    .setHeader("key").simple("${exchangeProperty.rcc_id}")
    .setHeader("event_key",simple("${exchangeProperty.rcc_id}"))
    .setHeader("number",simple("${exchangeProperty.rcc_id}"))
    //.log(LoggingLevel.INFO,"Retrieve court case status first")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-lookup-service/getCourtCaseStatusExists")
    .log(LoggingLevel.DEBUG, "Dems case status: ${body}")
    .unmarshal().json()

    .setProperty("primary_rcc_id", simple("${body[primaryAgencyFileId]}"))
    .log(LoggingLevel.INFO, "primary_rcc_id: ${exchangeProperty.primary_rcc_id}")

    //JADE-2671 - look-up primary rcc for update.
    .choice() // If this is an inactive case, look for the primary, if it exists.  That one should have all agency files listed.
      .when(simple("${body[status]} == 'Inactive' && ${exchangeProperty.primary_rcc_id} != ${header.event_key}"))
        .setHeader("key").simple("${exchangeProperty.primary_rcc_id}")
        .setHeader("event_key",simple("${exchangeProperty.primary_rcc_id}"))
        .setHeader("number",simple("${exchangeProperty.primary_rcc_id}"))
        .setProperty("rcc_id", simple("${exchangeProperty.primary_rcc_id}"))
        //.log(LoggingLevel.INFO,"Retrieve court case status first")
        .setHeader(Exchange.HTTP_METHOD, simple("GET"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .to("http://ccm-lookup-service/getCourtCaseStatusExists")
        .log(LoggingLevel.DEBUG, "Dems case status: ${body}")
        .unmarshal().json()
      .endChoice()
    .end()

    .choice()
      .when(simple("${body[status]} != 'Inactive'"))
        .setHeader("key", simple("${exchangeProperty.event_key_orig}"))
        .setHeader("event_key", simple("${exchangeProperty.event_key_orig}"))
        .to("direct:processPrimaryCourtCaseChanged")
      .endChoice()
      .otherwise()
        // go through other rccs and check if they exist in dems and is active, if they do, need to do a merge.
        .log(LoggingLevel.WARN, "Not an active primary case")

      .endChoice()
    .end()

    // re-set body to the metadata_data json.
    .setBody(simple("${exchangeProperty.metadata_data}"))
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
    .choice()
      .when(simple(" ${exchangeProperty.createCase} == 'true' || ${exchangeProperty.createOverrideFlag} == 'true'") )
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
              String derived_event_offset = KafkaComponentUtils.extractOffsetFromRecordMetadata(exchange.getProperty("derived_event_recordmetadata"));
              String derived_event_partition = KafkaComponentUtils.extractPartitionFromRecordMetadata(exchange.getProperty("derived_event_recordmetadata"));

              String derived_event_topic = (String)exchange.getProperty("derived_event_topic");

              EventKPI derived_event_kpi = new EventKPI(derived_event, EventKPI.STATUS.EVENT_CREATED);

              derived_event_kpi.setComponent_route_name(routeId);
              derived_event_kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
              derived_event_kpi.setEvent_topic_name(derived_event_topic);
              derived_event_kpi.setEvent_topic_offset(derived_event_offset);
              derived_event_kpi.setEvent_topic_partition(derived_event_partition);

              exchange.getMessage().setBody(derived_event_kpi);
            }
          })
          .marshal().json(JsonLibrary.Jackson, EventKPI.class)
          .log(LoggingLevel.DEBUG,"Publishing derived event KPI ...")
          .to("direct:publishBodyAsEventKPI")
          .log(LoggingLevel.DEBUG,"Derived event KPI published.")

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
              String derived_event_offset = KafkaComponentUtils.extractOffsetFromRecordMetadata(exchange.getProperty("derived_event_recordmetadata"));
              String derived_event_partition = KafkaComponentUtils.extractPartitionFromRecordMetadata(exchange.getProperty("derived_event_recordmetadata"));

              String derived_event_topic = (String)exchange.getProperty("derived_event_topic");

              EventKPI derived_event_kpi = new EventKPI(
                derived_event,
                EventKPI.STATUS.EVENT_CREATED);

              derived_event_kpi.setComponent_route_name(routeId);
              derived_event_kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
              derived_event_kpi.setEvent_topic_name(derived_event_topic);
              derived_event_kpi.setEvent_topic_offset(derived_event_offset);
              derived_event_kpi.setEvent_topic_partition(derived_event_partition);

              exchange.getMessage().setBody(derived_event_kpi);
            }
          })
          .marshal().json(JsonLibrary.Jackson, EventKPI.class)
          .log(LoggingLevel.DEBUG,"Publishing derived event KPI ...")
          .to("direct:publishBodyAsEventKPI")
          .log(LoggingLevel.DEBUG,"Derived event KPI published.")

        .doCatch(Exception.class)
          .log(LoggingLevel.ERROR,"General Exception thrown.")
          .log(LoggingLevel.ERROR,"${exception}")
          .setProperty("error_event_object", body())
          .process(new Processor() {
            public void process(Exchange exchange) throws Exception {

              throw exchange.getException();
            }
          })
      .end()
      .endChoice()
    .end()

    // wireTap makes an call and immediate return without waiting for the process to complete
    // the direct call will wait for a certain time before creating the Report End event.
    .wireTap("direct:generateInformationReportEvent")


    .log(LoggingLevel.INFO, "Checking for exceptions")
    .choice()
      .when(simple("${exchangeProperty.exception} != null"))
        .log(LoggingLevel.INFO, "There is an exception")

        .process(new Processor() {
          public void process(Exchange exchange) throws Exception {

            Exception ex = (Exception)exchange.getProperty("exception");
            throw ex;
          }
        })
      .otherwise()
        .log(LoggingLevel.INFO, "No exception")
        .log(LoggingLevel.ERROR, "Exception: ${exchangeProperty.exception}")
    .end()

    .log(LoggingLevel.INFO, "Completed processCourtCaseChanged")
    ;
  }

  private void generateInformationReportEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO, "Create ReportEvent for Information report")
    // create Report Event for an INFORMATION type report.
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        String event_message_id = exchange.getMessage().getHeader("event_message_id", String.class);
        String court_file_id = exchange.getMessage().getHeader("event_key", String.class);
        ReportEvent re = new ReportEvent();
        re.setEvent_status(ReportEvent.STATUS.REPORT.name());
        re.setEvent_key(court_file_id);
        re.setEvent_source(ReportEvent.SOURCE.JADE_CCM.name());
        re.setJustin_event_message_id(Integer.parseInt(event_message_id));
        re.setJustin_message_event_type_cd(ReportEvent.STATUS.REPORT.name());
        re.setMdoc_justin_no(court_file_id);
        re.setReport_type(ReportEvent.REPORT_TYPES.INFORMATION.name());
        exchange.getMessage().setBody(re, ReportEvent.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, ReportEvent.class)
    .delay(60000)
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
    .log(LoggingLevel.INFO,"key: ${header.number}")
    .process(new Processor() {
      @Override
        public void process(Exchange ex) {
          String autocreateFlag = ex.getProperty("autoCreateFlag",String.class);
          String createOverrideFlag = ex.getProperty("createOverrideFlag",String.class);
          String caseFound = ex.getProperty("caseFound",String.class);
          Boolean autoCreateBoolean = Boolean.valueOf(autocreateFlag);
          Boolean createOverrideBoolean = Boolean.valueOf(createOverrideFlag);
          Boolean caseFoundBoolean = Boolean.valueOf(caseFound!="");
          // If the case is not found in DEMS and autoCreateFlag is true, then set property
          // to create the case in DEMS.
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
        // proceed to create the case in DEMS, if it doesn't exist and criteria is met.
        .process(new Processor() {
          @Override
          public void process(Exchange ex) {
            // KPI: Preserve original event properties
            ex.setProperty("kpi_event_object_orig", ex.getProperty("kpi_event_object"));
            ex.setProperty("kpi_event_topic_offset_orig", ex.getProperty("kpi_event_topic_offset"));
            ex.setProperty("kpi_event_topic_partition_orig", ex.getProperty("kpi_event_topic_partition"));
            ex.setProperty("kpi_event_topic_name_orig", ex.getProperty("kpi_event_topic_name"));
            ex.setProperty("kpi_status_orig", ex.getProperty("kpi_status"));
            ex.setProperty("kpi_component_route_name_orig", ex.getProperty("kpi_component_route_name"));

            String event_message_id = ex.getMessage().getHeader("event_message_id", String.class);
            String rcc_id = (String)ex.getProperty("rcc_id");
            ChargeAssessmentEvent derived_event = new ChargeAssessmentEvent();
            derived_event.setEvent_status(ChargeAssessmentEvent.STATUS.CREATED.toString());
            derived_event.setEvent_source(ChargeAssessmentEvent.SOURCE.JADE_CCM.name());
            derived_event.setEvent_key(rcc_id);
            derived_event.setJustin_rcc_id(rcc_id);
            derived_event.setJustin_event_message_id(Integer.parseInt(event_message_id));
           
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
        .setProperty("kpi_event_topic_partition", simple("${exchangeProperty.kpi_event_topic_partition_orig}"))
        .setProperty("kpi_event_topic_name", simple("${exchangeProperty.kpi_event_topic_name_orig}"))
        .setProperty("kpi_status", simple("${exchangeProperty.kpi_status_orig}"))
        .setProperty("kpi_component_route_name", simple("${exchangeProperty.kpi_component_route_name_orig}"))
      .endChoice()
      .otherwise()
        .log(LoggingLevel.DEBUG,"Generating derived court case event: ${body}")
        .to("direct:processCourtCaseAuthListChanged")
      .endChoice()
    .end()

    // requery if court case exists in DEMS, in case prev logic created the record.
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-lookup-service/getCourtCaseStatusExists")
    .unmarshal().json()
    .setProperty("caseFound").simple("${body[id]}")
    .setProperty("dems_agency_files").simple("${body[agencyFileId]}")
    // look-up the case flags for each related rcc in JUSTIN
    // agencyFileId will have a ";" delimited list of rccs to parse through.
    .choice()
      .when(simple("${exchangeProperty.caseFound} != ''"))
      .doTry()
        .setHeader("number", simple("${exchangeProperty.rcc_id}"))
        .setHeader(Exchange.HTTP_METHOD, simple("GET"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .removeHeader(Exchange.CONTENT_ENCODING)
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
            exchange.setProperty("caseFlagsObject", b.getCase_flags());
            exchange.setProperty("courtNumber", b.getRcc_id());
            exchange.setProperty("accusedList", b.getAccused_persons());
           
          }
        })

        .log(LoggingLevel.INFO, "Case Flags initial: ${body}")
        .setProperty("caseFlags", simple("${body}"))

        // Go through list of related rccs and amalgamate case flags
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) {
            String demsAgencyFiles = (String)exchange.getProperty("dems_agency_files", String.class);
            String primaryRccId = (String)exchange.getProperty("rcc_id", String.class);
            //log.info("agencyFileIds: "+demsAgencyFiles);
            String[] demsAgencyFileList = demsAgencyFiles.split(";");
            ArrayList<String> agencyFileList = new ArrayList<String>();
            if(demsAgencyFileList != null && demsAgencyFileList.length > 0) {
              for(String demsAgencyFileId : demsAgencyFileList) {
                demsAgencyFileId = demsAgencyFileId.trim();
                //log.info("Comparing rcc: "+demsAgencyFileId);
                if(demsAgencyFileId.equalsIgnoreCase(primaryRccId)) {
                  continue;
                } else {
                  agencyFileList.add(demsAgencyFileId);
                }
              }
            }
            exchange.getMessage().setBody(agencyFileList);
          }
        })

        .log(LoggingLevel.INFO, "Unprocessed agency file list: ${body}")
        .split().jsonpathWriteAsString("$.*")
       
          .setProperty("agencyFileId", simple("${body}"))
          .log(LoggingLevel.DEBUG, "agency file: ${exchangeProperty.agencyFileId}")
          .process(new Processor() {
            @Override
            public void process(Exchange exchange) {
              String agencyFileId = exchange.getProperty("agencyFileId", String.class);
              //log.info("agencyFileId:"+agencyFileId);
              exchange.setProperty("agencyFileId", agencyFileId.replaceAll("\"", ""));
            }
          })

          .choice()
            .when(simple("${exchangeProperty.agencyFileId} != ''"))
            
              .log(LoggingLevel.DEBUG, "agency file id updated: ${exchangeProperty.agencyFileId}")
              .setHeader("number").simple("${exchangeProperty.agencyFileId}")
              .setHeader(Exchange.HTTP_METHOD, simple("GET"))
              .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
              .removeHeader(Exchange.CONTENT_ENCODING)
              .to("http://ccm-lookup-service/getCourtCaseDetails")

              .log(LoggingLevel.INFO,"Retrieved related Court Case from JUSTIN: ${body}")
              .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentData.class)
              .process(new Processor() {
                @Override
                public void process(Exchange exchange) {
                  ChargeAssessmentData bcm = exchange.getIn().getBody(ChargeAssessmentData.class);
                  // go through list of existing case flags and add any which aren't already existing.
                  List<String> existingCaseFlags = (List<String>)exchange.getProperty("caseFlagsObject", List.class);
                  //log.info("Printing flags:" + exchange.getProperty("caseFlags", String.class));

                  //log.info("Initial case flag list size: "+existingCaseFlags.size());
                  //log.info("Initial case flag list: "+existingCaseFlags);
                  for(String flag : bcm.getCase_flags()) {
                    //log.info("Check On: " + flag);
                    if(!existingCaseFlags.contains(flag)) {
                      //log.info("Adding: " + flag);
                      existingCaseFlags.add(flag);
                    }
                  }
                  //log.info("Final case flag list size: "+existingCaseFlags.size());
                  //log.info("Final case flag list: "+existingCaseFlags);
                  exchange.getMessage().setBody(existingCaseFlags);
                  //log.info("Set the property for caseFlags");
                  exchange.setProperty("caseFlagsObject", existingCaseFlags);
                }
              })
              .setProperty("caseFlags", simple("${body}"))
              //.log(LoggingLevel.INFO, "After the print case flags")
              //.log(LoggingLevel.DEBUG, "Properties Case Flags: ${exchangeProperty.caseFlags}")

              .setProperty("caseFlags", simple("${exchangeProperty.caseFlagsObject}"))
              //.log(LoggingLevel.INFO, "Properties Case Flags Object: ${exchangeProperty.caseFlags}")
            .endChoice()
          .end()
          //.log(LoggingLevel.INFO, "Properties Case Flags2: ${exchangeProperty.caseFlags}")
        .end()

        .log(LoggingLevel.INFO, "Case Flags: ${exchangeProperty.caseFlags}")

        .doTry()
          // reset the original values and add the JUSTIN derived list of case flags to the header.
          .setHeader("number", simple("${exchangeProperty.event_key_orig}"))
          .setHeader("event_key", simple("${exchangeProperty.event_key_orig}"))
          .setHeader("rcc_id", simple("${exchangeProperty.rcc_id}"))
          .setHeader("caseFound", simple("${exchangeProperty.caseFound}"))
          .setHeader("caseFlags", simple("${exchangeProperty.caseFlags}"))
          .log(LoggingLevel.DEBUG,"Found related court case. Rcc_id: ${header.rcc_id}")
          .setBody(simple("${exchangeProperty.metadata_data}"))
          .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
          .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))

          .to("http://ccm-dems-adapter/updateCourtCaseWithMetadata")

          //.log(LoggingLevel.DEBUG,"Completed update of court case. ${body}")
        .endDoTry()
        .doCatch(HttpOperationFailedException.class)
          .log(LoggingLevel.ERROR,"Exception in updateMetadataCourtCase call")
          .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${exception.statusCode}"))
          .setHeader("CCMException", simple("${exception.statusCode}"))

          .process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
              try {
                HttpOperationFailedException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);
                exchange.getMessage().setBody(cause.getResponseBody());

                log.error("HttpOperationFailedException returned body : " + exchange.getMessage().getBody(String.class));

                exchange.setProperty("exception", cause);

                if(exchange != null && exchange.getMessage() != null && exchange.getMessage().getBody() != null) {
                  String body = Base64.getEncoder().encodeToString(exchange.getMessage().getBody(String.class).getBytes());
                  exchange.getIn().setHeader("CCMExceptionEncoded", body);
                }
              } catch(Exception ex) {
                ex.printStackTrace();
              }
            }
          })

          .log(LoggingLevel.WARN, "Failed Case Court File Update: ${exchangeProperty.exception}")
          .log(LoggingLevel.ERROR,"CCMException: ${header.CCMException}")
        .end()

        .log(LoggingLevel.DEBUG,"Get accused list from court case.")
        // set the updated accusedList object to be the body to use it to retrieve all the accused
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) {
            ArrayList<String> accusedList = (ArrayList<String>)exchange.getProperty("accusedList", ArrayList.class);

            exchange.getMessage().setBody(accusedList, ArrayList.class);
          }
        })
        .marshal().json(JsonLibrary.Jackson, ArrayList.class)
        .setHeader("number",simple("${exchangeProperty.courtNumber}"))
        .log(LoggingLevel.DEBUG, "calling processAccused persons ${body}")
        .to("direct:processAccusedPersons")

      .endChoice()
      .otherwise()
        .log(LoggingLevel.WARN,"Case (rcc_id ${exchangeProperty.rcc_id}) not found; do nothing.")
      .endChoice()
    .end()
    ;
  }

  private void processCaseMerge() {
    // use method name as route id
    //IN: property = metadata_data (CourtCaseData)
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO, "processCaseMerge")

    .removeProperties("primary_courtcase_object")
    .setProperty("primary_rcc_id", simple(""))

    .setBody(simple("${exchangeProperty.metadata_data}"))

    .unmarshal().json(JsonLibrary.Jackson, CourtCaseData.class)

    // look for the primary rcc id in the related agency file list.
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        CourtCaseData ccd = exchange.getIn().getBody(CourtCaseData.class);
        ChargeAssessmentDataRef cadr = ccd.getPrimary_agency_file();
        exchange.setProperty("primary_rcc_id", cadr.getRcc_id());
        exchange.setProperty("primary_agency_file", cadr.getAgency_file_no());
      }
    })

    // Grab the justin court case details for the primary rcc id as well as its dems status info.
    .choice()
      .when(simple(" ${exchangeProperty.primary_rcc_id} != ''"))

        .setHeader("number").simple("${exchangeProperty.primary_rcc_id}")
        .setHeader(Exchange.HTTP_METHOD, simple("GET"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .removeHeader(Exchange.CONTENT_ENCODING)
        .to("http://ccm-lookup-service/getCourtCaseDetails")

        .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentData.class)
        .setProperty("primary_courtcase_object", body())

        .setHeader("key").simple("${exchangeProperty.primary_rcc_id}")
        .setHeader("event_key",simple("${exchangeProperty.primary_rcc_id}"))
        .setHeader("number",simple("${exchangeProperty.primary_rcc_id}"))
        .setHeader(Exchange.HTTP_METHOD, simple("GET"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        // make sure that this primary record is not inactivated.
        .to("http://ccm-lookup-service/getCourtCaseStatusExists")
        .log(LoggingLevel.DEBUG, "primary rcc status lookup: ${body}")
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
      .setProperty("agency_file_no", jsonpath("$.agency_file_no"))
      .setProperty("primary_yn", jsonpath("$.primary_yn"))

      .choice()
        .when(simple("${exchangeProperty.rcc_id} != ${exchangeProperty.primary_rcc_id}"))
          .setHeader("number").simple("${exchangeProperty.rcc_id}")
          .setHeader(Exchange.HTTP_METHOD, simple("GET"))
          .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
          .removeHeader(Exchange.CONTENT_ENCODING)
          .to("http://ccm-lookup-service/getCourtCaseDetails")
          .setProperty("courtcase_data", simple("${bodyAs(String)}"))
          .log(LoggingLevel.DEBUG, "Non-primary record: ${exchangeProperty.courtcase_data}")

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
              agencyFileName = agencyFileName.replaceAll(" ", "_");
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
                exchange.setProperty("primary_agency_file", (String)exchange.getProperty("agency_file_no", String.class));
              }
              exchange.setProperty("primary_courtcase_object", assessmentData);
            }
          })

        .endChoice()
        .otherwise()
          .setProperty("primary_rcc_id", simple("${exchangeProperty.rcc_id}"))
          .setProperty("primary_agency_file", simple("${exchangeProperty.agency_file_no}"))
        .endChoice()
      .end()

      // Grab the dems status of the non-primary dems case
      .setHeader("key").simple("${exchangeProperty.rcc_id}")
      .setHeader("event_key",simple("${exchangeProperty.rcc_id}"))
      .setHeader("number",simple("${exchangeProperty.rcc_id}"))
      //.log(LoggingLevel.INFO,"Retrieve court case status first")
      .setHeader(Exchange.HTTP_METHOD, simple("GET"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      // make sure that this primary record is not inactivated.
      .to("http://ccm-lookup-service/getCourtCaseStatusExists")
      .log(LoggingLevel.DEBUG, "rcc status lookup: ${body}")
      .unmarshal().json()
      // if the non-primary case is active, then merge it into the primary.
      .setProperty("sourceCaseId").simple("${body[id]}")
      .setProperty("sourceCaseStatus").simple("${body[status]}")
      .log(LoggingLevel.INFO, "Source Case Id: ${exchangeProperty.sourceCaseId}")
      .log(LoggingLevel.INFO, "primary vs current: ${exchangeProperty.primary_rcc_id} vs ${exchangeProperty.rcc_id}")
      .log(LoggingLevel.INFO, "primary vs current: ${exchangeProperty.primary_agency_file} vs ${exchangeProperty.agency_file_no}")

      // if the non primary dems case is still active, make call which will export the records over to the primary rcc
      // and then set the non primary case to no longer be active.
      .choice()
        .when(simple("${exchangeProperty.primary_rcc_id} != ${exchangeProperty.rcc_id} && ${body[status]} == 'Active' && ${exchangeProperty.sourceCaseId} != '' && ${exchangeProperty.destinationCaseId} != ''"))
          // make call to merge docs and inactivate the non primary one
          .setHeader("sourceCaseId").simple("${exchangeProperty.sourceCaseId}")
          .setHeader("destinationCaseId").simple("${exchangeProperty.destinationCaseId}")
          .setHeader("prefixName").simple("${exchangeProperty.merge_prefix}")
          .to("http://ccm-dems-adapter/mergeCaseRecordsAndInactivateCase")
        .endChoice()
        .otherwise()
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
    .log(LoggingLevel.DEBUG, "Final charge assessment list: ${body}")
    .setProperty("courtcase_data", simple("${bodyAs(String)}"))
    // add-on any additional rccs which are listed in the dems side.
    .to("direct:compileRelatedChargeAssessments")

    .setProperty("primary_courtcase", simple("${bodyAs(String)}"))
    .setBody(simple("${exchangeProperty.primary_courtcase}"))
    // proceed to update the agency file info in the case.
    //.log(LoggingLevel.INFO, "Agency File: ${body}")
    .choice()
      .when(simple("${exchangeProperty.destinationCaseId} != '' && ${exchangeProperty.destinationCasesStatus} != 'Inactive'"))
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

    .unmarshal().json(JsonLibrary.Jackson, CaseAppearanceSummaryList.class)
    .setProperty("appearance_list_object", body())

    .setHeader("number", simple("${header[event_key]}"))
    .to("direct:compileRelatedCourtFiles")
    .log(LoggingLevel.DEBUG,"Retrieved Court Case Metadata from JUSTIN: ${body}")
    .setProperty("metadata_data", simple("${bodyAs(String)}"))
    // grab appearances from other related court files and add to appearance_list_object
    .split()
      .jsonpathWriteAsString("$.related_court_cases")
      .setHeader("number", jsonpath("$.court_file_id"))
      .setHeader(Exchange.HTTP_METHOD, simple("GET"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .to("http://ccm-lookup-service/getCourtCaseAppearanceSummaryList")

      .log(LoggingLevel.DEBUG, "JUSTIN Case Summary for cf ${header.number}: ${body}")
      .unmarshal().json(JsonLibrary.Jackson, CaseAppearanceSummaryList.class)
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) {
          CaseAppearanceSummaryList casl = exchange.getIn().getBody(CaseAppearanceSummaryList.class);
          CaseAppearanceSummaryList casList = (CaseAppearanceSummaryList)exchange.getProperty("appearance_list_object", CaseAppearanceSummaryList.class);
          casList.getApprsummary().addAll(casl.getApprsummary());

          exchange.setProperty("appearance_list_object", casList);
        }
      })
    .end()

    // set the updated CaseAppearanceSummaryList object to be the body to use it to retrieve the earliest date.
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        CaseAppearanceSummaryList appearancedata = (CaseAppearanceSummaryList)exchange.getProperty("appearance_list_object", CaseAppearanceSummaryList.class);

        exchange.getMessage().setBody(appearancedata, CaseAppearanceSummaryList.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, CaseAppearanceSummaryList.class)

    .setProperty("business_data", simple("${bodyAs(String)}"))
    .log(LoggingLevel.DEBUG, "business_data: ${exchangeProperty.business_data}")

    .setBody(simple("${exchangeProperty.metadata_data}"))
    .log(LoggingLevel.DEBUG, "metadata_data: ${body}")
    .unmarshal().json(JsonLibrary.Jackson, CourtCaseData.class)

    // look for the primary rcc id in the related agency file list.
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        CourtCaseData ccd = exchange.getIn().getBody(CourtCaseData.class);
        ChargeAssessmentDataRef cadr = ccd.getPrimary_agency_file();
        exchange.setProperty("primary_rcc_id", cadr.getRcc_id());
        exchange.setProperty("primary_agency_file", cadr.getAgency_file_no());
      }
    })

    .setProperty("rcc_id", simple("${exchangeProperty.primary_rcc_id}"))
    .log(LoggingLevel.INFO,"Check case (rcc_id ${exchangeProperty.rcc_id}) existence ...")
    .setHeader("number", simple("${exchangeProperty.rcc_id}"))
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-lookup-service/getCourtCaseStatusExists")
    .log(LoggingLevel.DEBUG, "${body}")
    .unmarshal().json()

    .setProperty("primary_rcc_id", simple("${body[primaryAgencyFileId]}"))
    .setProperty("caseStatus").simple("${body[status]}")
    .log(LoggingLevel.INFO, "primary_rcc_id: ${exchangeProperty.primary_rcc_id}")

    // Make sure that we are re-routing to the primary case, if the value is set.
    .choice()
      .when(simple("${exchangeProperty.caseStatus} == 'Inactive' && ${exchangeProperty.primary_rcc_id} != ''"))
        .log(LoggingLevel.INFO, "get primary value")
        .setHeader("key").simple("${exchangeProperty.primary_rcc_id}")
        .setHeader("event_key",simple("${exchangeProperty.primary_rcc_id}"))
        .setHeader("number",simple("${exchangeProperty.primary_rcc_id}"))
        .setProperty("rcc_id", simple("${exchangeProperty.primary_rcc_id}"))
        //.log(LoggingLevel.INFO,"Retrieve court case status first")
        .setHeader(Exchange.HTTP_METHOD, simple("GET"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .to("http://ccm-lookup-service/getCourtCaseStatusExists")
        .log(LoggingLevel.DEBUG, "${body}")
        .unmarshal().json()
      .endChoice()
    .end()
    .setProperty("caseId").simple("${body[id]}")
    .setProperty("caseStatus").simple("${body[status]}")

    .choice()
      .when(simple("${exchangeProperty.caseId} != '' && ${exchangeProperty.caseStatus} != 'Inactive'"))
        .setHeader("rcc_id", simple("${exchangeProperty.rcc_id}"))
        .log(LoggingLevel.INFO,"Updating appearance summary for case. Rcc_id: ${header.rcc_id}")
        .setBody(simple("${exchangeProperty.business_data}"))
        .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .to("http://ccm-dems-adapter/updateCourtCaseWithAppearanceSummary")
        .endChoice()
      .otherwise()
        .log(LoggingLevel.INFO,"Case (rcc_id ${exchangeProperty.rcc_id}) not found or inactive; do nothing.")
      .endChoice()
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
    .unmarshal().json(JsonLibrary.Jackson, CaseCrownAssignmentList.class)
    .setProperty("assignment_list_object", body())

    .setHeader("number", simple("${header[event_key]}"))
    .to("direct:compileRelatedCourtFiles")
    .setProperty("metadata_data", simple("${bodyAs(String)}"))

    .log(LoggingLevel.DEBUG, "metadata: ${body}")
    // grab assignments from other related court files and add to assignment_list_object
    .split()
      .jsonpathWriteAsString("$.related_court_cases")
      .setHeader("number", jsonpath("$.court_file_id"))
      .setHeader(Exchange.HTTP_METHOD, simple("GET"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .to("http://ccm-lookup-service/getCourtCaseCrownAssignmentList")
      .log(LoggingLevel.DEBUG, "JUSTIN Case Crown Assignment for cf ${header.number}: ${body}")

      .unmarshal().json(JsonLibrary.Jackson, CaseCrownAssignmentList.class)
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) {
          CaseCrownAssignmentList casl = exchange.getIn().getBody(CaseCrownAssignmentList.class);
          CaseCrownAssignmentList casList = (CaseCrownAssignmentList)exchange.getProperty("assignment_list_object", CaseCrownAssignmentList.class);
          casList.addCrownAssignment(casl);

          exchange.setProperty("assignment_list_object", casList);
        }
      })
    .end()

    // set the updated CaseCrownAssignmentList object to be the body to use it to retrieve all the assignments
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        CaseCrownAssignmentList appearancedata = (CaseCrownAssignmentList)exchange.getProperty("assignment_list_object", CaseCrownAssignmentList.class);

        exchange.getMessage().setBody(appearancedata, CaseCrownAssignmentList.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, CaseCrownAssignmentList.class)

    .setProperty("business_data", simple("${bodyAs(String)}"))
    .log(LoggingLevel.DEBUG, "business_data: ${exchangeProperty.business_data}")

    .setBody(simple("${exchangeProperty.metadata_data}"))

    .unmarshal().json(JsonLibrary.Jackson, CourtCaseData.class)

    // look for the primary rcc id in the related agency file list.
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        CourtCaseData ccd = exchange.getIn().getBody(CourtCaseData.class);
        ChargeAssessmentDataRef cadr = ccd.getPrimary_agency_file();
        exchange.setProperty("primary_rcc_id", cadr.getRcc_id());
        exchange.setProperty("primary_agency_file", cadr.getAgency_file_no());
      }
    })

    .setProperty("rcc_id", simple("${exchangeProperty.primary_rcc_id}"))
    .log(LoggingLevel.INFO,"Check case (rcc_id ${exchangeProperty.rcc_id}) existence ...")
    .setHeader("number", simple("${exchangeProperty.rcc_id}"))
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-lookup-service/getCourtCaseStatusExists")
    .log(LoggingLevel.DEBUG, "${body}")
    .unmarshal().json()

    .setProperty("primary_rcc_id", simple("${body[primaryAgencyFileId]}"))
    .setProperty("caseStatus").simple("${body[status]}")
    .log(LoggingLevel.INFO, "primary_rcc_id: ${exchangeProperty.primary_rcc_id}")

    // Make sure that we are re-routing to the primary case, if the value is set.
    .choice()
      .when(simple("${exchangeProperty.caseStatus} == 'Inactive' && ${exchangeProperty.primary_rcc_id} != ''"))
      .log(LoggingLevel.INFO, "get primary value")
        .setHeader("key").simple("${exchangeProperty.primary_rcc_id}")
        .setHeader("event_key",simple("${exchangeProperty.primary_rcc_id}"))
        .setHeader("number",simple("${exchangeProperty.primary_rcc_id}"))
        .setProperty("rcc_id", simple("${exchangeProperty.primary_rcc_id}"))
        //.log(LoggingLevel.INFO,"Retrieve court case status first")
        .setHeader(Exchange.HTTP_METHOD, simple("GET"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .to("http://ccm-lookup-service/getCourtCaseStatusExists")
        .log(LoggingLevel.DEBUG, "${body}")
        .unmarshal().json()
      .endChoice()
    .end()
    .setProperty("caseId").simple("${body[id]}")
    .setProperty("caseStatus").simple("${body[status]}")

    .choice()
      .when(simple("${exchangeProperty.caseId} != '' && ${exchangeProperty.caseStatus} != 'Inactive'"))
        .setHeader("rcc_id", simple("${exchangeProperty.rcc_id}"))
        .log(LoggingLevel.INFO,"Found related court case. Rcc_id: ${header.rcc_id}")
        .setBody(simple("${exchangeProperty.business_data}"))
        .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .to("http://ccm-dems-adapter/updateCourtCaseWithCrownAssignmentData")
      .endChoice()
      .otherwise()
        .log(LoggingLevel.INFO,"Case (rcc_id ${exchangeProperty.rcc_id}) not found or inactive; do nothing.")
      .endChoice()
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
    // extract kpi_event_topic_partition
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        // extract the offset from response header.  Example format: "[some-topic-0@301]"

        try {
          // https://kafka.apache.org/30/javadoc/org/apache/kafka/clients/producer/RecordMetadata.html
          Object o = (Object)exchange.getProperty("kpi_event_topic_recordmetadata");
          String recordMetadata = o.toString();

          String event_offset = KafkaComponentUtils.extractOffsetFromRecordMetadata(recordMetadata);
          String event_partition = KafkaComponentUtils.extractPartitionFromRecordMetadata(recordMetadata);

          exchange.setProperty("kpi_event_topic_offset", event_offset);
          exchange.setProperty("kpi_event_topic_partition", event_partition);
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
    //IN: property = kpi_event_topic_partition
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
        kpi.setEvent_topic_partition(exchange.getProperty("kpi_event_topic_partition"));
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

  private void processAccusedPersons() {
    // input params:
    // List<CaseAccused>, rcc_id

    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: property = kpi_object
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"syncAccusedPersons ${header.number}")
    .log(LoggingLevel.INFO,"Processing request: ${body}")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-dems-adapter/syncAccusedPersons")
    .end();
  }

}