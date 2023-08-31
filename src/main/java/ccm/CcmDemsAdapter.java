package ccm;

import org.apache.camel.CamelException;

// To run this integration use:
// kamel run CcmDemsEdgeAdapter.java --property file:application.properties --profile openshift
//
// recover the service location. If you're running on minikube, minikube service platform-http-server --url=true
// curl -H "name:World" http://<service-location>/hello
//

// camel-k: language=java
// camel-k: dependency=mvn:org.apache.camel.quarkus
// camel-k: dependency=mvn:org.apache.camel.camel-quarkus-kafka
// camel-k: dependency=mvn:org.apache.camel.camel-quarkus-jsonpath
// camel-k: dependency=mvn:org.apache.camel.camel-jackson
// camel-k: dependency=mvn:org.apache.camel.camel-splunk-hec
// camel-k: dependency=mvn:org.apache.camel.camel-http
// camel-k: dependency=mvn:org.apache.camel.camel-http-common
// camel-k: dependency=mvn:org.slf4j.slf4j-api
// camel-k: dependency=mvn:org.apache.httpcomponents.httpcore
// camel-k: dependency=mvn:org.apache.httpcomponents.httpmime
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-mail
// camel-k: dependency=mvn:org.apache.camel:camel-kamelet
// camel-k: dependency=mvn:org.apache.camel:camel-java-joor-dsl
// camel-k: dependency=mvn:org.apache.camel:camel-endpointdsl
// camel-k: dependency=mvn:org.apache.camel:camel-rest
// camel-k: dependency=mvn:org.apache.camel:camel-http
// camel-k: dependency=mvn:org.apache.camel:camel-kafka
// camel-k: dependency=mvn:org.apache.camel:camel-core-languages
// camel-k: dependency=mvn:org.apache.camel:camel-mail
// camel-k: dependency=mvn:org.apache.camel:camel-attachments


import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import java.nio.charset.StandardCharsets;
import org.apache.camel.support.builder.ValueBuilder;


import com.fasterxml.jackson.databind.ObjectMapper;

import ccm.models.common.data.CourtCaseData;
import ccm.models.common.event.BaseEvent;
import ccm.models.common.event.EventKPI;
import ccm.models.common.data.AuthUser;
import ccm.models.common.data.AuthUserList;
import ccm.models.common.data.CaseAccused;
import ccm.models.common.data.CaseAppearanceSummaryList;
import ccm.models.common.data.CaseCrownAssignmentList;
import ccm.models.common.data.CaseHyperlinkData;
import ccm.models.common.data.ChargeAssessmentData;
import ccm.models.common.data.ChargeAssessmentDataRefList;
import ccm.models.common.data.document.ChargeAssessmentDocumentData;
import ccm.models.common.data.document.CourtCaseDocumentData;
import ccm.models.common.data.document.ImageDocumentData;
import ccm.models.common.data.document.ReportDocument;
import ccm.models.common.event.ReportEvent;
import ccm.models.common.event.Error;
import ccm.models.system.justin.JustinDocumentKeyList;
import ccm.models.system.dems.*;
import ccm.utils.DateTimeUtils;
import ccm.utils.JsonParseUtils;


import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;



//import org.apache.camel.http.common.HttpOperationFailedException;

public class CcmDemsAdapter extends RouteBuilder {
  @Override
  public void configure() throws Exception {

    attachExceptionHandlers();
    version();
    dems_version();
    getDemsFieldMappings();
    getDemsCaseFlagId();
    getCourtCaseExists();
    getCourtCaseIdByKey();
    getCourtCaseDataById();
    getCourtCaseDataByKey();
    getCourtCaseNameByKey();
    getCourtCaseStatusExists();
    getCourtCaseStatusByKey ();
    getCourtCaseStatusById();
    getCourtCaseCourtFileUniqueIdByKey();
    getCaseHyperlink();
    createCourtCase();
    updateCourtCase();
    updateCourtCaseWithMetadata();
    updateCourtCaseWithAppearanceSummary();
    updateCourtCaseWithCrownAssignmentData();
    http_syncCaseUserList();
    syncCaseUserList();
    processAccusedPerson();
    getPersonExists();
    getPersonByKey();
    createPerson();
    updatePerson();
    addParticipantToCase();
    getGroupMapByCaseId();
    prepareDemsCaseGroupMembersSyncHelperList();
    syncCaseGroupMembers();
    getCaseListByUserKey();
    processReportEvents();
    processDocumentRecord();
    processNonStaticDocuments();
    checkIncrementRecordDocId();
    changeDocumentRecord();
    updateDocumentRecord();
    createDocumentRecord();
    createCaseRecord();
    updateCaseRecord();
    streamCaseRecord();
    mergeCaseRecordsAndInactivateCase();
    getCaseRecordExistsByKey();
    getCaseRecordIdByDescription();
    getCaseDocIdExistsByKey();
    getCaseRecordIdByDocId();
    processUnknownStatus();
    publishEventKPI();
    deleteJustinRecords();
    inactivateCase();
  }



  private void attachExceptionHandlers() {


    // handle network connectivity errors
    onException(ConnectException.class, SocketTimeoutException.class)
      .backOffMultiplier(2)

      .log(LoggingLevel.ERROR,"onException(ConnectException, SocketTimeoutException) called.")
      .setBody(constant("An unexpected network error occurred"))
      .retryAttemptedLogLevel(LoggingLevel.ERROR)
      .handled(false)
    .end();

    // HttpOperation Failed
    onException(HttpOperationFailedException.class)
    .choice()
      .when(simple("${exchangeProperty.kpi_event_object} != null"))
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
            log.error("Response body : " + cause.getResponseBody());
            exchange.setProperty("error_status_code", cause.getStatusCode());

            log.error("HttpOperationFailed Exception event info : " + event.getEvent_source());
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
        //.handled(true)
        .to("kafka:{{kafka.topic.kpis.name}}")
        .endChoice()
      .otherwise()
        .log(LoggingLevel.ERROR, "HttpOperationFailedException thrown: ${exception.message}")
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            try {
              HttpOperationFailedException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);

              if(cause != null && cause.getResponseBody() != null) {
                String body = Base64.getEncoder().encodeToString(cause.getResponseBody().getBytes());
                exchange.getMessage().setBody(body);
              }
              log.error("Returned body : " + cause.getResponseBody());
            } catch(Exception ex) {
              ex.printStackTrace();
            }
          }
        })
        .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${exception.statusCode}"))
        .transform().simple("${body}")
        .setHeader("CCMException", simple("{\"error\": \"${exception.message}\"}"))
        .setHeader("CCMExceptionEncoded", simple("${body}"))
      .end()

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

    // Handle Camel Exception
    onException(CamelException.class)
    .choice()
      .when(simple("${exchangeProperty.kpi_event_object} != null"))
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            BaseEvent event = (BaseEvent)exchange.getProperty("kpi_event_object");
            Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
            ccm.models.common.event.Error error = new ccm.models.common.event.Error();
            error.setError_dtm(DateTimeUtils.generateCurrentDtm());
            error.setError_dtm(DateTimeUtils.generateCurrentDtm());
            error.setError_code("CamelException");
            error.setError_summary("Unable to process event, CamelException raised.");
            error.setError_details(cause.getMessage());
            if(cause != null) {
              cause.printStackTrace();
            }

            log.debug("CamelException caught, exception message : " + cause.getMessage() + " stack trace : " + cause.getStackTrace());
            log.error("CamelException Exception event info : " + event.getEvent_source());
            // KPI
            EventKPI kpi = new EventKPI(event, EventKPI.STATUS.EVENT_PROCESSING_FAILED);
            // KPI

            kpi.setEvent_topic_name((String)exchange.getProperty("kpi_event_topic_name"));
            kpi.setEvent_topic_offset(exchange.getProperty("kpi_event_topic_offset"));
            kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
            kpi.setComponent_route_name((String)exchange.getProperty("kpi_component_route_name"));
            kpi.setError(error);
            exchange.getMessage().setBody(kpi);
            String failedRouteId = exchange.getProperty(Exchange.FAILURE_ROUTE_ID, String.class);
            exchange.setProperty("kpi_component_route_name", failedRouteId);
          }
        })
        .marshal().json(JsonLibrary.Jackson, EventKPI.class)
        .log(LoggingLevel.ERROR,"Publishing derived event KPI in Exception handler ...")
        .log(LoggingLevel.INFO,"Derived event KPI published.")
        .log("Caught CamelException exception")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
        .setProperty("error_event_object", body())
        .to("kafka:{{kafka.topic.kpis.name}}")
        .endChoice()
      .otherwise()
        .log(LoggingLevel.ERROR, "CamelException thrown: ${exception.message}")
        .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("500"))
        .setBody(simple("{\"error\": \"${exception.message}\"}"))
        .transform().simple("Error reported: ${exception.message} - cannot process this message.")
        .setHeader(Exchange.HTTP_RESPONSE_TEXT, simple("{\"error\": \"${exception.message}\"}"))
        .setHeader("CCMException", simple("{\"error\": \"${exception.message}\"}"))
      .end()
    .end();

    // General Exception
    onException(Exception.class)
    .choice()
      .when(simple("${exchangeProperty.kpi_event_object} != null"))
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            BaseEvent event = (BaseEvent)exchange.getProperty("kpi_event_object");
            Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);

            ccm.models.common.event.Error error = new ccm.models.common.event.Error();
            error.setError_dtm(DateTimeUtils.generateCurrentDtm());
            error.setError_dtm(DateTimeUtils.generateCurrentDtm());
            error.setError_summary(cause.getMessage());
            error.setError_code("General Exception");
            error.setError_details(cause.toString());
            if(cause != null) {
              cause.printStackTrace();
            }

            log.error("General Exception caught, exception message : " + cause.getMessage());
            log.error("General Exception event info : " + event.getEvent_source());
            // KPI
            EventKPI kpi = new EventKPI(event, EventKPI.STATUS.EVENT_PROCESSING_FAILED);

            kpi.setEvent_topic_name((String)exchange.getProperty("kpi_event_topic_name"));
            kpi.setEvent_topic_offset(exchange.getProperty("kpi_event_topic_offset"));
            kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
            kpi.setComponent_route_name((String)exchange.getProperty("kpi_component_route_name"));
            kpi.setError(error);
            exchange.getMessage().setBody(kpi);

            String failedRouteId = exchange.getProperty(Exchange.FAILURE_ROUTE_ID, String.class);
            exchange.setProperty("kpi_component_route_name", failedRouteId);
          }
        })
        .marshal().json(JsonLibrary.Jackson, EventKPI.class)
        .log(LoggingLevel.ERROR,"Publishing derived event KPI in Exception handler ...")
        .log(LoggingLevel.INFO,"Derived event KPI published.")
        .log("Caught General exception exception")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
        .setProperty("error_event_object", body())
        .to("kafka:{{kafka.topic.kpis.name}}")
        .endChoice()
      .otherwise()
        .log(LoggingLevel.ERROR, "General Exception thrown: ${exception.message}")
        .log("Body: ${body}")
        .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("500"))
        .setBody(simple("{\"error\": \"${exception.message}\"}"))
        .transform().simple("Error reported: ${exception.message} - cannot process this message.")
        .setHeader(Exchange.HTTP_RESPONSE_TEXT, simple("{\"error\": \"${exception.message}\"}"))
        .setHeader("CCMException", simple("{\"error\": \"${exception.message}\"}"))
      .end()
   .end()
   ;
  }

  private void getCourtCaseStatusExists() {
     // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: header.number
    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"status exists key = ${header[number]}...")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .removeHeader("kafka.HEADERS")
    .removeHeaders("x-amz*")
    
    .to("direct:getCourtCaseStatusByKey")
    .end()
    ;
  }

  private void getCourtCaseStatusByKey() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    //IN: header.number
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html

    .setProperty("key", simple("${header.number}"))
    .log(LoggingLevel.INFO,"Case status exists key = ${exchangeProperty.key}")

    .to("direct:getCourtCaseIdByKey")
    .setProperty("id", jsonpath("$.id"))

    .to("direct:getCourtCaseStatusById")
  ;
  }
 
  private void getCourtCaseStatusById() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    //IN: header.number
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html

    .setProperty("caseNotFound", simple("{\"id\": \"\", \"key\": \"\", \"name\": \"\", \"caseState\": \"\", \"primaryAgencyFileId\": \"\", \"primaryAgencyFileNo\": \"\", \"agencyFileId\": \"\", \"agencyFileNo\": \"\", \"courtFileId\": \"\", \"courtFileNo\": \"\", \"status\": \"\"}"))

    .log(LoggingLevel.INFO, "caseId: '${exchangeProperty.id}'")
    .choice()
      .when(simple("${exchangeProperty.id} != ''"))
        .doTry()
          .to("direct:getCourtCaseDataById")
          .choice()
            .when(simple("${header.CamelHttpResponseCode} == 200"))
              .setProperty("DemsCourtCase", simple("${bodyAs(String)}"))
              .process(new Processor() {
                @Override
                public void process(Exchange exchange) {

                  String courtCaseJson = exchange.getProperty("DemsCourtCase", String.class);
                  String caseId = JsonParseUtils.getJsonElementValue(courtCaseJson, "id");
                  String caseKey = JsonParseUtils.getJsonElementValue(courtCaseJson, "key");
                  String caseName = JsonParseUtils.getJsonElementValue(courtCaseJson, "name");
                  String courtFileUniqueId = JsonParseUtils.getJsonArrayElementValue(courtCaseJson, "/fields", "/name", DemsFieldData.FIELD_MAPPINGS.MDOC_JUSTIN_NO.getLabel(), "/value");
                  String courtFileNo = JsonParseUtils.getJsonArrayElementValue(courtCaseJson, "/fields", "/name", DemsFieldData.FIELD_MAPPINGS.COURT_FILE_NO.getLabel(), "/value");
                  String caseState = JsonParseUtils.getJsonArrayElementValue(courtCaseJson, "/fields", "/name", DemsFieldData.FIELD_MAPPINGS.CASE_STATE.getLabel(),"/value");
                  String primaryAgencyFileId = JsonParseUtils.getJsonArrayElementValue(courtCaseJson, "/fields", "/name", DemsFieldData.FIELD_MAPPINGS.PRIMARY_AGENCY_FILE_ID.getLabel(),"/value");
                  String primaryAgencyFileNo = JsonParseUtils.getJsonArrayElementValue(courtCaseJson, "/fields", "/name", DemsFieldData.FIELD_MAPPINGS.PRIMARY_AGENCY_FILE_NO.getLabel(),"/value");
                  String agencyFileId = JsonParseUtils.getJsonArrayElementValue(courtCaseJson, "/fields", "/name", DemsFieldData.FIELD_MAPPINGS.AGENCY_FILE_ID.getLabel(),"/value");
                  String agencyFileNo = JsonParseUtils.getJsonArrayElementValue(courtCaseJson, "/fields", "/name", DemsFieldData.FIELD_MAPPINGS.AGENCY_FILE_NO.getLabel(),"/value");
                  String status = JsonParseUtils.getJsonElementValue(courtCaseJson, "status");

                  StringBuilder caseObjectJson = new StringBuilder("");
                  caseObjectJson.append("{");
                  caseObjectJson.append("\"id\":");
                  caseObjectJson.append("\"" + caseId + "\",");
                  caseObjectJson.append("\"key\":");
                  caseObjectJson.append("\"" + caseKey + "\",");
                  caseObjectJson.append("\"name\":");
                  caseObjectJson.append("\"" + caseName + "\",");
                  caseObjectJson.append("\"caseState\": ");
                  caseObjectJson.append( "\"" + caseState + "\",");
                  caseObjectJson.append("\"primaryAgencyFileId\": ");
                  caseObjectJson.append("\"" + primaryAgencyFileId + "\",");
                  caseObjectJson.append("\"primaryAgencyFileNo\": ");
                  caseObjectJson.append("\"" + primaryAgencyFileNo + "\",");
                  caseObjectJson.append("\"agencyFileId\": ");
                  caseObjectJson.append("\"" + agencyFileId + "\",");
                  caseObjectJson.append("\"agencyFileNo\": ");
                  caseObjectJson.append("\"" + agencyFileNo + "\",");
                  caseObjectJson.append("\"courtFileId\": ");
                  caseObjectJson.append("\"" + courtFileUniqueId + "\",");
                  caseObjectJson.append("\"courtFileNo\": ");
                  caseObjectJson.append("\"" + courtFileNo + "\",");
                  caseObjectJson.append("\"status\": ");
                  caseObjectJson.append( "\"" + status + "\"");
                  caseObjectJson.append("}");

                  exchange.getMessage().setBody(caseObjectJson.toString());
                }
              })
            .endChoice()
            .otherwise()
              .setBody(simple("${exchangeProperty.caseNotFound}"))
              .setHeader("CamelHttpResponseCode", simple("200"))
              .log(LoggingLevel.INFO,"Case not found.")
            .endChoice()
          .end() // choice end
        .endDoTry()
        .doCatch(Exception.class)
          .log(LoggingLevel.ERROR,"Exception: ${exception}")
          .log(LoggingLevel.INFO,"Exchange Context: ${exchange.context}")
          .setBody(simple("${exchangeProperty.caseNotFound}"))
          .setHeader("CamelHttpResponseCode", simple("200"))
        .end()


      .endChoice()
      .otherwise()
        .setBody(simple("${exchangeProperty.caseNotFound}"))
        .setHeader("CamelHttpResponseCode", simple("200"))
        .log(LoggingLevel.INFO,"Case not found.")
      .endChoice()
    .end()
    .log(LoggingLevel.INFO, "${body}")
  ;
  }

  private void processReportEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("kafka:{{kafka.topic.reports.name}}?groupId=ccm-dems-adapter")
    .routeId(routeId)
    .log(LoggingLevel.INFO,"Event from Kafka {{kafka.topic.reports.name}} topic (offset=${headers[kafka.OFFSET]}): ${body}\n" +
      "    on the topic ${headers[kafka.TOPIC]}\n" +
      "    on the partition ${headers[kafka.PARTITION]}\n" +
      "    with the offset ${headers[kafka.OFFSET]}\n" +
      "    with the key ${headers[kafka.KEY]}")
    .setHeader("event_key")
      .jsonpath("$.event_key")
    .setHeader("event_status")
      .jsonpath("$.event_status")
    .setHeader("rcc_id")
      .jsonpath("$.justin_rcc_id") // image data get does not return this value, so save in headers
    .setHeader("mdoc_justin_no")
      .jsonpath("$.mdoc_justin_no") // image data get does not return this value, so save in headers
    .setHeader("rcc_ids")
      .jsonpath("$.rcc_ids") // image data get does not return this value, so save in headers
    .setHeader("image_id")
      .jsonpath("$.image_id") // image data get does not return this value, so save in headers
    .setHeader("filtered_yn")
      .jsonpath("$.filtered_yn") // image data get does not return this value, so save in headers
    .setHeader("event_message_id")
      .jsonpath("$.justin_event_message_id")
    .setProperty("rcc_ids", simple("${headers[rcc_ids]}"))
    .setHeader("event").simple("${body}")
    .unmarshal().json(JsonLibrary.Jackson, ReportEvent.class)
    .setProperty("kpi_event_object", body())
    .setProperty("kpi_event_topic_name", simple("${headers[kafka.TOPIC]}"))
    .setProperty("kpi_event_topic_offset", simple("${headers[kafka.OFFSET]}"))
    .log(LoggingLevel.INFO, "rcc_id = ${header[rcc_id]}")
    .log(LoggingLevel.INFO, "part_id = ${header[part_id]}")
    .log(LoggingLevel.INFO, "mdoc_justin_no = ${header[mdoc_justin_no]}")
    .log(LoggingLevel.INFO, "rcc_ids = ${header[rcc_ids]}")
    .log(LoggingLevel.INFO, "image_id = ${header[image_id]}")
    .log(LoggingLevel.INFO, "filtered_yn = ${header[filtered_yn]}")
    .marshal().json(JsonLibrary.Jackson, ReportEvent.class)
    .choice()
      .when(header("event_status").isNotNull())
        .setProperty("kpi_component_route_name", simple("processReportEvents"))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
        .to("direct:publishEventKPI")
        .setBody(header("event"))

        .unmarshal().json(JsonLibrary.Jackson, ReportEvent.class)
        .process(new Processor() {
          @Override
          public void process(Exchange ex) {
            ReportEvent re = ex.getIn().getBody(ReportEvent.class);
            JustinDocumentKeyList keyList = new JustinDocumentKeyList(re);

            ex.getMessage().setBody(keyList);
          }
        })
        .marshal().json(JsonLibrary.Jackson, JustinDocumentKeyList.class)
        .log(LoggingLevel.DEBUG,"Lookup message: '${body}'")
        .to("direct:processDocumentRecord")
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

  private void processDocumentRecord() throws HttpOperationFailedException {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN
    // property: event_object
    // property: caseFound
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    // need to look-up rcc_id if it exists in the body.
    .log(LoggingLevel.DEBUG,"event_key = ${header[event_key]}")
    .setProperty("justin_request").body()
    .log(LoggingLevel.INFO,"rcc_ids = ${exchangeProperty.rcc_ids}")
    .log(LoggingLevel.INFO,"Lookup message: '${body}'")

    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .removeHeaders("CamelHttp*")
    .removeHeader("kafka.HEADERS")
    .removeHeaders("x-amz*")
    .removeHeader(Exchange.CONTENT_ENCODING)

    //.log(LoggingLevel.INFO, "headers: ${headers}")
    .to("http://ccm-lookup-service/getImageData")

    .log(LoggingLevel.DEBUG,"Received image data: '${body}'")
    .setProperty("report_document_list", simple("${bodyAs(String)}"))
    .setProperty("create_date") .jsonpath("$.create_date")
    .log(LoggingLevel.INFO, "create date: ${exchangeProperty.create_date}")

    // For cases like witness statement, there can be multiple docs returned.
    // This will split through each of the documents and process them individually.
    .log(LoggingLevel.INFO,"Parsing through report documents")
    .split()
      .jsonpathWriteAsString("$.documents")
      .log(LoggingLevel.INFO,"Parsing through single report document")
      .log(LoggingLevel.DEBUG,"Body: ${body}")
      // clear-out properties, so that they do not accidentally get re-used for each split iteration.
      .removeProperty("charge_assessment_document")
      .removeProperty("court_case_document")
      .removeProperty("image_document")

      .unmarshal().json(JsonLibrary.Jackson, ReportDocument.class)
      .process(new Processor() {
        @Override
        public void process(Exchange ex) {
          ReportDocument rd = ex.getIn().getBody(ReportDocument.class);
          String event_message_id = ex.getMessage().getHeader("event_message_id", String.class);
          String create_date = ex.getProperty("create_date", String.class);

          log.info("event_message_id: "+event_message_id);
          if(ex.getMessage().getHeader("rcc_id") != null) {
            log.info("processing into charge assessment document record");
            ChargeAssessmentDocumentData chargeAssessmentDocument = new ChargeAssessmentDocumentData(event_message_id, create_date, rd);
            DemsRecordData demsRecord = new DemsRecordData(chargeAssessmentDocument);


            ex.getMessage().setHeader("documentId", demsRecord.getDocumentId());
            ex.setProperty("charge_assessment_document", chargeAssessmentDocument);
            ex.setProperty("drd", demsRecord);

            ex.getMessage().setBody(demsRecord);
          } else if(rd.getPrimary_rcc_id() != null) {
            log.info("processing into charge assessment document record");
            ImageDocumentData imageDocument = new ImageDocumentData(event_message_id, create_date, rd);
            DemsRecordData demsRecord = new DemsRecordData(imageDocument);
            ex.getMessage().setHeader("primary_rcc_id", rd.getPrimary_rcc_id());

            Object mdoc_justin_no = ex.getMessage().getHeader("mdoc_justin_no");
            if(imageDocument.getMdoc_justin_no() != null && mdoc_justin_no == null) {
              ex.getMessage().setHeader("mdoc_justin_no", rd.getMdoc_justin_no());
            }

            ex.setProperty("image_document", imageDocument);
            ex.setProperty("drd", demsRecord);
            ex.setProperty("reportType", demsRecord.getDescriptions());
            ex.setProperty("reportTitle", demsRecord.getTitle());
            ex.getMessage().setHeader("documentId", demsRecord.getDocumentId());

            ex.getMessage().setBody(demsRecord);
          } else {
            log.debug("justin_request: " + ex.getProperty("justin_request",String.class));
            //JustinDocumentKeyList jdkl = (JustinDocumentKeyList)ex.getProperty("justin_request", JustinDocumentKeyList.class);
            log.info("processing into court case document record");
            CourtCaseDocumentData courtCaseDocument = new CourtCaseDocumentData(event_message_id, create_date, rd);
            Object filtered_yn = ex.getMessage().getHeader("filtered_yn");
            if(filtered_yn != null) {
              courtCaseDocument.setFiltered_yn((String)filtered_yn);
            }
            DemsRecordData demsRecord = new DemsRecordData(courtCaseDocument);
            ex.setProperty("reportType", demsRecord.getDescriptions());
            ex.setProperty("reportTitle", demsRecord.getTitle());

            Object mdoc_justin_no = ex.getMessage().getHeader("mdoc_justin_no");
            String rcc_list = ex.getProperty("rcc_ids", String.class);
            //log.info("obj mdoc_justin_no:" + mdoc_justin_no);
            //log.info("string rcc_ids:" + rcc_list);
            if((courtCaseDocument.getRcc_ids() == null || courtCaseDocument.getRcc_ids().isEmpty()) && rcc_list != null) {
              log.info("setting list from header.");
              // Justin won't necessarily return the list of rcc_ids, so need to set it based on report event message.
              ObjectMapper objectMapper = new ObjectMapper();
              try {
                //System.out.println(rcc_list);
                String[] rcc_id_list = objectMapper.readValue(rcc_list, String[].class);
                courtCaseDocument.setRcc_ids(Arrays.asList(rcc_id_list));
              } catch(Exception e) {
                e.printStackTrace();
              }

            }

            if(courtCaseDocument.getMdoc_justin_no() == null && mdoc_justin_no != null) {
              courtCaseDocument.setMdoc_justin_no((String)mdoc_justin_no);
            }

            ex.setProperty("court_case_document", courtCaseDocument);
            ex.setProperty("drd", demsRecord);

            // make sure the header has most up to date values.
            ex.getMessage().setHeader("rcc_ids", courtCaseDocument.getRcc_ids());
            ex.getMessage().setHeader("mdoc_justin_no", courtCaseDocument.getMdoc_justin_no());
            ex.getMessage().setHeader("documentId", demsRecord.getDocumentId());

            ex.getMessage().setBody(demsRecord);
          }
        }

      })
      .marshal().json(JsonLibrary.Jackson, DemsRecordData.class)
      .log(LoggingLevel.INFO,"rcc_id: ${header[rcc_id]}")
      .log(LoggingLevel.INFO,"mdoc_justin_no: ${header[mdoc_justin_no]}")
      .log(LoggingLevel.INFO,"rcc_ids: ${header[rcc_ids]}")
      .log(LoggingLevel.INFO,"image_id: ${header[image_id]}")
      .log(LoggingLevel.DEBUG,"Generating derived dems record: ${body}")
      .setProperty("dems_record").simple("${bodyAs(String)}") // save to properties, in case we need to parse through list of records
      .choice()
        .when(simple("${header.rcc_id} != null"))
          .log(LoggingLevel.INFO,"RCC based report")
          // get the primary rcc, based on the dems primary agency file id

          .setHeader("number", simple("${header[rcc_id]}"))
          // look for current status of the dems case.
          // and set the rcc to the primary rcc
          .to("direct:getCourtCaseStatusByKey")
          .unmarshal().json()
          .setProperty("caseId").simple("${body[id]}")
          .setProperty("caseStatus").simple("${body[status]}")
          .setProperty("caseRccId").simple("${body[primaryAgencyFileId]}")
          .process(new Processor() {
            @Override
            public void process(Exchange exchange) {
              String caseRccId = (String)exchange.getProperty("caseRccId", String.class);
              if(caseRccId != null && !caseRccId.isEmpty()) {
                exchange.getMessage().setHeader("number", caseRccId);
              }
            }
          })

          .to("direct:createDocumentRecord")
        .endChoice()
        .when(simple("${header.mdoc_justin_no} != null"))
          .log(LoggingLevel.INFO,"MDOC based report")
          // need to look-up the list of rcc ids associated to the mdoc
          .to("direct:processNonStaticDocuments")
          .endChoice()
        .when(simple("${header.rcc_ids} != null"))
          .log(LoggingLevel.INFO,"rcc id list based report")
          // need to parse through the list of rcc ids
          .setBody(simple("${exchangeProperty.court_case_document}"))
          .marshal().json(JsonLibrary.Jackson, CourtCaseDocumentData.class)
          .split()
            .jsonpathWriteAsString("$.rcc_ids")
            .setProperty("rcc_id",jsonpath("$"))
            //JADE 2603 for scenario #3
            .setHeader(Exchange.HTTP_METHOD, simple("GET"))
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .setHeader("number", simple("${exchangeProperty.rcc_id}"))
            .to("http://ccm-lookup-service/getCourtCaseDetails")
            .log(LoggingLevel.DEBUG,"body : ${body}")
            .setProperty("courtcase_data", simple("${body}"))
            .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentData.class)
            .process(new Processor() {
              @Override
              public void process(Exchange exchange) {
                ChargeAssessmentData ccdd = exchange.getIn().getBody(ChargeAssessmentData.class);
                CourtCaseDocumentData cadd = (CourtCaseDocumentData)exchange.getProperty("court_case_document", CourtCaseDocumentData.class);
                DemsRecordData demsRecord = (DemsRecordData)exchange.getProperty("dems_record", DemsRecordData.class);
                if(ccdd!=null){
                  cadd.setCourt_file_no(ccdd.getAgency_file());
                  demsRecord = new DemsRecordData(cadd);
                }
                if(demsRecord != null) {
                  exchange.getMessage().setHeader("documentId", demsRecord.getDocumentId());
                  exchange.setProperty("drd", demsRecord);
                }
                exchange.getMessage().setBody(demsRecord);
              }
            })
            .marshal().json(JsonLibrary.Jackson, DemsRecordData.class)
            .log(LoggingLevel.DEBUG,"demsrecord = ${bodyAs(String)}.")
            .setBody(simple("${body}"))
            .setHeader("number", simple("${exchangeProperty.rcc_id}"))
            .setHeader("reportType", simple("${exchangeProperty.reportType}"))
            .setHeader("reportTitle", simple("${exchangeProperty.reportTitle}"))
            .setProperty("dems_record").simple("${bodyAs(String)}")
            .to("direct:changeDocumentRecord")
          .end()
          .log(LoggingLevel.INFO, "Completed parsing through list of rcc_ids")
          .endChoice()
        .otherwise()
          .log(LoggingLevel.INFO,"No identifying values, so skipped.")
          .endChoice()

      .end() // end choice
    .end() // end split
    .log(LoggingLevel.INFO, "end of processDocumentRecord")
    ;
  }

  private void processNonStaticDocuments() throws HttpOperationFailedException {
   // use method name as route id
   String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

   // IN
   // header: mdoc_justin_no or primary_rcc_id
   // property: court_case_document or image_document
   from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html

    .setHeader("number", simple("${header[mdoc_justin_no]}"))
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    //.log(LoggingLevel.INFO, "headers: ${headers}")
    .to("http://ccm-lookup-service/getCourtCaseMetadata")
    .log(LoggingLevel.DEBUG,"Retrieved Court Case Metadata from JUSTIN: ${body}")
    .setProperty("metadata_data", simple("${bodyAs(String)}"))
    .unmarshal().json(JsonLibrary.Jackson, CourtCaseData.class)
    .setProperty("CourtCaseMetadata").body()
    .process(new Processor() {
      @Override
      public void process(Exchange ex) {
        CourtCaseDocumentData ccdd = (CourtCaseDocumentData)ex.getProperty("court_case_document", CourtCaseDocumentData.class);
        ImageDocumentData id = (ImageDocumentData)ex.getProperty("image_document", ImageDocumentData.class);
        CourtCaseData cdd = ex.getIn().getBody(CourtCaseData.class);
        DemsRecordData demsRecord = null;
        if(ccdd != null) { // This is an mdoc based report
          ccdd.setCourt_file_no(cdd.getCourt_file_number_seq_type());
          // need to re-create the Dems record object, as we didn't have the Court File No before querying court file.
          demsRecord = new DemsRecordData(ccdd);
        } else if(id != null) { // This is a primary_rcc_id based report
          id.setCourt_file_number(cdd.getCourt_file_number_seq_type());
          log.info("CourtFileNumber:"+id.getCourt_file_number());
          // need to re-create the Dems record object, as we didn't have the Court File No before querying court file.
          demsRecord = new DemsRecordData(id);
        }
        ex.setProperty("reportType", demsRecord.getDescriptions());
        ex.setProperty("reportTitle", demsRecord.getTitle());
        if(demsRecord != null) {
          ex.getMessage().setHeader("documentId", demsRecord.getDocumentId());
          ex.setProperty("drd", demsRecord);
        }
        ex.getMessage().setBody(demsRecord);
      }

    })
    .marshal().json(JsonLibrary.Jackson, DemsRecordData.class)
    .setProperty("dems_record").simple("${bodyAs(String)}") // save to properties, to parse through list of records
    // 2 possible cases, this is an mdoc based record, or a primary_rcc_id one.
    // in case of mdoc, go through list of rcc_ids returned from metadata, and add file to each.
    // in case of the primary_rcc_id on, just call for that particular record.
    .choice()
      .when(simple("${header.primary_rcc_id} != null"))
        .log(LoggingLevel.INFO,"Primary RCC based report")

        .setHeader("rcc_id", simple("${header[primary_rcc_id]}"))
        .setHeader("number", simple("${header[rcc_id]}"))
        // BCPSDEMS-1141 - Send all INFORMATION docs. If there is a doc id collision, increment.
        .setProperty("maxRecordIncrements").simple("10")
        .to("direct:checkIncrementRecordDocId")
        // set back body to dems record
        .setBody(simple("${exchangeProperty.dems_record}"))
        .marshal().json(JsonLibrary.Jackson, DemsRecordData.class)
        .to("direct:createDocumentRecord")
      .endChoice()
      .otherwise()
        .log(LoggingLevel.INFO,"Traverse through metadata to retrieve the rcc_ids to process.")
        .setBody(simple("${exchangeProperty.metadata_data}"))
        .split()
          .jsonpathWriteAsString("$.related_agency_file")
          .log(LoggingLevel.INFO, "get related_agency_file rcc_id")
          .setProperty("rcc_id",jsonpath("$.rcc_id"))
          .setProperty("primary_yn",jsonpath("$.primary_yn"))
          .log(LoggingLevel.INFO, "rcc_id: ${exchangeProperty.rcc_id}")
          .choice()
            .when(simple("${exchangeProperty.primary_yn} == 'Y'")) // should only push to primary rcc.
              .setHeader("number", simple("${exchangeProperty.rcc_id}"))
              .setHeader("reportType", simple("${exchangeProperty.reportType}"))
              .setHeader("reportTitle", simple("${exchangeProperty.reportTitle}"))
              .setBody(simple("${exchangeProperty.dems_record}"))
              .marshal().json(JsonLibrary.Jackson, DemsRecordData.class)
              .to("direct:changeDocumentRecord")
            .endChoice()
            .otherwise()
              .log(LoggingLevel.INFO, "Skipped rcc, as it is not primary")
            .endChoice()
          .end()
        .end()
      .endChoice()
    .end()

   .log(LoggingLevel.INFO, "end of processNonStaticDocuments")
   ;
  }

  private void checkIncrementRecordDocId() throws HttpOperationFailedException {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN
    // header: number
    // header: documentId
    // property: dems_record
    // property: maxRecordIncrements
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    // need to look-up rcc_id if it exists in the body.
    .log(LoggingLevel.INFO,"rcc_id = ${header[number]}")
    .log(LoggingLevel.INFO,"documentId = ${header[documentId]}")
    .log(LoggingLevel.DEBUG,"Lookup message: '${body}'")

    // Just in case, if ${exchangeProperty.maxRecordIncrements} is not provided, default to 10
    .choice()
      .when(simple("${exchangeProperty.maxRecordIncrements} == null"))
        .setProperty("maxRecordIncrements").simple("10")
    .end()
    .log(LoggingLevel.INFO,"maxRecordIncrements = ${exchangeProperty.maxRecordIncrements}")

    // do a loop and keep looping until the recordId is null or blank.
    // check to see if the court case exists, before trying to insert record to dems.
    .to("direct:getCaseDocIdExistsByKey")
    .log(LoggingLevel.INFO, "returned key: ${body}")
    .unmarshal().json()
    .setProperty("recordId").simple("${body[id]}")
    .setProperty("incrementCount").simple("0")
    .log(LoggingLevel.INFO, "recordId: '${exchangeProperty.recordId}'")
    // limit the number of times incremented to 10.
    .loopDoWhile(simple("${exchangeProperty.recordId} != '' && ${exchangeProperty.incrementCount} < ${exchangeProperty.maxRecordIncrements}")) // if the recordId value is not empty

      .log(LoggingLevel.INFO, "Incrementing document id, due to collision.")
      // increment the documentId.
      .process(new Processor() {
        @Override
        public void process(Exchange ex) {
          // check to see if the record with the doc id exists, if so, increment the document id
          DemsRecordData demsRecord = (DemsRecordData)ex.getProperty("drd", DemsRecordData.class);
          log.info("Processing increment count of: "+demsRecord.getIncrementalDocCount());
          demsRecord.incrementDocumentId();

          ex.getMessage().setHeader("documentId", demsRecord.getDocumentId());
          Integer incrementCount = (Integer)ex.getProperty("incrementCount", Integer.class);
          incrementCount++;
          ex.setProperty("incrementCount", incrementCount);
          ex.setProperty("drd", demsRecord);
          ex.getMessage().setBody(demsRecord);
        }
      })
      .marshal().json(JsonLibrary.Jackson, DemsRecordData.class)
      .setProperty("dems_record").simple("${bodyAs(String)}") // save to properties, to parse through list of records

      .log(LoggingLevel.INFO, "New document id: '${header[documentId]}'")
      // now check this next value to see if there is a collision of this document
      .to("direct:getCaseDocIdExistsByKey")
      .unmarshal().json()
      .setProperty("recordId").simple("${body[id]}")
      .log(LoggingLevel.INFO, "recordId: '${exchangeProperty.recordId}'")
    .end() // end loop


    .log(LoggingLevel.INFO, "end of incrementRecordDuplicateDocId")
    ;
  }

  private void changeDocumentRecord() throws HttpOperationFailedException {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN
    // header: number
    // header: reportType
    // header: reportTitle
    // property: dems_record
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    // need to look-up rcc_id if it exists in the body.
    .log(LoggingLevel.INFO,"rcc_id = ${header[number]}")
    .log(LoggingLevel.DEBUG,"Lookup message: '${body}'")

    // look for current status of the dems case.
    .to("direct:getCourtCaseStatusByKey")
    .unmarshal().json()
    .setProperty("caseId").simple("${body[id]}")
    .setProperty("caseStatus").simple("${body[status]}")
    .setProperty("caseRccId").simple("${body[primaryAgencyFileId]}")
    .setProperty("agencyRccId").simple("${body[agencyFileId]}")
    .choice()
      .when(simple("${exchangeProperty.caseRccId} != ''"))
        .setHeader("number", simple("${exchangeProperty.caseRccId}"))
      .endChoice()
    .end()

    // check to see if the court case exists, before trying to insert record to dems.
    .to("direct:getCaseRecordExistsByKey")
    .unmarshal().json()
    .setProperty("recordId").simple("${body[id]}")
    .log(LoggingLevel.INFO, "recordId: '${exchangeProperty.recordId}'")
    .choice()
      .when(simple("${exchangeProperty.recordId} != ''"))
        //.log(LoggingLevel.INFO, "Commented-out 5.5.2 and 5.5.3 value")
        .to("direct:updateDocumentRecord")
      .endChoice()
      .otherwise()
        // BCPSDEMS-1190 - If there is a doc id collision, increment.
        .setProperty("maxRecordIncrements").simple("500")
        .to("direct:checkIncrementRecordDocId")
        // set back body to dems record
        .setBody(simple("${exchangeProperty.dems_record}"))
        .marshal().json(JsonLibrary.Jackson, DemsRecordData.class)
        .to("direct:createDocumentRecord")
      .endChoice()
    .end()

    .log(LoggingLevel.INFO, "end of changeDocumentRecord")
    ;
  }


  private void createDocumentRecord() throws HttpOperationFailedException {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN
    // property: event_object
    // property: caseFound
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    // need to look-up rcc_id if it exists in the body.
    .log(LoggingLevel.INFO,"rcc_id = ${header[number]}")
    .log(LoggingLevel.DEBUG,"Lookup message: '${body}'")
    
    .removeProperty("recordId")

    .setProperty("key", simple("${header.number}"))
    // check to see if the court case exists, before trying to insert record to dems.
    .to("direct:getCourtCaseStatusByKey")
    .unmarshal().json()
    .setProperty("caseId").simple("${body[id]}")
    .setProperty("caseStatus").simple("${body[status]}")
    .log(LoggingLevel.INFO, "caseId: '${exchangeProperty.caseId}'")
    .choice()
      .when(simple("${exchangeProperty.caseId} != '' && ${exchangeProperty.caseStatus} == 'Active'"))
        .log(LoggingLevel.INFO, "Creating document record in dems")
        .setBody(simple("${exchangeProperty.dems_record}"))

        .log(LoggingLevel.DEBUG, "dems_record: '${exchangeProperty.dems_record}'")
        .log(LoggingLevel.DEBUG,"Sending derived dems record: ${body}")

        // proceed to create record in dems, base on the caseid
        .setHeader(Exchange.HTTP_METHOD, simple("POST"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .to("direct:createCaseRecord")
        .log(LoggingLevel.DEBUG,"Created dems record: ${body}")
        .setProperty("recordId", jsonpath("$.edtId"))
        .log(LoggingLevel.INFO, "recordId: '${exchangeProperty.recordId}'")
      .endChoice()
    .end()
    .choice()
      .when(simple("${exchangeProperty.caseId} != '' && ${exchangeProperty.recordId} != null && ${exchangeProperty.recordId} != ''"))
        .log(LoggingLevel.INFO, "attempt to stream the record's content.")
        // if inserting the record to dems was successful, then go ahead and stream the data to the record.
        .process(new Processor() {
          @Override
          public void process(Exchange ex) {
            ChargeAssessmentDocumentData cadd = (ChargeAssessmentDocumentData)ex.getProperty("charge_assessment_document", ChargeAssessmentDocumentData.class);
            CourtCaseDocumentData ccdd = (CourtCaseDocumentData)ex.getProperty("court_case_document", CourtCaseDocumentData.class);
            ImageDocumentData id = (ImageDocumentData)ex.getProperty("image_document", ImageDocumentData.class);
            String data = null;
            if(cadd != null) {
              data = cadd.getData();
            } else if(ccdd != null) {
              data = ccdd.getData();
            } else if(id != null) {
              data = id.getData();
            }

            String caseId = (String)ex.getProperty("caseId", String.class);
            String recordId = (String)ex.getProperty("recordId", String.class);
            DemsRecordDocumentData demsRecordDoc = new DemsRecordDocumentData(caseId, recordId, data);
            ex.getMessage().setBody(demsRecordDoc);

          }

        })
        .marshal().json(JsonLibrary.Jackson, DemsRecordDocumentData.class)
        .log(LoggingLevel.DEBUG,"Sending derived dems record: ${body}")

        // proceed to create record in dems, base on the caseid
        .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .to("direct:streamCaseRecord")
        .endChoice()
    .end()
    .log(LoggingLevel.INFO, "end of createDocumentRecord")
    ;
  }


  private void updateDocumentRecord() throws HttpOperationFailedException {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN
    // property: event_object
    // property: caseFound
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    // need to look-up rcc_id if it exists in the body.
    .log(LoggingLevel.INFO,"rcc_id = ${header[number]}")
    .log(LoggingLevel.DEBUG,"Lookup message: '${body}'")

    .setProperty("key", simple("${header.number}"))
    // check to see if the court case exists, before trying to insert record to dems.
    .to("direct:getCourtCaseIdByKey")
    .unmarshal().json()
    .setProperty("caseId").simple("${body[id]}")
    .log(LoggingLevel.INFO, "caseId: '${exchangeProperty.caseId}'")
    .choice()
      .when(simple("${exchangeProperty.caseId} != ''"))
        .log(LoggingLevel.INFO, "Updating document record in dems")
        .setBody(simple("${exchangeProperty.dems_record}"))

        .log(LoggingLevel.DEBUG, "dems_record: '${exchangeProperty.dems_record}'")
        .log(LoggingLevel.DEBUG,"Sending derived dems record: ${body}")

        // proceed to create record in dems, base on the caseid
        .setHeader(Exchange.HTTP_METHOD, simple("POST"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .to("direct:updateCaseRecord")
        .log(LoggingLevel.DEBUG,"Created dems record: ${body}")
        .setProperty("recordId", jsonpath("$.edtId"))
        .log(LoggingLevel.INFO, "recordId: '${exchangeProperty.recordId}'")

        .endChoice()
    .end()

    .choice()
      .when(simple("${exchangeProperty.caseId} != '' && ${exchangeProperty.recordId} != null && ${exchangeProperty.recordId} != ''"))
        .log(LoggingLevel.INFO, "attempt to stream the record's content.")
        // if inserting the record to dems was successful, then go ahead and stream the data to the record.
        .process(new Processor() {
          @Override
          public void process(Exchange ex) {
            ChargeAssessmentDocumentData cadd = (ChargeAssessmentDocumentData)ex.getProperty("charge_assessment_document", ChargeAssessmentDocumentData.class);
            CourtCaseDocumentData ccdd = (CourtCaseDocumentData)ex.getProperty("court_case_document", CourtCaseDocumentData.class);
            ImageDocumentData id = (ImageDocumentData)ex.getProperty("image_document", ImageDocumentData.class);
            String data = null;
            if(cadd != null) {
              data = cadd.getData();
            } else if(ccdd != null) {
              data = ccdd.getData();
            } else if(id != null) {
              data = id.getData();
            }

            String caseId = (String)ex.getProperty("caseId", String.class);
            String recordId = (String)ex.getProperty("recordId", String.class);
            DemsRecordDocumentData demsRecordDoc = new DemsRecordDocumentData(caseId, recordId, data);
            ex.getMessage().setBody(demsRecordDoc);

          }

        })
        .marshal().json(JsonLibrary.Jackson, DemsRecordDocumentData.class)
        .log(LoggingLevel.DEBUG,"Sending derived dems record: ${body}")

        // proceed to create record in dems, base on the caseid
        .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .to("direct:streamCaseRecord")
        .endChoice()
    .end()
    .log(LoggingLevel.INFO, "end of updateDocumentRecord")
    ;
  }


  protected void version() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/v1/version?httpMethodRestrict=GET")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"version query request received")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .choice()
      .when(simple("${header.authorization} == 'Bearer {{adapter.token}}'"))
        .to("http://ccm-justin-mock-app/v1/version")
        .setProperty("version").simple("${body}")
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .setBody().simple("${exchangeProperty.version}${exchangeProperty.version}")
        .log(LoggingLevel.INFO,"Response: ${exchangeProperty.version}")
      .otherwise()
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(401))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .setBody().simple("{ \"message\": \"Authentication error.\" }")
        .log(LoggingLevel.DEBUG,"Response: ${body}")
      .end();
    // .to("http://ccm-justin-mock-app/v1/version").setBody().simple("${body}").log(LoggingLevel.DEBUG,"${body}")
  }

  private void dems_version() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/dems/v1/version?httpMethodRestrict=GET")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"DEMS version query request received")
    .choice()
      .when(simple("${header.authorization} == 'Bearer {{adapter.token}}'"))
        .removeHeader("CamelHttpUri")
        .removeHeader("CamelHttpBaseUri")
        .removeHeaders("CamelHttp*")
        .setHeader(Exchange.HTTP_METHOD, simple("GET"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
        .to("https://{{dems.host}}/version")
        .log(LoggingLevel.DEBUG,"Response: ${body}")
      .otherwise()
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(401))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .setBody().simple("{ \"message\": \"Authentication error.\" }")
        .log(LoggingLevel.DEBUG,"Response: ${body}")
      .end();
  }

  private void getDemsFieldMappings() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: exchangeProperty.id
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/fields")
    .log(LoggingLevel.INFO,"Retrieved dems field mappings.")
    ;
  }

  private void getDemsCaseFlagId() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
      .routeId(routeId)
      .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
      .log(LoggingLevel.DEBUG,"CaseFlagName = ${exchangeProperty.caseFlagName}")
      .to("direct:getDemsFieldMappings")
      .setProperty("DemsFieldMappings", simple("${bodyAs(String)}"))
      //.log(LoggingLevel.DEBUG,"Response: ${body}")
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) {
          String demsFieldMappingsJson = exchange.getProperty("DemsFieldMappings", String.class);
          String caseFlagName = exchange.getProperty("caseFlagName", String.class);
          String value = JsonParseUtils.readJsonElementKeyValue(JsonParseUtils.getJsonArrayElement(demsFieldMappingsJson, "", "/name", "Case Flags", "/listItems")
                                               , "", "/name", caseFlagName, "/id");
          exchange.setProperty("caseFlagId", value);
          System.out.println("caseFlagId:" + value);
        }

      })
      .setBody(simple("${exchangeProperty.caseFlagId}"))
      ;
  }

  private void getCourtCaseExists() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: header.number

    from("platform-http:/" + routeId)
      .routeId(routeId)
      .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
      // .log(LoggingLevel.DEBUG,"Before delay call...")
      // .delay(10000)
      // .log(LoggingLevel.DEBUG,"After delay call.")
      .setProperty("key", simple("${header.number}"))
      .log(LoggingLevel.DEBUG,"Key = ${exchangeProperty.key}")
      .to("direct:getCourtCaseIdByKey")
    ;
  }

  private void getCourtCaseIdByKey() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: exchangeProeprty.key
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"key = ${exchangeProperty.key}...")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .removeHeader("kafka.HEADERS")
    .removeHeaders("x-amz*")
    .removeHeader(Exchange.CONTENT_ENCODING) // In certain cases, the encoding was gzip, which DEMS does not support
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    //.log(LoggingLevel.INFO, "headers: ${headers}")
    .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/cases/${exchangeProperty.key}/id?throwExceptionOnFailure=false")
    //.toD("http://httpstat.us:443/500") // --> testing code, remove later
    //.toD("rest:get:org-units/{{dems.org-unit.id}}/cases/${exchangeProperty.key}/id?throwExceptionOnFailure=false&host={{dems.host}}&bindingMode=json&ssl=true")
    //.toD("netty-http:https://{{dems.host}}/org-units/{{dems.org-unit.id}}/cases/${exchangeProperty.key}/id?throwExceptionOnFailure=false")
    .log(LoggingLevel.DEBUG, "Returned case id: '${body}'")
    .doTry()
      //.log(LoggingLevel.INFO, "headers: ${headers}")
      .setProperty("length",jsonpath("$.length()"))
      .choice()
        .when(simple("${header.CamelHttpResponseCode} == 200 && ${exchangeProperty.length} > 0"))
          .setProperty("id", jsonpath("$[0].id"))
          .setBody(simple("{\"id\": \"${exchangeProperty.id}\"}"))
        .endChoice()
        .when(simple("${header.CamelHttpResponseCode} == 200"))
          .log(LoggingLevel.DEBUG,"body = '${body}'.")
          .setProperty("id", simple(""))
          .setBody(simple("{\"id\": \"\"}"))
          .setHeader("CamelHttpResponseCode", simple("200"))
          .log(LoggingLevel.INFO,"Case not found.")
        .endChoice()
        .otherwise()
          .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${header.CamelHttpResponseCode}"))

          .process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
              try {
                if(exchange != null && exchange.getMessage() != null && exchange.getMessage().getBody() != null) {
                  log.error("Returned body : " + exchange.getMessage().getBody(String.class));
                  String body = Base64.getEncoder().encodeToString(exchange.getMessage().getBody(String.class).getBytes());
                  exchange.getMessage().setBody(body);
                }
              } catch(Exception ex) {
                ex.printStackTrace();
              }
            }
          })
          .transform().simple("${body}")
          .setHeader("CCMException", simple("{\"error\": \"${header.CamelHttpResponseCode}\"}"))
          .setHeader("CCMExceptionEncoded", simple("${body}"))

          .log(LoggingLevel.ERROR,"body = '${body}'.")
        .endChoice()
      .end()
      .endDoTry()
      .doCatch(Exception.class)
        .log(LoggingLevel.INFO,"General Exception thrown.")
        .log(LoggingLevel.INFO,"${exception}")
        .process(new Processor() {
          public void process(Exchange exchange) throws Exception {

            exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, exchange.getMessage().getHeader("CamelHttpResponseCode"));
            exchange.getMessage().setBody(exchange.getException().getMessage());
            throw exchange.getException();
          }
        })
      .end()
    ;
  }

  private void getCourtCaseDataById() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: exchangeProperty.id
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"Processing request (id=${exchangeProperty.id})...")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .removeHeader(Exchange.CONTENT_ENCODING) // In certain cases, the encoding was gzip, which DEMS does not support
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.id}")
    .log(LoggingLevel.INFO,"Retrieved court case data by id.")
    ;
  }

  private void getCourtCaseDataByKey() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: exchangeProperty.key
    // OUT: JSON
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"Processing request")
    .to("direct:getCourtCaseIdByKey")
    .setProperty("id", jsonpath("$.id"))
    .to("direct:getCourtCaseDataById")
    ;
  }

  private void getCourtCaseNameByKey() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: exchangeProperty.key
    // OUT: String
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"Processing request")
    .to("direct:getCourtCaseDataByKey")
    .setProperty("courtCaseName",jsonpath("$.name"))
    .log(LoggingLevel.DEBUG,"DEMS court case name (key = ${exchangeProperty.key}): ${exchangeProperty.courtCaseName}")
    .setBody(simple("${exchangeProperty.courtCaseName}"))
    ;
  }

  private void getCourtCaseCourtFileUniqueIdByKey() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: exchangeProperty.key
    // OUT: String
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"Processing request")
    .to("direct:getCourtCaseDataByKey")
    .setProperty("DemsCourtCase", simple("${bodyAs(String)}"))
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        String courtCaseJson = exchange.getProperty("DemsCourtCase", String.class);
        String courtFileUniqueId = JsonParseUtils.getJsonArrayElementValue(courtCaseJson, "/fields", "/name", DemsFieldData.FIELD_MAPPINGS.MDOC_JUSTIN_NO.getLabel(), "/value");
        exchange.setProperty("courtFileUniqueId", courtFileUniqueId);
        String kFileValue = JsonParseUtils.readJsonElementKeyValue(JsonParseUtils.getJsonArrayElement(courtCaseJson, "/fields", "/name", "Case Flags", "/value")
                                                                     , "", "/name", "K", "/id");
        exchange.setProperty("kFileValue", kFileValue);
      }

    })
    .log(LoggingLevel.INFO,"DEMS court case name (key = ${exchangeProperty.key}): ${exchangeProperty.courtFileUniqueId}:  ${exchangeProperty.kFileValue}")
    ;
  }

  private void getCaseHyperlink() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.key
    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"Processing request.  Key = ${header.key} ...")
    .setProperty("key", simple("${header.key}"))
    .to("direct:getCourtCaseIdByKey")
    .unmarshal().json()
    .setProperty("caseId").simple("${body[id]}")
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .choice()
      .when(simple("${exchangeProperty.caseId} != ''"))
        .setProperty("hyperlinkPrefix", simple("{{dems.case.hyperlink.prefix}}"))
        .setProperty("hyperlinkSuffix", simple("{{dems.case.hyperlink.suffix}}"))
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            String prefix = exchange.getProperty("hyperlinkPrefix", String.class);
            String suffix = exchange.getProperty("hyperlinkSuffix", String.class);
            String caseId = exchange.getProperty("caseId", String.class);
            CaseHyperlinkData body = new CaseHyperlinkData();

            body.setMessage("Case found.");
            body.setHyperlink(prefix + caseId + suffix);
            exchange.getMessage().setBody(body);
          }
        })
        .log(LoggingLevel.INFO, "Case (key: ${header.key}) found; caseId: '${exchangeProperty.caseId}'")
        .endChoice()
      .otherwise()
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            CaseHyperlinkData body = new CaseHyperlinkData();
            body.setMessage("Case not found.");
            exchange.getMessage().setBody(body);
          }
        })
        .log(LoggingLevel.INFO, "Case (key: ${header.key}) not found.")
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
        .endChoice()
    .end()
    .marshal().json(JsonLibrary.Jackson, CaseHyperlinkData.class)
    ;
  }

  private void createCourtCase() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG,"Processing request: ${body}")
    .setProperty("CourtCaseMetadata", simple("${bodyAs(String)}"))
    .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentData.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        String caseTemplateId = exchange.getContext().resolvePropertyPlaceholders("{{dems.casetemplate.id}}");
        ChargeAssessmentData b = exchange.getIn().getBody(ChargeAssessmentData.class);

        DemsChargeAssessmentCaseData d = new DemsChargeAssessmentCaseData(caseTemplateId,b,b.getRelated_charge_assessments());
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsChargeAssessmentCaseData.class)
    .log(LoggingLevel.INFO,"DEMS-bound request data: '${body}'")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/cases")
    .log(LoggingLevel.INFO,"Court case created.")
    .setProperty("courtCaseId", jsonpath("$.id"))
    //jade 1747
    .log(LoggingLevel.INFO,"Call SyncCaseParticipants")
    .setProperty("ParticipantTypeFilter", simple("Accused"))
    .setProperty("Participants",simple(""))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .setBody(simple("{\"ParticipantTypeFilter\":\"${exchangeProperty.ParticipantTypeFilter}\",\"Participants\":[]}"))
    .log(LoggingLevel.DEBUG,"${body}")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.courtCaseId}/participants/sync")
    .setBody(simple("${exchangeProperty.CourtCaseMetadata}"))
    .split()
      .jsonpathWriteAsString("$.accused_persons")
      .setHeader("key", jsonpath("$.identifier"))
      .setHeader("courtCaseId").simple("${exchangeProperty.courtCaseId}")
      .log(LoggingLevel.DEBUG,"Found accused participant. Key: ${header.number}")
      .to("direct:processAccusedPerson")
    .end()
    ;
  }

  private void updateCourtCase() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"Processing request: ${body}")
    .setProperty("JustinCourtCase", simple("${bodyAs(String)}"))
    .setProperty("key", simple("${header.event_key}"))
    .to("direct:getCourtCaseCourtFileUniqueIdByKey")
    //.log(LoggingLevel.DEBUG,"Existing values: ${exchangeProperty.courtFileUniqueId} : ${exchangeProperty.kFileValue}")
    .setBody(simple("${exchangeProperty.JustinCourtCase}"))
    .setProperty("CourtCaseMetadata", simple("${bodyAs(String)}"))
    .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentData.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        String caseTemplateId = exchange.getContext().resolvePropertyPlaceholders("{{dems.casetemplate.id}}");

        // JADE-1927-  If DEMS case already exists, and is an approved court case (custom field "Court File Unique ID" is not null), the K flag will not be overridden.
        String doesCourtFileUniqueIdExist = exchange.getProperty("courtFileUniqueId", String.class);
        String doesKFilePreExist = exchange.getProperty("kFileValue", String.class);
        ChargeAssessmentData b = exchange.getIn().getBody(ChargeAssessmentData.class);
        if(doesCourtFileUniqueIdExist != null && !doesCourtFileUniqueIdExist.isEmpty()) {
          // this is an approved court case.
          if(doesKFilePreExist != null && !doesKFilePreExist.isEmpty()) {
            // dems copy of the case has k file set.
            if(!b.getCase_flags().contains("K")) {
              // new copy from justin doesn't have k set, so need to set it, to retain it.
              b.getCase_flags().add("K");
            }
          } else if(b.getCase_flags().contains("K")) {
            // dems copy of the case does not have the k file set.
            // but the justin copy has k set, so remove it.
            b.getCase_flags().remove("K");
          }

        }

        DemsChargeAssessmentCaseData d = new DemsChargeAssessmentCaseData(caseTemplateId,b,b.getRelated_charge_assessments());
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsChargeAssessmentCaseData.class)
    .log(LoggingLevel.INFO,"DEMS-bound request data: '${body}'")
    .setProperty("update_data", simple("${body}"))
    // get case id
    .setProperty("key", jsonpath("$.key"))
    .to("direct:getCourtCaseIdByKey")
    .setProperty("dems_case_id", jsonpath("$.id"))
    // update case
    .setBody(simple("${exchangeProperty.update_data}"))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .log(LoggingLevel.DEBUG,"Updating DEMS case (key = ${exchangeProperty.key}) ...")
    //JADE-2293
    .doTry()
      .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}")
      .log(LoggingLevel.INFO,"DEMS case updated.")
      //jade 1747
      .log(LoggingLevel.INFO,"Call SyncCaseParticipants")
      .setProperty("ParticipantTypeFilter", simple("Accused"))
      .setProperty("Participants",simple(""))
      .removeHeader("CamelHttpUri")
      .removeHeader("CamelHttpBaseUri")
      .removeHeaders("CamelHttp*")
      .setHeader(Exchange.HTTP_METHOD, simple("POST"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
      .setBody(simple("{\"ParticipantTypeFilter\":\"${exchangeProperty.ParticipantTypeFilter}\",\"Participants\":[]}"))
      .log(LoggingLevel.DEBUG,"${body}")
      .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/participants/sync")
      .setBody(simple("${exchangeProperty.CourtCaseMetadata}"))
      .split()
        .jsonpathWriteAsString("$.accused_persons")
        .setHeader("key", jsonpath("$.identifier"))
        .setHeader("courtCaseId").simple("${exchangeProperty.dems_case_id}")
        .log(LoggingLevel.INFO,"Updating accused participant ...")
        .log(LoggingLevel.DEBUG,"Participant key = ${header.key}")
        .to("direct:processAccusedPerson")
        .log(LoggingLevel.INFO,"Accused participant updated.")
      .end()
    .endDoTry()
    .doCatch(HttpOperationFailedException.class)
      .log(LoggingLevel.INFO,"Exception: ${exception}")
      .log(LoggingLevel.INFO,"Exchange Context: ${exchange.context}")
      .choice()
        .when().simple("${exception.statusCode} >= 400")
          .log(LoggingLevel.INFO,"Client side error.  HTTP response code = ${exception.statusCode}")
          .log(LoggingLevel.INFO, "Body: '${exception}'")
          .log(LoggingLevel.INFO, "${exception.message}")
          .process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
              try {
                HttpOperationFailedException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);

                exchange.getMessage().setBody(cause.getResponseBody());
                log.info("Returned body : " + cause.getResponseBody());
              } catch(Exception ex) {
                ex.printStackTrace();
              }
            }
          })
          .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${exception.statusCode}"))
          //.transform(exceptionMessage())
          .stop()
        .endChoice()
      .end()
    .end()
    ;
  }

  private void updateCourtCaseWithMetadata() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.rcc_id
    // IN: header.caseFlags
    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG,"Processing request: ${body}")
    .setProperty("metadata_data", simple("${bodyAs(String)}"))
    .setProperty("key", simple("${header.rcc_id}"))
    .setProperty("caseFlags", simple("${header.caseFlags}"))
    .unmarshal().json(JsonLibrary.Jackson, CourtCaseData.class)
    .setProperty("CourtCaseMetadata").body()
    // retrieve court case name from DEMS
    .to("direct:getCourtCaseNameByKey")
    .setProperty("courtCaseName",simple("${bodyAs(String)}"))
    .log(LoggingLevel.DEBUG,"getCourtCaseNameByKey: ${exchangeProperty.courtCaseName}")
    // generate DEMS court case metatdata
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        String caseFlags = exchange.getProperty("caseFlags", String.class);
        if(caseFlags != null && caseFlags.length() > 2) {
          if(caseFlags.startsWith("[")) {
            caseFlags = caseFlags.substring(1, caseFlags.length() - 1);
          }
        } else {
          caseFlags = null;
        }
        //log.error(caseFlags);
        List<String> existingCaseFlags = new ArrayList<String>();
        if(caseFlags != null) {
          existingCaseFlags = Arrays.asList(caseFlags.split(", "));
        }
        String key = exchange.getProperty("key", String.class);
        String courtCaseName = exchange.getProperty("courtCaseName", String.class);
        CourtCaseData bcm = exchange.getProperty("CourtCaseMetadata", CourtCaseData.class);
        DemsApprovedCourtCaseData d = new DemsApprovedCourtCaseData(key, courtCaseName, bcm, existingCaseFlags,bcm.getRelated_court_cases());
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsApprovedCourtCaseData.class)
    .log(LoggingLevel.INFO,"DEMS-bound request data: '${body}'")
    .setProperty("update_data", simple("${body}"))
    // get case id
    .setProperty("key", jsonpath("$.key"))
    .to("direct:getCourtCaseIdByKey")
    .setProperty("dems_case_id", jsonpath("$.id"))
    // update case
    .setBody(simple("${exchangeProperty.update_data}"))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}")
    .log(LoggingLevel.INFO,"Court case updated.")
    .log(LoggingLevel.INFO,"Create participants")
     //jade 1747
    .log(LoggingLevel.INFO,"Call SyncCaseParticipants")
    .setProperty("ParticipantTypeFilter", simple("Accused"))
    .setProperty("Participants",simple(""))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .setBody(simple("{\"ParticipantTypeFilter\":\"${exchangeProperty.ParticipantTypeFilter}\",\"Participants\":[]}"))
    .log(LoggingLevel.DEBUG,"${body}")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/participants/sync")
    .setBody(simple("${exchangeProperty.metadata_data}"))
    .split()
      .jsonpathWriteAsString("$.accused_persons")
      .setHeader("key", jsonpath("$.identifier"))
      .setHeader("courtCaseId").simple("${exchangeProperty.dems_case_id}")
      .log(LoggingLevel.DEBUG,"Found accused participant. Key: ${header.key} Case Id: ${header.courtCaseId}")
      .to("direct:processAccusedPerson")
    .end()
    ;
  }

  private void updateCourtCaseWithAppearanceSummary() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.rcc_id
    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG,"Processing request: ${body}")
    .setProperty("key", simple("${header.rcc_id}"))
    .unmarshal().json(JsonLibrary.Jackson, CaseAppearanceSummaryList.class)
    .setProperty("business_data").body()
    // retrieve court case name from DEMS
    .to("direct:getCourtCaseNameByKey")
    .setProperty("courtCaseName",simple("${bodyAs(String)}"))
    .log(LoggingLevel.DEBUG,"getCourtCaseNameByKey: ${exchangeProperty.courtCaseName}")
    // generate DEMS court case appearance summary
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        String key = exchange.getProperty("key", String.class);
        String courtCaseName = exchange.getProperty("courtCaseName", String.class);
        CaseAppearanceSummaryList b = exchange.getProperty("business_data", CaseAppearanceSummaryList.class);
        DemsCaseAppearanceSummaryData d = new DemsCaseAppearanceSummaryData(key, courtCaseName, b);
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsCaseAppearanceSummaryData.class)
    .log(LoggingLevel.DEBUG,"DEMS-bound request data: '${body}'")
    .setProperty("update_data", simple("${body}"))
    // get case id
    .setProperty("key", jsonpath("$.key"))
    .to("direct:getCourtCaseIdByKey")
    .setProperty("dems_case_id", jsonpath("$.id"))
    // update case
    .setBody(simple("${exchangeProperty.update_data}"))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}")
    .log(LoggingLevel.INFO,"Court case updated.")
    ;
  }

  private void updateCourtCaseWithCrownAssignmentData() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.rcc_id
    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"Processing request: ${body}")
    .setProperty("key", simple("${header.rcc_id}"))
    .unmarshal().json(JsonLibrary.Jackson, CaseCrownAssignmentList.class)
    .setProperty("business_data").body()
    // retrieve court case name from DEMS
    .to("direct:getCourtCaseNameByKey")
    .setProperty("courtCaseName",simple("${bodyAs(String)}"))
    .log(LoggingLevel.DEBUG,"getCourtCaseNameByKey: ${exchangeProperty.courtCaseName}")
    // generate DEMS court case crown assignment data
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        String key = exchange.getProperty("key", String.class);
        String courtCaseName = exchange.getProperty("courtCaseName", String.class);
        CaseCrownAssignmentList b = exchange.getProperty("business_data", CaseCrownAssignmentList.class);
        DemsCaseCrownAssignmentData d = new DemsCaseCrownAssignmentData(key, courtCaseName, b);
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsCaseCrownAssignmentData.class)
    .log(LoggingLevel.INFO,"DEMS-bound request data: '${body}'")
    .setProperty("update_data", simple("${body}"))
    // get case id
    .setProperty("key", jsonpath("$.key"))
    .to("direct:getCourtCaseIdByKey")
    .setProperty("dems_case_id", jsonpath("$.id"))
    // update case
    .setBody(simple("${exchangeProperty.update_data}"))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}")
    .log(LoggingLevel.INFO,"Court case updated.")
    ;
  }

  private void http_syncCaseUserList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/syncCaseUserList")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("key", simple("${header.event_key}"))
    //.setBody(simple("{\"rcc_id\":\"50433.0734\",\"auth_users_list\":[{\"part_id\":\"11429.0026\",\"crown_agency\":null,\"user_name\":null},{\"part_id\":\"85056.0734\",\"crown_agency\":null,\"user_name\":null},{\"part_id\":\"85062.0734\",\"crown_agency\":null,\"user_name\":null},{\"part_id\":\"85170.0734\",\"crown_agency\":null,\"user_name\":null}]}"))
    //.setBody(simple("${header.temp-body}"))
    //.removeHeader("temp-body")
    .log(LoggingLevel.DEBUG,"Processing request (event_key = ${exchangeProperty.key}): ${body}")
    .to("direct:syncCaseUserList");
  }

  private void syncCaseUserList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG,"Processing request: ${body}")
    .unmarshal().json(JsonLibrary.Jackson, AuthUserList.class)
    .process(new Processor() {
      public void process(Exchange exchange) {
        AuthUserList b = exchange.getIn().getBody(AuthUserList.class);
        DemsAuthUsersList da = new DemsAuthUsersList(b);
        exchange.getMessage().setBody(da);
        exchange.setProperty("auth_user_list_object", b);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsAuthUsersList.class)
    .setProperty("dems_auth_user_list").simple("${body}")
    .log(LoggingLevel.DEBUG,"DEMS-bound case users sync request data: '${body}'.")
    .setProperty("sync_data", simple("${body}"))
    // get case id
    // exchangeProperty.key already set
    .to("direct:getCourtCaseIdByKey")
    .setProperty("dems_case_id", jsonpath("$.id"))
    // sync case users
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization", simple("Bearer " + "{{dems.token}}"))
    .setBody(simple("${exchangeProperty.dems_auth_user_list}"))
    .log(LoggingLevel.INFO,"Synchronizing case users ...")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/case-users/sync")
    .log(LoggingLevel.INFO,"Case users synchronized.")
    // retrieve DEMS case group map
    .to("direct:getGroupMapByCaseId")
    // create DEMS case group members sync helper list
    .to("direct:prepareDemsCaseGroupMembersSyncHelperList")
    // sync case group members
    .to("direct:syncCaseGroupMembers")
    ;
  }

  private void prepareDemsCaseGroupMembersSyncHelperList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: exchangeProperty.auth_user_list_object
    // IN: exchangeProperty.dems_case_group_map
    // ---
    // OUT: exchangeProperty.dems_case_group_members_sync_helper_list
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .process(new Processor() {
      public void process(Exchange exchange) {
        AuthUserList userList = (AuthUserList)exchange.getProperty("auth_user_list_object");
        DemsCaseGroupMap demsCaseGroupMapForCase = (DemsCaseGroupMap)exchange.getProperty("dems_case_group_map");

        List<DemsCaseGroupMembersSyncHelper> demsGroupMembersSyncHelperList = new ArrayList<DemsCaseGroupMembersSyncHelper>();

        // create an empty DEMS case group members map with all case groups.
        HashMap<String,DemsCaseGroupMembersSyncData> demsGroupMembersMapByName = new HashMap<String,DemsCaseGroupMembersSyncData>();
        for (DemsListItemFieldData.CASE_GROUP_FIELD_MAPPINGS demsCaseGroup : DemsListItemFieldData.CASE_GROUP_FIELD_MAPPINGS.values()) {
          DemsCaseGroupMembersSyncData emptySyncData = new DemsCaseGroupMembersSyncData();
          demsGroupMembersMapByName.put(demsCaseGroup.getDems_name(),emptySyncData);
        }

        // iterate through auth user list
        for (AuthUser user : userList.getAuth_user_list()) {
          DemsListItemFieldData.CASE_GROUP_FIELD_MAPPINGS demsCaseGroupListMapping = DemsListItemFieldData.CASE_GROUP_FIELD_MAPPINGS.findCaseGroupByJustinName(user.getRole());

          String demsCaseGroupName = (demsCaseGroupListMapping == null) ? null : demsCaseGroupListMapping.getDems_name();
          Long demsCaseGroupId = (demsCaseGroupListMapping == null) ? null : demsCaseGroupMapForCase.getIdByName(demsCaseGroupName);

          if (demsCaseGroupId != null) {
            DemsCaseGroupMembersSyncData syncData = demsGroupMembersMapByName.get(demsCaseGroupName);

            // check if sync data is found
            if (syncData != null) {
              // add user to sync data
              syncData.getValues().add(user.getKey());
              // System.out.println("DEBUG: User added to sync data for DEMS group '" + demsCaseGroupName + "' (id=" + demsCaseGroupId + "), user id = " + user.getPart_id());
            }
          } else {
            // System.out.println("ERROR: Cannot add user sync data for DEMS group '" + demsCaseGroupName + "' (id=" + demsCaseGroupId + "), user id = " + user.getPart_id() + ", user JRS role = " + user.getJrs_role());
          }
        }

        for (String actualDemsCaseGroupName : demsCaseGroupMapForCase.getMap().keySet()) {
          Long actualDemsCaseGroupId = demsCaseGroupMapForCase.getIdByName(actualDemsCaseGroupName);
          DemsCaseGroupMembersSyncData syncData = demsGroupMembersMapByName.get(actualDemsCaseGroupName);
          DemsCaseGroupMembersSyncHelper helper = null;

          if (syncData != null) {
            // add sync data to helper list
            helper = new DemsCaseGroupMembersSyncHelper(actualDemsCaseGroupId, actualDemsCaseGroupName, syncData);
            // System.out.println("DEBUG: found case group: " + actualDemsCaseGroupName);
          } else {
            // add empty sync data to helper list
            DemsCaseGroupMembersSyncData emptySyncData = new DemsCaseGroupMembersSyncData();
            helper = new DemsCaseGroupMembersSyncHelper(actualDemsCaseGroupId, actualDemsCaseGroupName, emptySyncData);
          }

          demsGroupMembersSyncHelperList.add(helper);
        }
        exchange.setProperty("dems_case_group_members_sync_helper_list", demsGroupMembersSyncHelperList);
      }
    })
    ;
  }

  private void syncCaseGroupMembers() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: exchangeProperty.dems_case_id
    // IN: exchangeProperty.dems_case_group_members_sync_helper_list
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"Case group sync processing started.")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization", simple("Bearer " + "{{dems.token}}"))
    .setBody(simple("${exchangeProperty.dems_case_group_members_sync_helper_list}"))
    .marshal().json()
    .log(LoggingLevel.DEBUG,"body = '${body}'")
    .split().jsonpathWriteAsString("$")
      .setProperty("dems_case_group_name", jsonpath("$.caseGroupName"))
      .setProperty("dems_case_group_id", jsonpath("$.caseGroupId"))
      .unmarshal().json(JsonLibrary.Jackson, DemsCaseGroupMembersSyncHelper.class)
      .process(new Processor() {
        public void process(Exchange exchange) {
          DemsCaseGroupMembersSyncHelper helper = (DemsCaseGroupMembersSyncHelper)exchange.getIn().getBody();
          exchange.getMessage().setBody(helper.getSyncData());
        }
      })
      .marshal().json(JsonLibrary.Jackson, DemsCaseGroupMembersSyncData.class)
      .log(LoggingLevel.DEBUG,"Syncing case group (name='${exchangeProperty.dems_case_group_name}', id='${exchangeProperty.dems_case_group_id}'). sync data = '${body}' ...")
      .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/groups/${exchangeProperty.dems_case_group_id}/sync")
      .log(LoggingLevel.INFO,"Case group (name='${exchangeProperty.dems_case_group_name}', id='${exchangeProperty.dems_case_group_id}') members synchronized.")
      .end()
    .log(LoggingLevel.INFO,"Case group sync processing completed.")
    ;
  }

  private void processAccusedPerson() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.key
    // IN: header.courtCaseId
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG,"processAccusedPerson.  key = ${header[key]}")
    .setProperty("person_data", simple("${bodyAs(String)}"))
    .log(LoggingLevel.DEBUG,"Accused Person data = ${body}.")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("key").simple("${header.key}")
    .log(LoggingLevel.INFO,"Check whether person exists in DEMS")
    .to("direct:getPersonExists")
    .log(LoggingLevel.DEBUG,"${body}")
    .unmarshal().json()
    .setProperty("personFound").simple("${body[id]}")
    .setHeader("organizationId").jsonpath("$.orgs[0].organisationId", true)
    .setHeader("key").simple("${header.key}")
    .setHeader("courtCaseId").simple("${header.courtCaseId}")
    .setBody(simple("${exchangeProperty.person_data}"))
    .choice()
      .when(simple("${exchangeProperty.personFound} == ''"))
        .to("direct:createPerson")
      .endChoice()
      .otherwise()
        .log(LoggingLevel.DEBUG,"PersonId: ${exchangeProperty.personFound}")
        .setHeader("personId").simple("${exchangeProperty.personFound}")
        .log(LoggingLevel.DEBUG,"OrganizationId: ${header.organizationId}")
        .to("direct:updatePerson")
      .endChoice()
      .end()
    .setHeader("key").simple("${header.key}")
    .setHeader("courtCaseId").simple("${header.courtCaseId}")
    .to("direct:addParticipantToCase")
    ;
  }

  private void getPersonExists() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.key
    from("direct:" + routeId)
      .routeId(routeId)
      .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
      .setProperty("key", simple("${header.key}"))
      .to("direct:getPersonByKey")
    ;
  }

  private void getPersonByKey() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.key
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"Processing request (key=${header[key]})...")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .removeHeader(Exchange.CONTENT_ENCODING) // In certain cases, the encoding was gzip, which DEMS does not support
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/persons/${header[key]}?throwExceptionOnFailure=false")
    .choice()
      .when().simple("${header.CamelHttpResponseCode} == 200")
        // person found
        .setProperty("id", jsonpath("$.id"))
        .log(LoggingLevel.DEBUG,"Participant found. Id = ${exchangeProperty.id}")
        .endChoice()
      .when().simple("${header.CamelHttpResponseCode} == 404")
        // person not found
        .log(LoggingLevel.INFO,"Participant not found.")
        .setBody(simple("{\"id\": \"\"}"))
        .setHeader("CamelHttpResponseCode", simple("200"))
        .endChoice()
    .end()
    ;
  }

  private void createPerson() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG,"Processing request: ${body}")
    .setProperty("PersonData").body()
    .unmarshal().json(JsonLibrary.Jackson, CaseAccused.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        CaseAccused b = exchange.getIn().getBody(CaseAccused.class);
        DemsPersonData d = new DemsPersonData(b);
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsPersonData.class)
    .log(LoggingLevel.INFO,"Creating person in DEMS ...")
    .log(LoggingLevel.DEBUG,"DEMS-bound request data: '${body}'")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/persons")
    .log(LoggingLevel.INFO,"Person created.")
    ;
  }

  private void updatePerson() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.key
    // IN: header.personId
    // IN: header.organizationId
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO, "Updating person in DEMS ...")
    .log(LoggingLevel.DEBUG,"Processing request: ${body}")
    .setProperty("PersonData").body()
    .setProperty("personId").simple("${header[personId]}")
    .setProperty("organizationId").simple("${header[organizationId]}")
    .unmarshal().json(JsonLibrary.Jackson, CaseAccused.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        CaseAccused b = exchange.getIn().getBody(CaseAccused.class);
        DemsPersonData d = new DemsPersonData(b);
        String personId = exchange.getProperty("personId", String.class);
        String organizationId = exchange.getProperty("organizationId", String.class);
        d.setId(personId);
        DemsOrganisationData o = new DemsOrganisationData(organizationId);
        d.setOrgs(new ArrayList<DemsOrganisationData>());
        d.getOrgs().add(o);
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsPersonData.class)
    .log(LoggingLevel.DEBUG,"DEMS-bound request data: '${body}'")
    .setProperty("update_data", simple("${body}"))
    // update case
    .setBody(simple("${exchangeProperty.update_data}"))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/persons/${header[key]}")
    .log(LoggingLevel.INFO,"Person updated.")
    ;
  }

  private void addParticipantToCase() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.key
    // IN: header.courtCaseId
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html"{{dems.host}}
    .log(LoggingLevel.DEBUG,"Processing request: ${body}")
    .setProperty("participantType").simple("Accused")
    .setProperty("key").simple("${header.key}")
    .setProperty("courtCaseId").simple("${header.courtCaseId}")
    .log(LoggingLevel.INFO,"addParticipantToCase.  key = ${header[key]} case = ${header[courtCaseId]}")
    .choice()
      .when(simple("${header[courtCaseId]} != '' && ${header[courtCaseId]} != null"))
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) {
            String key = exchange.getProperty("key", String.class);
            String participantType = exchange.getProperty("participantType", String.class);
            DemsCaseParticipantData d = new DemsCaseParticipantData(key, participantType);
            exchange.getMessage().setBody(d);
          }
        })
        .marshal().json(JsonLibrary.Jackson, DemsCaseParticipantData.class)
        .log(LoggingLevel.DEBUG,"DEMS-bound request data: '${body}'")
        .removeHeader("CamelHttpUri")
        .removeHeader("CamelHttpBaseUri")
        .removeHeaders("CamelHttp*")
        .setHeader(Exchange.HTTP_METHOD, simple("POST"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
        .log(LoggingLevel.INFO,"Adding person to case ...")
        .toD("https://{{dems.host}}/cases/${exchangeProperty.courtCaseId}/participants")
        .log(LoggingLevel.INFO,"Person added to case.")
      .endChoice()
    .otherwise()
      .log(LoggingLevel.INFO,"Court case id was not defined. Skipped linking to a case.")
    .endChoice()
    ;
  }

  private void getGroupMapByCaseId() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: exchangeProperty.dems_case_id
    // OUT: exchangeProperty.dems_case_group_map object
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    //.log(LoggingLevel.INFO, "headers: ${headers}")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .log(LoggingLevel.INFO,"Looking up case groups (case id = ${exchangeProperty.dems_case_id}) ...")
    .removeHeader(Exchange.CONTENT_ENCODING) // In certain cases, the encoding was gzip, which DEMS does not support
    .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/groups")
    // create initial case group map
    .convertBodyTo(String.class)
    //.log(LoggingLevel.DEBUG, " Message from DEMS: ${body}")
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        DemsCaseGroupMap caseGroupMap = new DemsCaseGroupMap((String)exchange.getIn().getBody());
        exchange.setProperty("dems_case_group_map", caseGroupMap);
      }
    })
    ;
  }

  private void getCaseListByUserKey() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.key
    // OUT: body as ChargeAssessmentCaseDataRefList
    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .log(LoggingLevel.DEBUG,"Looking up case list by user key (${header.key}) ...")
    .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/users/key:${header.key}/cases?throwExceptionOnFailure=false")
    .choice()
      .when().simple("${header.CamelHttpResponseCode} == 200")
        // create initial case list
        .convertBodyTo(String.class)
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) {
            DemsCaseRefList demsCaseList = new DemsCaseRefList((String)exchange.getIn().getBody());
            ChargeAssessmentDataRefList caseList = new ChargeAssessmentDataRefList(demsCaseList);
            exchange.getMessage().setBody(caseList);
            exchange.setProperty("case_list_size", caseList.getCase_list().size());
          }
        })
        .marshal().json(JsonLibrary.Jackson, ChargeAssessmentDataRefList.class)
        .log(LoggingLevel.INFO,"User found; case list size = ${exchangeProperty.case_list_size}.")
        .endChoice()
      .when().simple("${header.CamelHttpResponseCode} == 404")
        .log(LoggingLevel.DEBUG,"User not found.  Message from DEMS: ${body}")
        .endChoice()
    .end()
    ;
  }

  private void createCaseRecord() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG,"Processing request: ${body}")
    .setProperty("DemsRecordData", simple("${bodyAs(String)}"))
    .setProperty("key", simple("${header.number}"))
    .to("direct:getCourtCaseIdByKey")
    .setProperty("dems_case_id", jsonpath("$.id"))
    // update case
    .setBody(simple("${exchangeProperty.DemsRecordData}"))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .log(LoggingLevel.INFO,"Creating DEMS case record (dems_case_id = ${exchangeProperty.dems_case_id}) ...")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/records")
    .log(LoggingLevel.INFO,"DEMS case record created.")
    .setProperty("recordId", jsonpath("$.edtId"))
    .log(LoggingLevel.DEBUG,"DEMS case record created. ${body}")
    ;
  }

  private void updateCaseRecord() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG,"Processing request: ${body}")
    .setProperty("DemsRecordData", simple("${bodyAs(String)}"))
    .setProperty("key", simple("${header.number}"))
    .to("direct:getCourtCaseIdByKey")
    .setProperty("dems_case_id", jsonpath("$.id"))
    // update case
    .setBody(simple("${exchangeProperty.DemsRecordData}"))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .log(LoggingLevel.INFO,"Creating DEMS case record (dems_case_id = ${exchangeProperty.dems_case_id}) ...")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/records/${exchangeProperty.recordId}")
    .log(LoggingLevel.INFO,"DEMS case record created.")
    .setProperty("recordId", jsonpath("$.edtId"))
    .log(LoggingLevel.DEBUG,"DEMS case record created. ${body}")
    ;
  }

  private void streamCaseRecord() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG,"Processing request: ${body}")
    .setProperty("DemsRecordDocumentData", simple("${bodyAs(String)}"))
    .setProperty("dems_case_id", jsonpath("$.caseId"))
    .setProperty("dems_record_id", jsonpath("$.recordId"))
    // decode the data element from Base64
    .log(LoggingLevel.INFO,"dems_case_id: ${exchangeProperty.dems_case_id}")
    .log(LoggingLevel.INFO,"dems_record_id: ${exchangeProperty.dems_record_id}")
    .unmarshal().json(JsonLibrary.Jackson, DemsRecordDocumentData.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception{
        DemsRecordDocumentData b = exchange.getIn().getBody(DemsRecordDocumentData.class);
        //log.info("about to decode data"+ b.getData());
        byte[] decodedBytes = Base64.getDecoder().decode(b.getData());
        exchange.getIn().setBody(decodedBytes);
        //log.info("decodedBytes" + decodedBytes);
        //log.info("decoded data");
        String fileName = "myfile.pdf";
        String boundary = "simpleboundary";
        String multipartHeader = "--" + boundary + "\r\n" + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" + "Content-Type: application/octet-stream\r\n" + "\r\n";
        String multipartFooter = "\r\n" + "--" + boundary + "--";
        byte[] headerBytes = multipartHeader.getBytes(StandardCharsets.UTF_8);
        byte[] footerBytes = multipartFooter.getBytes(StandardCharsets.UTF_8);
        byte[] multipartBody = new byte[headerBytes.length + decodedBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, multipartBody, 0, headerBytes.length);
        System.arraycopy(decodedBytes, 0, multipartBody, headerBytes.length, decodedBytes.length);
        System.arraycopy(footerBytes, 0, multipartBody, headerBytes.length + decodedBytes.length, footerBytes.length);
        exchange.getMessage().setHeader("Content-Disposition", new ValueBuilder(simple("form-data; name=\"file\"; filename=\"${header.CamelFileName}\"")));
        exchange.getMessage().setHeader("CamelHttpMethod", constant("PUT"));
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "multipart/form-data;boundary=" + boundary);
        exchange.getMessage().setBody(multipartBody);
      }
    })
    //.to("file:/tmp/output?fileName=${exchangeProperty.dems_case_id}-${exchangeProperty.dems_record_id}-jade.pdf")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .log(LoggingLevel.INFO,"Uploading DEMS case record native file (caseId = ${exchangeProperty.dems_case_id} recordId = ${exchangeProperty.dems_record_id}) ...")
    .log(LoggingLevel.DEBUG, "headers: ${headers}")
    .log(LoggingLevel.DEBUG, "body: ${body}")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/records/${exchangeProperty.dems_record_id}/Native?renditionAction=delete")
    .log(LoggingLevel.INFO,"DEMS case record native file uploaded.")
    ;
  }

  private void mergeCaseRecordsAndInactivateCase() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.sourceCaseId
    // IN: header.destinationCaseId
    // IN: header.prefixName
    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"sourceCaseId = ${header[sourceCaseId]}")
    .log(LoggingLevel.INFO,"destinationCaseId = ${header[destinationCaseId]}")
    .log(LoggingLevel.INFO,"prefixName = ${header[prefixName]}")

    // first need to check if there are any records from source case which needs to be migrated.
    
     .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/cases/${header.sourceCaseId}/records?throwExceptionOnFailure=false")
    .doTry()
      .setProperty("length",jsonpath("$.totalRows"))
      .log(LoggingLevel.INFO, "${exchangeProperty.length}")
      .choice()
        .when(simple("${header.CamelHttpResponseCode} == 200 && ${exchangeProperty.length} > 0"))
          .log(LoggingLevel.INFO, "Migrate source case document records over to destination case")
          // copy the records over to the new destination case.
          //.setBody(simple("{\"prefix\" : \"${header.prefixName}\"}"))
          .setBody(simple("{}"))
          .log(LoggingLevel.INFO, "prefixing: ${body}")
          .removeHeader("CamelHttpUri")
          .removeHeader("CamelHttpBaseUri")
          .removeHeaders("CamelHttp*")
          .setHeader(Exchange.HTTP_METHOD, simple("POST"))
          .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
          .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
          .toD("https://{{dems.host}}/cases/${header.sourceCaseId}/export-to-case/merge-case/${header.destinationCaseId}")
        .endChoice()
      .end()

      // inactivate the source case.
      .setProperty("id", simple("${header.sourceCaseId}"))
      .to("direct:getCourtCaseDataById")
      .setProperty("sourceCaseName",jsonpath("$.name"))
      .setProperty("sourceRccId",jsonpath("$.key"))

      // get dest key for setting primary agency file
      .setProperty("id", simple("${header.destinationCaseId}"))
      .to("direct:getCourtCaseStatusById")
      .setProperty("destRccId",jsonpath("$.primaryAgencyFileId"))
      .setProperty("destAgencyFile",jsonpath("$.primaryAgencyFileNo"))
      .setBody(simple("{\"name\": \"${exchangeProperty.sourceCaseName}\",\"key\": \"${exchangeProperty.sourceRccId}\",\"status\": \"Inactive\", \"fields\": [{\"name\":\"Primary Agency File ID\",\"value\":\"${exchangeProperty.destRccId}\"}, {\"name\":\"Primary Agency File No.\",\"value\":\"${exchangeProperty.destAgencyFile}\"}]}"))
      
      .removeHeader("CamelHttpUri")
      .removeHeader("CamelHttpBaseUri")
      .removeHeaders("CamelHttp*")
      .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
      .toD("https://{{dems.host}}/cases/${header.sourceCaseId}")
    .endDoTry()
    .doCatch(Exception.class)
      .log(LoggingLevel.ERROR,"Exception: ${exception}")
      .log(LoggingLevel.INFO,"Exchange Context: ${exchange.context}")
      .choice()
        .when().simple("${exception.statusCode} >= 400")
          .log(LoggingLevel.INFO,"Client side error.  HTTP response code = ${exception.statusCode}")
          .log(LoggingLevel.INFO, "Body: '${exception}'")
          .log(LoggingLevel.INFO, "${exception.message}")
          .process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
              try {
                HttpOperationFailedException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);

                exchange.getMessage().setBody(cause.getResponseBody());
                log.info("Returned body : " + cause.getResponseBody());
              } catch(Exception ex) {
                ex.printStackTrace();
              }
            }
          })
          .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${exception.statusCode}"))
        .endChoice()
      .end()
    .end()

    ;
  }

  private void getCaseRecordExistsByKey() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: header.number
    //IN: header.reportType
    //IN: header.reportTitle

    from("direct:" + routeId)
      .routeId(routeId)
      .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
      .setProperty("key", simple("${header.number}"))
      .log(LoggingLevel.INFO,"key = ${exchangeProperty.key}...")
      .to("direct:getCourtCaseIdByKey")
      .setProperty("courtCaseId", jsonpath("$.id"))
      .choice()
        .when(simple("${exchangeProperty.courtCaseId} != ''"))
          .to("direct:getCaseRecordIdByDescription")
        .endChoice()
      .end()
    ;
  }

  private void getCaseRecordIdByDescription() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: property.courtCaseId
    // IN: header.reportType
    // IN: header.reportTitle
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"courtCaseId = ${exchangeProperty.courtCaseId}...")
    .log(LoggingLevel.INFO,"reportType = ${header.reportType}...")
    //.log(LoggingLevel.INFO,"reportTitle = ${header.reportTitle}...")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    // filter on descriptions and title
    // filter-out save version of Yes, and sort any No value first.
    .toD("https://{{dems.host}}/cases/${exchangeProperty.courtCaseId}/records?filter=descriptions:\"${header.reportType}\" AND title:\"${header.reportTitle}\" AND SaveVersion:NOT Yes&fields=cc_SaveVersion,cc_OriginalFileNumber&sort=cc_SaveVersion desc")
    .log(LoggingLevel.DEBUG,"returned case records = ${body}...")

    .setProperty("length",jsonpath("$.items.length()"))
    .log(LoggingLevel.INFO, "length: ${exchangeProperty.length}")
    .choice()
      .when(simple("${header.CamelHttpResponseCode} == 200 && ${exchangeProperty.length} > 0"))
        .setProperty("id", jsonpath("$.items[0].edtID"))
        .doTry()
          .setProperty("originalFileNumber", jsonpath("$.items[0].cc_OriginalFileNumber"))
        .endDoTry()
        .doCatch(Exception.class)
          .setProperty("originalFileNumber", simple(""))
        .end()
        .doTry()
          .setProperty("saveVersion", jsonpath("$.items[0].cc_SaveVersion"))
        .endDoTry()
        .doCatch(Exception.class)
          .setProperty("saveVersion", simple(""))
        .end()
        .setBody(simple("{\"id\": \"${exchangeProperty.id}\", \"saveVersion\": \"${exchangeProperty.saveVersion}\", \"originalFileNumber\": \"${exchangeProperty.originalFileNumber}\"}"))
      .endChoice()
      .when(simple("${header.CamelHttpResponseCode} == 200"))
        .log(LoggingLevel.DEBUG,"body = '${body}'.")
        .setProperty("id", simple(""))
        .setBody(simple("{\"id\": \"\", \"saveVersion\": \"\", \"originalFileNumber\": \"\"}"))
        .setHeader("CamelHttpResponseCode", simple("200"))
        .log(LoggingLevel.INFO,"Case record not found.")
      .endChoice()
    .end()
    .log(LoggingLevel.INFO, "${body}")
    ;
  }

  private void getCaseDocIdExistsByKey() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: header.number
    //IN: header.documentId

    from("direct:" + routeId)
      .routeId(routeId)
      .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
      .setProperty("key", simple("${header.number}"))
      .log(LoggingLevel.INFO,"key = ${exchangeProperty.key}...")
      .to("direct:getCourtCaseIdByKey")
      .setProperty("courtCaseId", jsonpath("$.id"))
      .choice()
        .when(simple("${exchangeProperty.courtCaseId} != ''"))
          .to("direct:getCaseRecordIdByDocId")
        .endChoice()
      .end()
    ;
  }

  private void getCaseRecordIdByDocId() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: property.courtCaseId
    // IN: header.documentId
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"courtCaseId = ${exchangeProperty.courtCaseId}...")
    .log(LoggingLevel.INFO,"documentId = ${header.documentId}...")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    // filter on descriptions and title
    // filter-out save version of Yes, and sort any No value first.
    .toD("https://{{dems.host}}/cases/${exchangeProperty.courtCaseId}/records?filter=documentId:\"${header.documentId}\"&fields=cc_SaveVersion,cc_OriginalFileNumber")
    .log(LoggingLevel.DEBUG,"returned case records = ${body}...")

    .setProperty("length",jsonpath("$.items.length()"))
    .log(LoggingLevel.DEBUG, "length: ${exchangeProperty.length}")
    .choice()
      .when(simple("${header.CamelHttpResponseCode} == 200 && ${exchangeProperty.length} > 0"))
        .setProperty("id", jsonpath("$.items[0].edtID"))

        .doTry()
          .setProperty("originalFileNumber", jsonpath("$.items[0].cc_OriginalFileNumber"))
        .endDoTry()
        .doCatch(Exception.class)
          .setProperty("originalFileNumber", simple(""))
        .end()
        .doTry()
          .setProperty("saveVersion", jsonpath("$.items[0].cc_SaveVersion"))
        .endDoTry()
        .doCatch(Exception.class)
          .setProperty("saveVersion", simple(""))
        .end()
        .setBody(simple("{\"id\": \"${exchangeProperty.id}\", \"saveVersion\": \"${exchangeProperty.saveVersion}\", \"originalFileNumber\": \"${exchangeProperty.originalFileNumber}\"}"))

      .endChoice()
      .when(simple("${header.CamelHttpResponseCode} == 200"))
        .log(LoggingLevel.DEBUG,"body = '${body}'.")
        .setProperty("id", simple(""))
        .setBody(simple("{\"id\": \"\", \"saveVersion\": \"\", \"originalFileNumber\": \"\"}"))
        .setHeader("CamelHttpResponseCode", simple("200"))
        .log(LoggingLevel.INFO,"Case record not found.")
      .endChoice()
    .end()
    .log(LoggingLevel.INFO, "${body}")
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

  private void deleteJustinRecords() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: header.number
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"looking to inactive case id = ${header.case_id}...")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/cases/${header.case_id}/records?fields=cc_SaveVersion,cc_source")
    .log(LoggingLevel.INFO,"returned case records = ${body}...")

    .setProperty("length",jsonpath("$.items.length()"))
    .log(LoggingLevel.INFO, "length: ${exchangeProperty.length}")
    .split()
      .jsonpathWriteAsString("$.items")
      .setProperty("edtId",jsonpath("$.edtID"))
      .setProperty("recordSource",jsonpath("$.cc_Source"))
      .log(LoggingLevel.INFO,"Body: ${body}")
      .doTry()
        .choice()
          .when(simple("${exchangeProperty.recordSource} !contains 'BCPS'"))
            // As per BCPSDEMS-415, only delete the native/pdf, leave the metadata
            .process(new Processor() {
              @Override
              public void process(Exchange exchange) throws Exception{
                byte[] decodedBytes = null;
                exchange.getIn().setBody(decodedBytes);
                String fileName = "deleted.pdf";
                String boundary = "simpleboundary";
                String multipartHeader = "--" + boundary + "\r\n" + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" + "Content-Type: application/octet-stream\r\n" + "\r\n";
                String multipartFooter = "\r\n" + "--" + boundary + "--";
                byte[] headerBytes = multipartHeader.getBytes(StandardCharsets.UTF_8);
                byte[] footerBytes = multipartFooter.getBytes(StandardCharsets.UTF_8);
                byte[] multipartBody = new byte[headerBytes.length + footerBytes.length];
                System.arraycopy(headerBytes, 0, multipartBody, 0, headerBytes.length);
                System.arraycopy(footerBytes, 0, multipartBody, headerBytes.length, footerBytes.length);
                exchange.getMessage().setHeader("Content-Disposition", new ValueBuilder(simple("form-data; name=\"file\"; filename=\"${header.CamelFileName}\"")));
                exchange.getMessage().setHeader("CamelHttpMethod", constant("PUT"));
                exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "multipart/form-data;boundary=" + boundary);
                exchange.getMessage().setBody(multipartBody);
              }
            })
            .removeHeader("CamelHttpUri")
            .removeHeader("CamelHttpBaseUri")
            .removeHeaders("CamelHttp*")
            .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
            .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
            .log(LoggingLevel.INFO,"Uploading DEMS case record native file (caseId = ${header.case_id} recordId = ${exchangeProperty.edtId}) ...")
            .log(LoggingLevel.DEBUG, "headers: ${headers}")
            .log(LoggingLevel.DEBUG, "body: ${body}")
            .toD("https://{{dems.host}}/cases/${header.case_id}/records/${exchangeProperty.edtId}/Native?renditionAction=delete")
            .log(LoggingLevel.INFO,"DEMS case record native file removed.")
            .removeHeader("CamelHttpUri")
            .removeHeader("CamelHttpBaseUri")
            .removeHeaders("CamelHttp*")
            .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
            .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
            .log(LoggingLevel.INFO,"Uploading DEMS case record native file (caseId = ${header.case_id} recordId = ${exchangeProperty.edtId}) ...")
            .log(LoggingLevel.DEBUG, "headers: ${headers}")
            .log(LoggingLevel.DEBUG, "body: ${body}")
            .toD("https://{{dems.host}}/cases/${header.case_id}/records/${exchangeProperty.edtId}/Text")
            .log(LoggingLevel.INFO,"DEMS case record text file removed.")

            /*
            .removeHeader("CamelHttpUri")
            .removeHeader("CamelHttpBaseUri")
            .removeHeaders("CamelHttp*")
            .removeHeader("kafka.HEADERS")
            .removeHeaders("x-amz*")
            .setProperty("dems_case_id", simple("${header.case_id}"))
            .setHeader(Exchange.HTTP_METHOD, simple("DELETE"))
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
            .log(LoggingLevel.INFO,"Deleting DEMS case record (case_id = ${header.case_id}) ...")
            .toD("https://{{dems.host}}/cases/${header.case_id}/records/${exchangeProperty.edtId}")
             */
          .endChoice()
        .end()
      .endDoTry()
      .doCatch(Exception.class)
        .log(LoggingLevel.ERROR,"Exception: ${exception}")
        .log(LoggingLevel.INFO,"Exchange Context: ${exchange.context}")
        .choice()
          .when().simple("${exception.statusCode} >= 400")
            .log(LoggingLevel.INFO,"Client side error.  HTTP response code = ${exception.statusCode}")
            .log(LoggingLevel.INFO, "Exception: '${exception}'")
            .log(LoggingLevel.INFO, "${exception.message}")
            .log(LoggingLevel.INFO, "Body: '${body}'")
            .log(LoggingLevel.INFO, "Record not cleared")
            .process(new Processor() {
              @Override
              public void process(Exchange exchange) throws Exception {
                try {
                  HttpOperationFailedException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);

                  exchange.getMessage().setBody(cause.getResponseBody());
                  log.info("Returned body : " + cause.getResponseBody());
                } catch(Exception ex) {
                  ex.printStackTrace();
                }
              }
            })
            .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${exception.statusCode}"))
          .endChoice()
        .end()
      .end()
    .end()
    ;
  }

  private void inactivateCase() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: header.number
    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"looking to inactive case id = ${header.case_id}...")
    
    .setProperty("dems_case_id", simple("${header.case_id}"))
    
    .toD("direct:deleteJustinRecords")
    .log(LoggingLevel.INFO,"DEMS case records deleted.  Return code of ${header.CamelHttpResponseCode}")
    .doTry()
      .choice()
        .when(simple("${header.CamelHttpResponseCode} >= 200 && ${header.CamelHttpResponseCode} < 300"))
          .log(LoggingLevel.INFO, "Inactivate case")
          // inactivate the case.
          .setProperty("id", simple("${header.case_id}"))
          .to("direct:getCourtCaseStatusById")
          .setProperty("caseName",jsonpath("$.name"))
          .setProperty("rccId",jsonpath("$.key"))

          .setBody(simple("{\"name\": \"${exchangeProperty.caseName}\",\"key\": \"${exchangeProperty.rccId}\",\"status\": \"Inactive\"}"))
          //.log(LoggingLevel.INFO, "${body}")
          .removeHeader("CamelHttpUri")
          .removeHeader("CamelHttpBaseUri")
          .removeHeaders("CamelHttp*")
          .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
          .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
          .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
          .toD("https://{{dems.host}}/cases/${header.case_id}")
          .log(LoggingLevel.INFO, "Case inactivated.")
        .endChoice()
        .otherwise()
          .log(LoggingLevel.INFO, "Case lookup didn't return results.")
        .endChoice()
      .end()
    .endDoTry()
    .doCatch(Exception.class)
      .log(LoggingLevel.ERROR,"Exception: ${exception}")
      .log(LoggingLevel.INFO,"Exchange Context: ${exchange.context}")
      .choice()
        .when().simple("${exception.statusCode} >= 400")
          .log(LoggingLevel.INFO,"Client side error.  HTTP response code = ${exception.statusCode}")
          .log(LoggingLevel.INFO, "Body: '${exception}'")
          .log(LoggingLevel.INFO, "${exception.message}")
          .log(LoggingLevel.INFO, "Case not inactivated")
          .process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
              try {
                HttpOperationFailedException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);

                exchange.getMessage().setBody(cause.getResponseBody());
                log.info("Returned body : " + cause.getResponseBody());
              } catch(Exception ex) {
                ex.printStackTrace();
              }
            }
          })
          .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${exception.statusCode}"))
        .endChoice()
      .end()
   .end();
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

}