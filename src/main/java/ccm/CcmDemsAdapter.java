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
// camel-k:dependency=mvn:org.apache.camel:camel-jaxb


import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import java.nio.charset.StandardCharsets;
import org.apache.camel.support.builder.ValueBuilder;
import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.HttpHostConnectException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ccm.models.common.data.CourtCaseData;
import ccm.models.common.data.FileNote;
import ccm.models.common.event.BaseEvent;
import ccm.models.common.event.EventKPI;
import ccm.models.common.data.AuthUser;
import ccm.models.common.data.AuthUserList;
import ccm.models.common.data.CaseAccused;
import ccm.models.common.data.CaseAccusedList;
import ccm.models.common.data.CaseAppearanceSummaryList;
import ccm.models.common.data.CaseCrownAssignmentList;
import ccm.models.common.data.CaseHyperlinkData;
import ccm.models.common.data.CaseHyperlinkDataList;
import ccm.models.common.data.ChargeAssessmentData;
import ccm.models.common.data.ChargeAssessmentDataRef;
import ccm.models.common.data.ChargeAssessmentDataRefList;
import ccm.models.common.data.CommonCaseList;
import ccm.models.common.data.document.ChargeAssessmentDocumentData;
import ccm.models.common.data.document.CourtCaseDocumentData;
import ccm.models.common.data.document.ImageDocumentData;
import ccm.models.common.data.document.ReportDocument;
import ccm.models.common.event.Error;
import ccm.models.system.dems.*;
import ccm.utils.DateTimeUtils;
import ccm.utils.JsonParseUtils;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;


public class CcmDemsAdapter extends RouteBuilder {
  @Override
  public void configure() throws Exception {

    attachExceptionHandlers();
    version();
    dems_version();
    getDemsFieldMappings();
    getDemsCaseFlagId();
    getDemsFieldListIdName();
    getCourtCaseExists();
    getCourtCaseIdByKey();
    getPrimaryCourtCaseIdByKey();
    getCourtCaseDataById();
    getCourtCaseDataByKey();
    getCourtCaseNameByKey();
    getCourtCaseStatusExists();
    getCourtCaseStatusByKey ();
    getDemsFieldMappingsrccStatus();
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
    processDocumentRecord();
    processStaticDocuments();
    processNonStaticDocuments();
    checkIncrementRecordDocId();
    changeDocumentRecord();
    updateDocumentRecord();
    createDocumentRecord();
    createCaseRecord();
    updateCaseRecord();
    streamCaseRecord();
    streamCaseRecordNative();
    streamCaseRecordPdf();
    mergeCaseRecordsAndInactivateCase();
    getCaseRecordImageExistsByKey();
    getCaseRecordIdByDescriptionImageId();
    getCaseRecordExistsByKey();
    getCaseRecordIdByDescription();
    getCaseDocIdExistsByKey();
    getCaseRecordIdByDocId();
    getCaseRecordDocIdByEdtId();
    processUnknownStatus();
    publishEventKPI();
    deleteJustinRecords();
    inactivateCase();
    getCaseListHyperlink();
    reassignParticipantCases();
    checkPersonExist();
    syncAccusedPersons();
    http_syncAccusedPersons();
    deleteExistingCase();
    updateExistingParticipantwithOTCV2();
    processParticipantsList();
    updateOtcParticipants();
    destroyCaseRecords();
    activateCase();
    processNoteRecord();
    streamNoteRecord();
    processDeleteNoteRecord();
    deleteJustinFileNoteRecord();
    updateExistingCaseFileNotes();
    processCaseList();
    getPrimaryCourtCaseExists();
    inactivateActiveReturnedCases();
  }



  private void attachExceptionHandlers() {


    // handle network connectivity errors
    onException(ConnectException.class, SocketTimeoutException.class, HttpHostConnectException.class)
      .maximumRedeliveries(10).redeliveryDelay(45000)
      .log(LoggingLevel.ERROR,"onException(ConnectException, SocketTimeoutException) called.")
      .setBody(constant("An unexpected network error occurred"))
      .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("500"))
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
            kpi.setEvent_topic_partition(exchange.getProperty("kpi_event_topic_partition"));
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
            kpi.setEvent_topic_partition(exchange.getProperty("kpi_event_topic_partition"));
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
        //.log("Body: ${body}")
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
    .log(LoggingLevel.DEBUG,"status exists key = ${header[number]}...")
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

private void getDemsFieldMappingsrccStatus() {
  // use method name as route id
  String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

  // IN: exchangeProperty.id
  from("platform-http:/" + routeId)
  .routeId(routeId)
  .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
  .log(LoggingLevel.INFO,"rccstatus = ${header[rccStatus]}")
  .setProperty("rccStatus",simple("${header[rccStatus]}"))
  .removeHeader("CamelHttpUri")
  .removeHeader("CamelHttpBaseUri")
  .removeHeaders("CamelHttp*")
  .setHeader(Exchange.HTTP_METHOD, simple("GET"))
  .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
  .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
  .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/fields")
  .log(LoggingLevel.DEBUG,"Retrieved dems field mappings.")
  .setProperty("DemsFieldMappings", simple("${bodyAs(String)}"))
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        String demsFieldMappingsJson = exchange.getProperty("DemsFieldMappings", String.class);
        String rccstatus = exchange.getProperty("rccStatus", String.class);
        String value = JsonParseUtils.readJsonElementKeyValue(JsonParseUtils.getJsonArrayElement(demsFieldMappingsJson, "", "/name", "RCC Status", "/listItems")
                                             , "", "/id", rccstatus, "/name");
        exchange.setProperty("rccStatus", value);
        System.out.println("rccStatus:" + value);
      }

    })
    .setBody(simple("${exchangeProperty.rccStatus}"));
}

  private void getCourtCaseStatusById() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    //IN: exchangeProperty.id
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html

    .process(exchange -> {
      DemsCaseStatus emptyCase = new DemsCaseStatus();
      exchange.getMessage().setBody(emptyCase);
    })
    .marshal().json(JsonLibrary.Jackson, DemsCaseStatus.class)
    .setProperty("caseNotFound").simple("${bodyAs(String)}")

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
                  DemsCaseStatus caseStatus = new DemsCaseStatus();

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
                  String rccStatus = JsonParseUtils.getJsonArrayElementValue(courtCaseJson, "/fields", "/name", DemsFieldData.FIELD_MAPPINGS.RCC_STATUS.getLabel(),"/value");

                  caseStatus.setId(caseId);
                  caseStatus.setKey(caseKey);
                  caseStatus.setName(caseName);
                  caseStatus.setCaseState(caseState);
                  caseStatus.setPrimaryAgencyFileId(primaryAgencyFileId);
                  caseStatus.setPrimaryAgencyFileNo(primaryAgencyFileNo);
                  caseStatus.setAgencyFileId(agencyFileId);
                  caseStatus.setAgencyFileNo(agencyFileNo);
                  caseStatus.setCourtFileId(courtFileUniqueId);
                  caseStatus.setCourtFileNo(courtFileNo);
                  caseStatus.setStatus(status);
                  caseStatus.setRccStatus(rccStatus);

                  exchange.getMessage().setBody(caseStatus);
                  exchange.setProperty("fieldName", "RCC Status");
                  exchange.setProperty("fieldListId", rccStatus);
                }
              })
              .setProperty("caseStatusObj", body())

              // translate the rcc status with actual name.
              .to("direct:getDemsFieldListIdName")
              .log(LoggingLevel.INFO, "Returned Rcc Status: ${body}")

              .process(exchange -> {
                DemsCaseStatus caseStatus = exchange.getProperty("caseStatusObj", DemsCaseStatus.class);
                String rccStatus = exchange.getIn().getBody(String.class);
                if(rccStatus != null && !rccStatus.isEmpty()) {
                  caseStatus.setRccStatus(rccStatus);
                }

                exchange.getMessage().setBody(caseStatus);
              })
              .marshal().json(JsonLibrary.Jackson, DemsCaseStatus.class)
              .setProperty("caseStatus").simple("${bodyAs(String)}")
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
    .log(LoggingLevel.DEBUG, "DEMS Case Status: ${body}")
  ;
  }

  private void processDocumentRecord() throws HttpOperationFailedException {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN
    // property: event_object
    // property: caseFound
    from("platform-http:/" + routeId )
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    // need to look-up rcc_id if it exists in the body.
    .log(LoggingLevel.DEBUG,"event_key = ${header[event_key]}")
    .setProperty("justin_request").body()
    .setProperty("rcc_ids", simple("${headers[rcc_ids]}"))
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
    .setProperty("length",jsonpath("$.documents.length()"))
    .log(LoggingLevel.INFO, "create date: ${exchangeProperty.create_date}")

    .process(new Processor() {
      @Override
      public void process(Exchange ex) {
        ArrayList<Exception> errorList = new ArrayList<Exception>();
        ex.setProperty("errorList", errorList);
      }
    })

    // For cases like witness statement, there can be multiple docs returned.
    // This will split through each of the documents and process them individually.
    .log(LoggingLevel.INFO,"Parsing through report documents of count: ${exchangeProperty.length}")
    .split()
      .jsonpathWriteAsString("$.documents")
      .doTry()
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
        .log(LoggingLevel.INFO,"rcc_id: ${header[rcc_id]} primary_rcc_id: ${header[primary_rcc_id]} mdoc_justin_no: ${header[mdoc_justin_no]} rcc_ids: ${header[rcc_ids]} image_id: ${header[image_id]}")
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
            .to("direct:processStaticDocuments")
            .log(LoggingLevel.INFO,"End of RCC based report")
          .endChoice()
          .when(simple("${header.mdoc_justin_no} != null"))
            .log(LoggingLevel.INFO,"MDOC based report")
            // primary_rcc_id should end-up here, because we had also set the mdoc_justin_no
            // need to look-up the list of rcc ids associated to the mdoc
            .to("direct:processNonStaticDocuments")
          .endChoice()
          .when(simple("${header.rcc_ids} != null"))
            .log(LoggingLevel.INFO,"rcc id list based report: ${header.rcc_ids}")

            // Filter through rcc list and make sure it's not a duplicated primary rcc.
            .process(new Processor() {
              @Override
              public void process(Exchange exchange) {
                CourtCaseDocumentData ccdd = (CourtCaseDocumentData)exchange.getProperty("court_case_document", CourtCaseDocumentData.class);
                List<String> rccList = ccdd.getRcc_ids();
                exchange.setProperty("rcc_list", rccList);
              }
            })

            .setBody(simple("${exchangeProperty.court_case_document}"))
            .marshal().json(JsonLibrary.Jackson, CourtCaseDocumentData.class)
            .split()
              .jsonpathWriteAsString("$.rcc_ids")
              .setProperty("rcc_id",jsonpath("$"))

              .setHeader("rcc_id", simple("${exchangeProperty.rcc_id}"))
              .setHeader("number", simple("${header[rcc_id]}"))

              .doTry()
                // look for current status of the dems case, and grab the primary agency file
                .to("direct:getCourtCaseStatusByKey")
                .unmarshal().json()
                .setProperty("caseId").simple("${body[id]}")
                .setProperty("caseStatus").simple("${body[status]}")
                .setProperty("caseRccId").simple("${body[primaryAgencyFileId]}")
                .setProperty("agencyRccId").simple("${body[agencyFileId]}")
                .process(new Processor() {
                  @Override
                  public void process(Exchange exchange) {
                    CourtCaseDocumentData ccdd = (CourtCaseDocumentData)exchange.getProperty("court_case_document", CourtCaseDocumentData.class);
                    ArrayList<String> rccList = (ArrayList<String>)exchange.getProperty("rcc_list", ArrayList.class);
                    String caseId = (String)exchange.getProperty("caseId", String.class);
                    String rccId = (String)exchange.getProperty("rcc_id", String.class);
                    String caseRccId = (String)exchange.getProperty("caseRccId", String.class);

                    log.debug("comparing rcc: "+rccId + " to caseRcc: "+caseRccId);
                    if(caseRccId != null && !caseRccId.isEmpty() && !caseRccId.equalsIgnoreCase(rccId)) {
                      //log.info("rccs do not match");

                      // check if the rcc list already contains the caseRccId
                      boolean primaryFound = false;
                      for(String rcc : rccList) {
                        if(rcc.equalsIgnoreCase(caseRccId)) {
                          primaryFound = true;
                          break;
                        }
                      }

                      String removeRcc = null;
                      for(String rcc : rccList) {
                        if(rcc.equalsIgnoreCase(rccId)) {
                          removeRcc = rcc;
                          break;
                        }
                      }

                      if(primaryFound && removeRcc != null) {
                        //log.info("removing the rcc: "+ rccId);
                        rccList.remove(removeRcc);
                      }
                    } else if(caseId == null || caseId.isEmpty()) {

                      //log.info("rcc_id not found in dems:"+rccId);
                      String removeRcc = null;
                      for(String rcc : rccList) {
                        if(rcc.equalsIgnoreCase(rccId)) {
                          removeRcc = rcc;
                          break;
                        }
                      }
                      // the rcc doesn't exist in the dems environment, so remove from the list.
                      if(removeRcc != null) {
                        rccList.remove(removeRcc);
                      }
                    }
                    ccdd.setRcc_ids(rccList);
                    exchange.setProperty("court_case_document", ccdd);

                    //log.info("New rcc list size: "+rccList.size());
                  }
                })
              .endDoTry()
              .doCatch(HttpOperationFailedException.class)
                .log(LoggingLevel.ERROR,"Exception in processDocumentRecord call")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${exception.statusCode}"))
                .setHeader("CCMException", simple("${exception.statusCode}"))

                .process(new Processor() {
                  @Override
                  public void process(Exchange exchange) throws Exception {
                    try {
                      ArrayList<Exception> errorList = (ArrayList<Exception>)exchange.getProperty("errorList", ArrayList.class);
                      if(errorList == null) {
                        errorList = new ArrayList<Exception>();
                      }


                      HttpOperationFailedException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);
                      exchange.getMessage().setBody(cause.getResponseBody());

                      log.error("HttpOperationFailedException returned body : " + exchange.getMessage().getBody(String.class));

                      exchange.setProperty("exception", cause);

                      if(exchange != null && exchange.getMessage() != null && exchange.getMessage().getBody() != null) {
                        String body = Base64.getEncoder().encodeToString(exchange.getMessage().getBody(String.class).getBytes());
                        exchange.getIn().setHeader("CCMExceptionEncoded", body);
                      }

                      errorList.add(cause);
                      exchange.setProperty("errorList", errorList);
                    } catch(Exception ex) {
                      ex.printStackTrace();
                    }
                  }
                })

                .log(LoggingLevel.WARN, "Failed report: ${exchangeProperty.exception}")
                .log(LoggingLevel.ERROR,"CCMException: ${header.CCMException}")
              .end()

            .end() // end split

            .setBody(simple("${exchangeProperty.court_case_document}"))
            .marshal().json(JsonLibrary.Jackson, CourtCaseDocumentData.class)
            .split()
              .jsonpathWriteAsString("$.rcc_ids")
              .setProperty("rcc_id",jsonpath("$"))
              .log(LoggingLevel.INFO, "---- rcc record is: '${exchangeProperty.rcc_id}'")
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
            .log(LoggingLevel.ERROR,"No identifying values, so skipped.")
          .endChoice()

        .end() // end choice
      .endDoTry()
      .doCatch(HttpOperationFailedException.class)
        .log(LoggingLevel.ERROR,"Exception while processing single document")
        .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${exception.statusCode}"))
        .setHeader("CCMException", simple("${exception.statusCode}"))

        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            try {
              ArrayList<Exception> errorList = (ArrayList<Exception>)exchange.getProperty("errorList", ArrayList.class);
              if(errorList == null) {
                log.info("Had to create new arrayList");
                errorList = new ArrayList<Exception>();
              }


              HttpOperationFailedException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);
              exchange.getMessage().setBody(cause.getResponseBody());

              log.error("HttpOperationFailedException returned body : " + exchange.getMessage().getBody(String.class));

              exchange.setProperty("exception", cause);

              if(exchange != null && exchange.getMessage() != null && exchange.getMessage().getBody() != null) {
                String body = Base64.getEncoder().encodeToString(exchange.getMessage().getBody(String.class).getBytes());
                exchange.getIn().setHeader("CCMExceptionEncoded", body);
              }

              errorList.add(cause);
              exchange.setProperty("errorList", errorList);
            } catch(Exception ex) {
              ex.printStackTrace();
            }
          }
        })

        .log(LoggingLevel.WARN, "Failed report: ${exchangeProperty.exception}")
        .log(LoggingLevel.ERROR,"CCMException: ${header.CCMException}")
      .end()

    .end() // end split

    .log(LoggingLevel.INFO, "Checking for exceptions")
    .choice()
      .when(simple("${exchangeProperty.errorList} != null"))
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            ArrayList<Exception> errorList = (ArrayList<Exception>)exchange.getProperty("errorList", ArrayList.class);
            if(errorList != null && errorList.size() > 0) {
              log.info("Get first error of: "+errorList.size());
              Exception ex = (Exception)errorList.get(0);
              log.info("There is an exception");
              log.error("Exception: "+ex.getMessage());
              throw ex;
            } else {
              log.info("There is no exception");
            }
          }
        })
      .otherwise()
        .log(LoggingLevel.INFO, "No errorList found.")
    .end()

    .log(LoggingLevel.INFO, "end of processDocumentRecord")
    ;
  }

  
  private void processStaticDocuments() throws HttpOperationFailedException {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
 
    // IN
    // header: rcc_id
    // property: drd
    from("direct:" + routeId)
     .routeId(routeId)
     .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
     .choice()
        .when(simple("${exchangeProperty.caseId} != ''"))

          // IN: header.reportType
          // IN: header.reportTitle
          // IN: header.imageId
          .process(new Processor() {
            @Override
            public void process(Exchange ex) {
              DemsRecordData demsRecordObj = (DemsRecordData)ex.getProperty("drd", DemsRecordData.class);
              String courtCaseId = (String)ex.getProperty("caseId");

              ex.getMessage().setHeader("imageId", demsRecordObj.getImage_id());
              ex.getMessage().setHeader("reportType", demsRecordObj.getDescriptions());
              ex.getMessage().setHeader("reportTitle", demsRecordObj.getTitle());
              ex.getMessage().setHeader("documentId", demsRecordObj.getDocumentId());
              ex.setProperty("courtCaseId", courtCaseId);
              ex.setProperty("drd", demsRecordObj);
            }

          })

          .to("direct:getCaseRecordIdByDescriptionImageId")
          .unmarshal().json()
          .choice()
            .when(simple("${body[id]} == ''"))
              // BCPSDEMS-1141 - Send all INFORMATION docs. If there is a doc id collision, increment.
              .setProperty("maxRecordIncrements").simple("10")
              .to("direct:checkIncrementRecordDocId")
              // set back body to dems record
              .setBody(simple("${exchangeProperty.dems_record}"))
              .log(LoggingLevel.DEBUG, "${body}")
              .marshal().json(JsonLibrary.Jackson, DemsRecordData.class)
              .to("direct:createDocumentRecord")
            .endChoice()
            .otherwise()
              .log(LoggingLevel.WARN, "Skipped adding image document.  One with image id already exists.")
            .endChoice()


      .end()

     .log(LoggingLevel.INFO, "end of processStaticDocuments")
     ;
    }
  


  private void processNonStaticDocuments() throws HttpOperationFailedException {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
 
    // IN
    // header: mdoc_justin_no and/or primary_rcc_id
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
         ImageDocumentData idd = (ImageDocumentData)ex.getProperty("image_document", ImageDocumentData.class);
         CourtCaseData cdd = ex.getIn().getBody(CourtCaseData.class);
         DemsRecordData demsRecord = null;
         if(ccdd != null) { // This is an mdoc based report
           ccdd.setCourt_file_no(cdd.getCourt_file_number_seq_type());
           // need to re-create the Dems record object, as we didn't have the Court File No before querying court file.
           demsRecord = new DemsRecordData(ccdd);
         } else if(idd != null) { // This is a primary_rcc_id based report
           idd.setCourt_file_number(cdd.getCourt_file_number_seq_type());
           log.info("CourtFileNumber:"+idd.getCourt_file_number());
           // need to re-create the Dems record object, as we didn't have the Court File No before querying court file.
           demsRecord = new DemsRecordData(idd);
           ex.getMessage().setHeader("imageId", idd.getImage_id());
           log.info("imageId:"+idd.getImage_id());
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
 
         // BCPSDEMS-1401 make sure primary rcc is active
         .setHeader("rcc_id", simple("${header[primary_rcc_id]}"))
         .setHeader("number", simple("${header[rcc_id]}"))
 
         .doTry()
           // look for current status of the dems case, and grab the primary agency file
           .to("direct:getCourtCaseStatusByKey")
           .unmarshal().json()
           .setProperty("caseId").simple("${body[id]}")
           .setProperty("caseStatus").simple("${body[status]}")
           .setProperty("caseRccId").simple("${body[primaryAgencyFileId]}")
           .setProperty("agencyRccId").simple("${body[agencyFileId]}")
           .choice()
             .when(simple("${exchangeProperty.caseRccId} != ''"))
               .setHeader("rcc_id", simple("${exchangeProperty.caseRccId}"))
               .setHeader("number", simple("${exchangeProperty.caseRccId}"))
             .endChoice()
             .otherwise()
               .log(LoggingLevel.INFO, "Skipped adding image document.  One with image id already exists.")
             .endChoice()
           .end()
 
           // look-up the image id and do a check if it already exists.  If so, then skip.
           .to("direct:getCaseRecordImageExistsByKey")
           .unmarshal().json()
           .choice()
             .when(simple("${body[id]} == ''"))
               // BCPSDEMS-1141 - Send all INFORMATION docs. If there is a doc id collision, increment.
               .setProperty("maxRecordIncrements").simple("10")
               .to("direct:checkIncrementRecordDocId")
               // set back body to dems record
               .setBody(simple("${exchangeProperty.dems_record}"))
               .log(LoggingLevel.DEBUG, "${body}")
               .marshal().json(JsonLibrary.Jackson, DemsRecordData.class)
               .to("direct:createDocumentRecord")
             .endChoice()
             .otherwise()
               .log(LoggingLevel.WARN, "Skipped adding image document.  One with image id already exists.")
             .endChoice()
           .end()
         .endDoTry()
         .doCatch(HttpOperationFailedException.class)
           .log(LoggingLevel.ERROR,"Exception in processNonStaticDocuments call")
           .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${exception.statusCode}"))
           .setHeader("CCMException", simple("${exception.statusCode}"))
 
           .process(new Processor() {
             @Override
             public void process(Exchange exchange) throws Exception {
               try {
                 ArrayList<Exception> errorList = (ArrayList<Exception>)exchange.getProperty("errorList", ArrayList.class);
                 if(errorList == null) {
                   errorList = new ArrayList<Exception>();
                 }
 
 
                 HttpOperationFailedException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);
                 exchange.getMessage().setBody(cause.getResponseBody());
 
                 log.error("HttpOperationFailedException returned body : " + exchange.getMessage().getBody(String.class));
 
                 exchange.setProperty("exception", cause);
 
                 if(exchange != null && exchange.getMessage() != null && exchange.getMessage().getBody() != null) {
                   String body = Base64.getEncoder().encodeToString(exchange.getMessage().getBody(String.class).getBytes());
                   exchange.getIn().setHeader("CCMExceptionEncoded", body);
                 }
 
                 errorList.add(cause);
                 exchange.setProperty("errorList", errorList);
               } catch(Exception ex) {
                 ex.printStackTrace();
               }
             }
           })
 
           .log(LoggingLevel.WARN, "Failed report: ${exchangeProperty.exception}")
           .log(LoggingLevel.ERROR,"CCMException: ${header.CCMException}")
         .end()
         .log(LoggingLevel.INFO, "Completed primary rcc id based call.")
       .endChoice()
       .otherwise()
         .log(LoggingLevel.INFO,"Traverse through metadata to retrieve the rcc_ids to process.")
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
 
         .log(LoggingLevel.INFO, "get related_agency_file rcc_id")
         .setProperty("rcc_id",jsonpath("$.rcc_id"))
         .setProperty("primary_yn",jsonpath("$.primary_yn"))
         .log(LoggingLevel.INFO, "rcc_id: ${exchangeProperty.rcc_id} primary_yn: ${exchangeProperty.primary_yn}")
         .setHeader("number", simple("${exchangeProperty.rcc_id}"))
         .setHeader("reportType", simple("${exchangeProperty.reportType}"))
         .setHeader("reportTitle", simple("${exchangeProperty.reportTitle}"))
         .setBody(simple("${exchangeProperty.dems_record}"))
         .marshal().json(JsonLibrary.Jackson, DemsRecordData.class)
         .to("direct:changeDocumentRecord")
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
    // property: maxRecordIncrements
    // property: drd (DemsRecordData object)
    // OUT
    // property: dems_record
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

      .log(LoggingLevel.INFO, "*** Incrementing document id, due to collision.")
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
    // header: documentId
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
    // property: dems_record
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    // need to look-up rcc_id if it exists in the body.
    .log(LoggingLevel.INFO,"rcc_id = ${header[number]}")
    .log(LoggingLevel.DEBUG,"Lookup message: '${body}'")
    .log(LoggingLevel.DEBUG,"Document: '${exchangeProperty.dems_record}'")

    .removeProperty("recordId")

    .setProperty("key", simple("${header.number}"))
    // check to see if the court case exists, before trying to insert record to dems.
    .to("direct:getCourtCaseStatusByKey")
    .unmarshal().json()
    .setProperty("caseId").simple("${body[id]}")
    .setProperty("caseStatus").simple("${body[status]}")
    .log(LoggingLevel.INFO, "caseId: '${exchangeProperty.caseId}'")

    // Check to make sure that there will not be any document collision.
    .setBody(simple("${exchangeProperty.dems_record}"))
    .unmarshal().json(JsonLibrary.Jackson, DemsRecordData.class)

    .process(new Processor() {
      @Override
      public void process(Exchange ex) {
        // check to see if the record with the doc id exists, if so, increment the document id
        DemsRecordData demsRecord = (DemsRecordData)ex.getIn().getBody(DemsRecordData.class);

        ex.getMessage().setHeader("documentId", demsRecord.getDocumentId());
        log.info("DocId: " + demsRecord.getDocumentId());
      }

    })

    // now check this next value to see if there is a collision of this document
    .to("direct:getCaseDocIdExistsByKey")
    .log(LoggingLevel.INFO, "returned key: ${body}")
    .unmarshal().json()
    .setProperty("existingRecordId").simple("${body[id]}")
    .log(LoggingLevel.INFO, "existingRecordId: '${exchangeProperty.existingRecordId}'")

    // Make sure that it is an existing and active case, before attempting to add the record
    .choice()
      .when(simple("${exchangeProperty.existingRecordId} == '' && ${exchangeProperty.caseId} != '' && ${exchangeProperty.caseStatus} == 'Active'"))
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
      .otherwise()
        .log(LoggingLevel.WARN, "Did not create case record due to existing record id: ${exchangeProperty.existingRecordId}, case id: ${exchangeProperty.caseId}, or case status: ${exchangeProperty.caseStatus}")
      .endChoice()
    .end()
    .choice()
      .when(simple("${exchangeProperty.caseId} != '' && ${exchangeProperty.recordId} != null && ${exchangeProperty.recordId} != ''"))
        .log(LoggingLevel.INFO, "attempt to stream the record's content.")
        // if inserting the record to dems was successful, then go ahead and stream the data to the record.
        .process(new Processor() {
          @Override
          public void process(Exchange ex) {
            // There are potentially 3 different record object types returne from JUSTIN, find the correct one,
            // convert it to DemsRecordDocumentData and set it as the body.
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
      .when(simple("${exchangeProperty.existingRecordId} != ''"))
        .log(LoggingLevel.WARN, "Skipped new document creation, due to doc id collision with record: ${exchangeProperty.existingRecordId}")
       // .log(LoggingLevel.DEBUG, "${exchangeProperty.dems_record}")
      .endChoice()
    .end()
    .log(LoggingLevel.INFO, "end of createDocumentRecord")
    ;
  }

  private void updateDocumentRecord() throws HttpOperationFailedException {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN
    // header: number (rcc_id)
    // property: dems_record
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    // need to look-up rcc_id if it exists in the body.
    .log(LoggingLevel.INFO,"rcc_id = ${header[number]}")
    .log(LoggingLevel.DEBUG,"Lookup message: '${body}'")

    .setProperty("key", simple("${header.number}"))
    // check to see if the court case exists, before trying to insert record to dems.
    .to("direct:getCourtCaseStatusByKey")
    .unmarshal().json()
    .setProperty("caseId").simple("${body[id]}")
    .setProperty("caseStatus").simple("${body[status]}")
    .log(LoggingLevel.INFO, "caseId: '${exchangeProperty.caseId}'")
    .choice()
      .when(simple("${exchangeProperty.caseId} != '' && ${exchangeProperty.caseStatus} == 'Active'"))
        .log(LoggingLevel.INFO, "Updating document record in dems")
        .setBody(simple("${exchangeProperty.dems_record}"))

        .log(LoggingLevel.DEBUG, "dems_record: '${exchangeProperty.dems_record}'")
        .log(LoggingLevel.DEBUG,"Sending derived dems record: ${body}")

        // proceed to create record in dems, base on the caseid
        .setHeader(Exchange.HTTP_METHOD, simple("POST"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .to("direct:updateCaseRecord")
        .log(LoggingLevel.DEBUG,"Updated dems record: ${body}")
        .setProperty("recordId", jsonpath("$.edtId"))
        .log(LoggingLevel.INFO, "recordId: '${exchangeProperty.recordId}'")

      .endChoice()
      .otherwise()
        .log(LoggingLevel.WARN, "Did not create case record due to case id: ${exchangeProperty.caseId}, or case status: ${exchangeProperty.caseStatus}")
      .endChoice()
    .end()

    .choice()
      .when(simple("${exchangeProperty.caseId} != '' && ${exchangeProperty.recordId} != null && ${exchangeProperty.recordId} != ''"))
        .log(LoggingLevel.INFO, "attempt to stream the record's content.")
        // if inserting the record to dems was successful, then go ahead and stream the data to the record.
        .process(new Processor() {
          @Override
          public void process(Exchange ex) {
            // There are potentially 3 different record object types returne from JUSTIN, find the correct one,
            // convert it to DemsRecordDocumentData and set it as the body.
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
    .log(LoggingLevel.DEBUG,"Retrieved dems field mappings.")
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

  private void getDemsFieldListIdName() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: property.fieldName (i.e. "RCC Status")
    //IN: property.fieldListId (i.e. "19")
    //OUT: body (i.e. "Received")
    from("direct:" + routeId)
      .routeId(routeId)
      .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
      .log(LoggingLevel.DEBUG,"Field Name = ${exchangeProperty.fieldName} Field List Id = ${exchangeProperty.fieldListId}")
      .choice()
        .when(simple("${exchangeProperty.fieldName} != '' && ${exchangeProperty.fieldListId} != ''"))
          .to("direct:getDemsFieldMappings")
          .setProperty("DemsFieldMappings", simple("${bodyAs(String)}"))
          .log(LoggingLevel.DEBUG,"Response: ${body}")
          .process(new Processor() {
            @Override
            public void process(Exchange exchange) {
              String demsFieldMappingsJson = exchange.getProperty("DemsFieldMappings", String.class);
              String fieldName = exchange.getProperty("fieldName", String.class);
              String fieldListId = exchange.getProperty("fieldListId", String.class);
              JsonNode node = JsonParseUtils.getJsonArrayElement(demsFieldMappingsJson, "", "/name", fieldName, "/listItems");
              String name = JsonParseUtils.readJsonElementKeyValue(node, "", "/id", fieldListId, "/name");
              exchange.setProperty("fieldListName", name);
              log.debug("fieldListId: " + fieldListId + " fieldListName:" + name);
            }

          })
          .setBody(simple("${exchangeProperty.fieldListName}"))
        .endChoice()
        .otherwise()
          .log(LoggingLevel.WARN, "Field Name or Filed List Id not provided.")
          .setBody(simple(""))
        .end()
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

    // IN: exchangeProperty.key
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
    .doTry()

      .setProperty("maxRecordIncrements").simple("25")
      .setProperty("incrementCount").simple("0")
      .setProperty("continueLoop").simple("true")
      // limit the number of times incremented to 25.
      .loopDoWhile(simple("${exchangeProperty.continueLoop} == 'true' && ${exchangeProperty.key} != '' && ${exchangeProperty.key} != null && ${exchangeProperty.incrementCount} < ${exchangeProperty.maxRecordIncrements}"))
        .setHeader(Exchange.HTTP_METHOD, simple("GET"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
        //.log(LoggingLevel.INFO, "headers: ${headers}")
        .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/cases/${exchangeProperty.key}/id?throwExceptionOnFailure=false")
        //.toD("http://httpstat.us/500") // --> testing code, remove later
        .log(LoggingLevel.DEBUG, "Returned case id: '${body}'")
        //.log(LoggingLevel.INFO, "headers: ${headers}")
        .setProperty("length",jsonpath("$.length()"))

        // check if there is an override to skip looping, instead of waiting for a case to complete creation.
        .choice()
          .when(simple("${exchangeProperty.skipLoop} == 'true'"))
            .setProperty("continueLoop").simple("false")
            .log(LoggingLevel.DEBUG, "skip loop")
          .endChoice()
        .end()

        .choice()
          .when(simple("${header.CamelHttpResponseCode} == 200 && ${exchangeProperty.length} > 0"))
            .setProperty("edtCaseStatus",jsonpath("$[0].status"))
            .setProperty("edtCaseId", jsonpath("$[0].id"))
          .endChoice()
        .end()

        .log(LoggingLevel.INFO, "${exchangeProperty.key} Case Id: ${exchangeProperty.edtCaseId} Case Status: ${exchangeProperty.edtCaseStatus} Http Response: ${header.CamelHttpResponseCode}")
        .choice()
          .when(simple("${header.CamelHttpResponseCode} == 200 && ${exchangeProperty.length} == 0"))
            .setProperty("continueLoop").simple("false")
          .endChoice()
          .when(simple("${exchangeProperty.edtCaseStatus} == 'Removed'"))
            .log(LoggingLevel.WARN, "The case is removed in EDT, clear-out the returned body.")
            .setBody(simple(""))
            .setProperty("continueLoop").simple("false")

            .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("404"))
            .setHeader("CCMException", simple("{\"error\": \"Record is deleted in EDT.\"}"))
            .stop()
          .endChoice()
          .when(simple("${exchangeProperty.edtCaseStatus} == 'Offline'"))
            .log(LoggingLevel.WARN, "The case is offline in EDT, clear-out the returned body.")
            .setBody(simple(""))
            .setProperty("continueLoop").simple("false")

            .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("500"))
            .setHeader("CCMException", simple("{\"error\": \"Record is offline in EDT.\"}"))
            .stop()
          .endChoice()
          .when(simple("${exchangeProperty.edtCaseStatus} != 'Active' && ${exchangeProperty.edtCaseStatus} != 'Inactive'"))
            .log(LoggingLevel.DEBUG,"${body}")
            .log(LoggingLevel.INFO, "Case ${exchangeProperty.key} not active yet, wait 10 seconds... iteration: ${exchangeProperty.incrementCount}")
            .delay(10000)
            .log(LoggingLevel.INFO, "Retry case id retrieval... ${exchangeProperty.key}")
          .endChoice()
          .otherwise()
            .setProperty("continueLoop").simple("false")
          .endChoice()
        .end()
        // increment the loop count.
        .process(new Processor() {
          @Override
          public void process(Exchange ex) {
            Integer incrementCount = (Integer)ex.getProperty("incrementCount", Integer.class);
            incrementCount++;
            ex.setProperty("incrementCount", incrementCount);
          }
        })
      .end() // end loop

      .choice()
        .when(simple("${header.CamelHttpResponseCode} == 200 && ${exchangeProperty.length} > 0"))
          .setProperty("id", jsonpath("$[0].id"))
          .setProperty("status",jsonpath("$[0].status"))
          .setBody(simple("{\"id\": \"${exchangeProperty.id}\", \"status\": \"${exchangeProperty.status}\"}"))
        .endChoice()
        .when(simple("${header.CamelHttpResponseCode} == 200"))
          .log(LoggingLevel.DEBUG,"body = '${body}'.")
          .setProperty("id", simple(""))
          .setBody(simple("{\"id\": \"\", \"status\": \"\"}"))
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
      .log(LoggingLevel.ERROR,"General Exception thrown.")
      .log(LoggingLevel.ERROR,"${exception}")
      .log(LoggingLevel.ERROR,"${body}")
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

  private void getPrimaryCourtCaseIdByKey() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: exchangeProperty.key
    // IN: exchangeProperty.skipLoop
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"key = ${exchangeProperty.key}...")
    .doTry()
      .to("direct:getCourtCaseIdByKey")
      .setProperty("originalCaseId", simple("${body}"))
      .unmarshal().json()
      .setProperty("caseId").simple("${body[id]}")
      .setProperty("caseStatus").simple("${body[status]}")
      .choice()
        .when(simple("${exchangeProperty.caseStatus} == 'Inactive'"))
          .log(LoggingLevel.INFO, "Inactive case, lookup primary key")
          .setProperty("id", simple("${exchangeProperty.caseId}"))
          .to("direct:getCourtCaseStatusById")
          .setProperty("primaryRccId",jsonpath("$.primaryAgencyFileId"))
        .end()
      .choice()
        .when(simple("${exchangeProperty.primaryRccId} != '' && ${exchangeProperty.primaryRccId} != null && ${exchangeProperty.primaryRccId} != ${exchangeProperty.key}"))
          .setProperty("key", simple("${exchangeProperty.primaryRccId}"))
          .to("direct:getCourtCaseIdByKey")
        .endChoice()
        .otherwise()
          .setBody(simple("${exchangeProperty.originalCaseId}"))
        .endChoice()
    .endDoTry()
    .doCatch(Exception.class)
      .log(LoggingLevel.ERROR,"General Exception thrown.")
      .log(LoggingLevel.ERROR,"${exception}")
      .log(LoggingLevel.ERROR,"${body}")
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

  private void getPrimaryCourtCaseExists() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: header.number

    from("platform-http:/" + routeId)
      .routeId(routeId)
      .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
      // .log(LoggingLevel.DEBUG,"Before delay call...")
      // .delay(10000)
      // .log(LoggingLevel.DEBUG,"After delay call.")
      .log(LoggingLevel.INFO,"Processing request.  Key = ${header.key} ...")
      .setProperty("key", simple("${header.key}"))
      .to("direct:getPrimaryCourtCaseIdByKey")
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
    .doTry()

      .setProperty("maxRecordIncrements").simple("25")
      .setProperty("incrementCount").simple("0")
      .setProperty("continueLoop").simple("true")
      // limit the number of times incremented to 25.
      .loopDoWhile(simple("${exchangeProperty.continueLoop} == 'true' && ${exchangeProperty.id} != '' && ${exchangeProperty.id} != null && ${exchangeProperty.incrementCount} < ${exchangeProperty.maxRecordIncrements}"))

        .removeHeader("CamelHttpUri")
        .removeHeader("CamelHttpBaseUri")
        .removeHeaders("CamelHttp*")
        .removeHeader(Exchange.CONTENT_ENCODING) // In certain cases, the encoding was gzip, which DEMS does not support
        .setHeader(Exchange.HTTP_METHOD, simple("GET"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
        .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/cases/info/${exchangeProperty.id}")
        .log(LoggingLevel.DEBUG,"Retrieved court case data by id.")

        .setProperty("edtCaseStatus",jsonpath("$.status"))
        .log(LoggingLevel.INFO, "${exchangeProperty.id} Case Status: ${exchangeProperty.edtCaseStatus}")
        .choice()
          .when(simple("${exchangeProperty.edtCaseStatus} == 'Removed'"))
            .log(LoggingLevel.WARN, "The case is removed in EDT, clear-out the returned body.")
            .setBody(simple(""))
            .setProperty("continueLoop").simple("false")

            .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("404"))
            .setHeader("CCMException", simple("{\"error\": \"Record is deleted in EDT.\"}"))
            .stop()
          .endChoice()
          .when(simple("${exchangeProperty.edtCaseStatus} == 'Offline'"))
            .log(LoggingLevel.WARN, "The case is offline in EDT, clear-out the returned body.")
            .setBody(simple(""))
            .setProperty("continueLoop").simple("false")

            .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("500"))
            .setHeader("CCMException", simple("{\"error\": \"Record is offline in EDT.\"}"))
            .stop()
          .endChoice()
          .when(simple("${exchangeProperty.edtCaseStatus} != 'Active' && ${exchangeProperty.edtCaseStatus} != 'Inactive'"))
            .log(LoggingLevel.DEBUG,"${body}")
            .log(LoggingLevel.INFO, "Case ${exchangeProperty.id} not active yet, wait 10 seconds... iteration: ${exchangeProperty.incrementCount}")
            .delay(10000)
            .log(LoggingLevel.INFO, "Retry case data retrieval... ${exchangeProperty.id}")
          .endChoice()
          .otherwise()
            .setProperty("continueLoop").simple("false")
          .endChoice()
        .end()
        // increment the loop count.
        .process(new Processor() {
          @Override
          public void process(Exchange ex) {
            Integer incrementCount = (Integer)ex.getProperty("incrementCount", Integer.class);
            incrementCount++;
            ex.setProperty("incrementCount", incrementCount);
          }
        })

      .end() // end loop

      .choice()
        .when(simple("${exchangeProperty.edtCaseStatus} == 'Queued'"))
          .log(LoggingLevel.ERROR, "Court case... ${exchangeProperty.id} possibly stuck in queue.")
          .setBody(simple(""))
          .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("500"))
          .setHeader("CCMException", simple("{\"error\": \"Case id ${exchangeProperty.id} possibly stuck in queued state.\"}"))
          .stop()
        .endChoice()
      .end()

    .endDoTry()
    .doCatch(HttpOperationFailedException.class)
      // sometimes, if events come in a little too fast for the same case, it may cause an error
      // wait 25 seconds then try again.
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
          HttpOperationFailedException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);

          log.error("Returned status code : " + cause.getStatusCode());
          log.error("Response body : " + cause.getResponseBody());
        }
      })
      .delay(25000)
      .log(LoggingLevel.WARN,"Re-attempting to retrieve case data (id=${exchangeProperty.id})...")
      .removeHeader("CamelHttpUri")
      .removeHeader("CamelHttpBaseUri")
      .removeHeaders("CamelHttp*")
      .removeHeader(Exchange.CONTENT_ENCODING) // In certain cases, the encoding was gzip, which DEMS does not support
      .setHeader(Exchange.HTTP_METHOD, simple("GET"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
      .toD("https://{{dems.host}}/cases/${exchangeProperty.id}")
      .log(LoggingLevel.INFO,"Retrieved court case data by id.")
    .end()
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
    .log(LoggingLevel.INFO,"Processing request ${exchangeProperty.key}")
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
    .log(LoggingLevel.INFO,"Processing request ${exchangeProperty.key}")
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
    .log(LoggingLevel.INFO,"Processing request ${exchangeProperty.key}")
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
    .log(LoggingLevel.DEBUG,"Processing request.  Key = ${header.key} ...")
    .setProperty("key", simple("${header.key}"))
    .setProperty("skipLoop", simple("true"))
    .to("direct:getPrimaryCourtCaseIdByKey")
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
            String rccId = exchange.getProperty("key", String.class);
            CaseHyperlinkData body = new CaseHyperlinkData();

            body.setMessage("Case found.");
            body.setHyperlink(prefix + caseId + suffix);
            body.setRcc_id(rccId);
            exchange.getMessage().setBody(body);
          }
        })
        .log(LoggingLevel.DEBUG, "Case (key: ${header.key}) found; caseId: '${exchangeProperty.caseId}'")
        .endChoice()
      .otherwise()
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            CaseHyperlinkData body = new CaseHyperlinkData();
            String rccId = exchange.getProperty("key", String.class);
            body.setMessage("Case not found.");
            body.setRcc_id(rccId);
            exchange.getMessage().setBody(body);
          }
        })
        .log(LoggingLevel.DEBUG, "Case (key: ${header.key}) not found.")
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
        .endChoice()
    .end()
    .marshal().json(JsonLibrary.Jackson, CaseHyperlinkData.class)
    ;
  }

  //as part of jade 2425
  private void getCaseListHyperlink() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"Processing request.  Key = ${body} ...")
    .setProperty("key", simple("${body}"))

    .unmarshal().json(JsonLibrary.Jackson, CommonCaseList.class)
    // set the hyperlink object to be returned in the body at end
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        CommonCaseList data = exchange.getMessage().getBody(CommonCaseList.class);
        CaseHyperlinkDataList hyperlinkObject = new CaseHyperlinkDataList(data);
        exchange.setProperty("metadata_object", hyperlinkObject);
      }
    })

    .marshal().json(JsonLibrary.Jackson, CaseHyperlinkDataList.class)
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .setBody(simple("${exchangeProperty.key}"))
    .log(LoggingLevel.DEBUG,"rcc_ids: ${exchangeProperty.key}")
    .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/cases/lookup-ids")
    .log(LoggingLevel.DEBUG, "Returned body from lookup id: '${body}'")
    .log(LoggingLevel.DEBUG,"rcc_ids: ${exchangeProperty.key}")

    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        List<DemsLookupSearchData> items = new ArrayList<DemsLookupSearchData>();
        exchange.setProperty("lookup_results", items);
      }
    })

    .setProperty("length",jsonpath("$.length()"))
    .choice()
      .when(simple("${header.CamelHttpResponseCode} == 200 && ${exchangeProperty.length} > 0"))
        // Loop through list and update the primary id
        .split()
          .jsonpathWriteAsString("$.*")
          .setProperty("caseId",jsonpath("$.id"))
          .setProperty("caseKey",jsonpath("$.key"))
          .setProperty("caseStatus",jsonpath("$.status"))

          .choice()
            .when(simple("${exchangeProperty.caseStatus} == 'Inactive'"))

              .setProperty("key", simple("${exchangeProperty.caseKey}"))
              .setProperty("skipLoop", simple("true"))
              .to("direct:getPrimaryCourtCaseIdByKey")
              .unmarshal().json()
              .setProperty("caseId").simple("${body[id]}")
            .endChoice()
          .end()

          .process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
              Integer id = exchange.getProperty("caseId", Integer.class);
              String key = exchange.getProperty("caseKey", String.class);
              String status = exchange.getProperty("caseStatus", String.class);
              DemsLookupSearchData item = new DemsLookupSearchData(id, key, status);
              List<DemsLookupSearchData> items = (List<DemsLookupSearchData>)exchange.getProperty("lookup_results", ArrayList.class);
              items.add(item);
              exchange.setProperty("lookup_results", items);
            }
          })

        .end()
      .endChoice()
      .when(simple("${header.CamelHttpResponseCode} != 200"))
        .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${header.CamelHttpResponseCode}"))
        .stop()
      .endChoice()
    .end()

    .choice()
      .when().simple("${header.CamelHttpResponseCode} == 200")
        //.unmarshal(new ListJacksonDataFormat(DemsLookupSearchData.class))
        .unmarshal().json(JsonLibrary.Jackson, List.class)
        .setProperty("hyperlinkPrefix", simple("{{dems.case.hyperlink.prefix}}"))
        .setProperty("hyperlinkSuffix", simple("{{dems.case.hyperlink.list.suffix}}"))
        .setProperty("caseIds").simple("${body}")
        .log(LoggingLevel.DEBUG,"case ids: ${exchangeProperty.caseIds}")
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            List<DemsLookupSearchData> items = (List<DemsLookupSearchData>)exchange.getProperty("lookup_results", ArrayList.class);
            CaseHyperlinkDataList metadata = (CaseHyperlinkDataList)exchange.getProperty("metadata_object", CaseHyperlinkDataList.class);
            String prefix = exchange.getProperty("hyperlinkPrefix", String.class);
            String suffix = exchange.getProperty("hyperlinkSuffix", String.class);
            //log.info("originalList size: "+metadata.getcase_hyperlinks().size());
            metadata.processCaseHyperlinks(prefix, suffix, items);
            //log.info("postprocessList size: "+metadata.getcase_hyperlinks().size());
            //for(CaseHyperlinkData data : metadata.getcase_hyperlinks()) {
            //  log.info("RCC: " + data.getRcc_id() + " " +data.getHyperlink());
            //}

            exchange.setProperty("metadata_object", metadata);
          }
        })
        .setBody(simple("${exchangeProperty.metadata_object}"))
        .marshal().json(JsonLibrary.Jackson, CaseHyperlinkDataList.class)
      .endChoice()
    .end()

    .log(LoggingLevel.DEBUG, "Final body: ${body}")
    ;
  }

  private void createCourtCase() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"createCourtCase ${header.event_key}")
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
    .doTry()
      .log(LoggingLevel.DEBUG,"DEMS-bound request data: '${body}'")
      .removeHeader("CamelHttpUri")
      .removeHeader("CamelHttpBaseUri")
      .removeHeaders("CamelHttp*")
      .setHeader(Exchange.HTTP_METHOD, simple("POST"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
      .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/cases")
      .log(LoggingLevel.INFO,"Court case was created.")
      //.toD("http://httpstat.us/504")
      .setProperty("courtCaseId", jsonpath("$.id"))
      .log(LoggingLevel.INFO, "New case id: ${exchangeProperty.courtCaseId}")
      .setProperty("id", simple("${exchangeProperty.courtCaseId}"))
      .setProperty("key", simple("${header.event_key}"))
      .to("direct:getCourtCaseDataByKey")
      //.delay(15000)

    .endDoTry()
    .doCatch(HttpOperationFailedException.class)
      .log(LoggingLevel.ERROR,"Exception: ${exception}")
      .log(LoggingLevel.ERROR,"Exchange Context: ${exchange.context}")
      .choice()
        .when().simple("${exception.statusCode} == 504")
          .log(LoggingLevel.ERROR, "Encountered timeout for rcc: ${header.event_key}.  Wait additional 65 seconds to continue.")
           // Sometimes EDT takes longer to create a case than their 30 second gateway timeout, so add a delay and continue on.
          .delay(65000)
          .setHeader(Exchange.HTTP_METHOD, simple("GET"))
          .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
          .setProperty("key", simple("${header.event_key}"))
          .to("direct:getCourtCaseIdByKey")

          .setProperty("courtCaseId", jsonpath("$.id"))
          .setProperty("id", simple("${exchangeProperty.courtCaseId}"))
          .to("direct:getCourtCaseDataById")
        .endChoice()
        .when().simple("${exception.statusCode} >= 400")
          .log(LoggingLevel.ERROR,"Client side error.  HTTP response code = ${exception.statusCode}")
          .log(LoggingLevel.ERROR, "Body: '${exception}'")
          .log(LoggingLevel.ERROR, "${exception.message}")
          .process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
              try {
                HttpOperationFailedException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);

                exchange.getMessage().setBody(cause.getResponseBody());
                log.info("Returned response body : " + cause.getResponseBody());
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

  private void updateCourtCase() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG,"Processing request: ${body}")
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
        d.setWaitForCaseCompletion(true);
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsChargeAssessmentCaseData.class)
    .log(LoggingLevel.DEBUG,"DEMS-bound request data: '${body}'")
    .setProperty("update_data", simple("${body}"))
    // get case id
    .setProperty("key", jsonpath("$.key"))
    .to("direct:getCourtCaseIdByKey")
    .setProperty("dems_case_id", jsonpath("$.id"))

    //JADE-2293
    .doTry()
      // update case
      .setBody(simple("${exchangeProperty.update_data}"))
      .removeHeader("CamelHttpUri")
      .removeHeader("CamelHttpBaseUri")
      .removeHeaders("CamelHttp*")
      .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
      .log(LoggingLevel.INFO,"Updating DEMS case (key = ${exchangeProperty.key}) ...")
      .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}")
      .log(LoggingLevel.INFO,"DEMS case updated.")
      //.toD("http://httpstat.us/500")

    .endDoTry()
    .doCatch(HttpOperationFailedException.class)
      .log(LoggingLevel.ERROR,"Exception: ${exception}")
      .log(LoggingLevel.ERROR,"Exchange Context: ${exchange.context}")
      .choice()
        .when().simple("${exception.statusCode} == 504")
          .log(LoggingLevel.ERROR, "Encountered timeout.  Wait additional 30 seconds to continue.")
           // Sometimes EDT takes longer to create a case than their 30 second gateway timeout, so add a delay and continue on.
          .delay(30000)
        .endChoice()
        .when().simple("${exception.statusCode} >= 400")
          .log(LoggingLevel.ERROR,"Client side error.  HTTP response code = ${exception.statusCode}")
          .log(LoggingLevel.ERROR, "Body: '${exception}'")
          .log(LoggingLevel.ERROR, "${exception.message}")
          .process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
              try {
                HttpOperationFailedException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);

                exchange.getMessage().setBody(cause.getResponseBody());
                log.info("Returned response body : " + cause.getResponseBody());
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
    .log(LoggingLevel.DEBUG,"DEMS-bound request data: '${body}'")
    .setProperty("update_data", simple("${body}"))
    // get case id
    .setProperty("key", jsonpath("$.key"))
    .to("direct:getCourtCaseIdByKey")
    .setProperty("dems_case_id", jsonpath("$.id"))
    .doTry()
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
      //.toD("http://httpstat.us/504")

    .endDoTry()
    .doCatch(HttpOperationFailedException.class)
      .log(LoggingLevel.ERROR,"Exception: ${exception}")
      .log(LoggingLevel.ERROR,"Exchange Context: ${exchange.context}")
      .choice()
        .when().simple("${exception.statusCode} == 504")
          .log(LoggingLevel.ERROR, "Encountered timeout.  Wait additional 15 seconds to continue.")
           // Sometimes EDT takes longer to create a case than their 30 second gateway timeout, so add a delay and continue on.
          .delay(15000)
        .endChoice()
        .when().simple("${exception.statusCode} >= 400")
          .log(LoggingLevel.ERROR,"Client side error.  HTTP response code = ${exception.statusCode}")
          .log(LoggingLevel.ERROR, "Body: '${exception}'")
          .log(LoggingLevel.ERROR, "${exception.message}")
          .process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
              try {
                HttpOperationFailedException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);

                exchange.getMessage().setBody(cause.getResponseBody());
                log.info("Returned response body : " + cause.getResponseBody());
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

  private void updateCourtCaseWithAppearanceSummary() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.rcc_id
    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG,"Processing request: ${body}")
    .setProperty("key", simple("${header.rcc_id}"))
    .log(LoggingLevel.INFO, "updateCourtCaseWithAppearanceSummary for rcc: ${header.rcc_id}")
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
    .log(LoggingLevel.DEBUG,"Processing request: ${body}")
    .setProperty("key", simple("${header.rcc_id}"))
    .log(LoggingLevel.INFO, "updateCourtCaseWithCrownAssignmentData for rcc: ${header.rcc_id}")
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
    .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/users/sync")
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
    .log(LoggingLevel.INFO,"processAccusedPerson.  key = ${header[key]}")
    .setProperty("person_data", simple("${bodyAs(String)}"))
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("key").simple("${header.key}")
    .log(LoggingLevel.DEBUG,"Check whether person exists in DEMS")
    .to("direct:getPersonExists")
    .log(LoggingLevel.DEBUG,"Person exist : ${body}")
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
        .log(LoggingLevel.INFO,"PersonId: ${exchangeProperty.personFound}")
        .setHeader("personId").simple("${exchangeProperty.personFound}")
        .log(LoggingLevel.DEBUG,"OrganizationId: ${header.organizationId}")
        .log(LoggingLevel.DEBUG,"${body}")
        .to("direct:updatePerson")
      .endChoice()
    .end()
    .setHeader("key").simple("${header.key}")
    .setHeader("courtCaseId").simple("${header.courtCaseId}")
    .to("direct:addParticipantToCase")
    ;
  }

  //jade 1750 ... search if both FROM and TO part ids exist in the DEMS system
  private void checkPersonExist() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.key
    from("platform-http:/" + routeId)
      .routeId(routeId)
      .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
      .setProperty("key", simple("${header.key}"))
      .to("direct:getPersonByKey")
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
        d.generateOTC();
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
        d.setId(personId);
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsPersonData.class)
    .log(LoggingLevel.DEBUG,"DEMS-bound request data: '${body}'")
    .setProperty("update_data", simple("${body}"))
    // update case
    .doTry()
      .setBody(simple("${exchangeProperty.update_data}"))
      .removeHeader("CamelHttpUri")
      .removeHeader("CamelHttpBaseUri")
      .removeHeaders("CamelHttp*")
      .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
      .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/persons/${header[personId]}")
      .log(LoggingLevel.INFO,"Person updated.")
    .endDoTry()
    .doCatch(Exception.class)
      .log(LoggingLevel.ERROR,"Exception: ${exception}")
      .log(LoggingLevel.INFO,"Exchange Context: ${exchange.context}")
      .log(LoggingLevel.WARN,"Initial person update failed, pausing to retry.")
      // Wait 10 seconds to retry updating the person record.
      .delay(10000)
      .setBody(simple("${exchangeProperty.update_data}"))
      .removeHeader("CamelHttpUri")
      .removeHeader("CamelHttpBaseUri")
      .removeHeaders("CamelHttp*")
      .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
      .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/persons/${header[personId]}")
      .log(LoggingLevel.INFO,"Retry of person updated.")
    .end()

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
        .removeHeader(Exchange.CONTENT_ENCODING) // In certain cases, the encoding was gzip, which DEMS does not support
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
    .log(LoggingLevel.DEBUG,"Processing request: " + simple("${body}"))
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
    .delay(1500)
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
    .setProperty("courtCaseId", jsonpath("$.id"))
    // try to look-up the docId, to prevent doc collision issue.
    .removeProperty("existingDocumentId")
    .to("direct:getCaseRecordDocIdByEdtId")
    .doTry()
      .setProperty("existingDocumentId", jsonpath("$.documentID"))
    .endDoTry()
    .doCatch(Exception.class)
      .log(LoggingLevel.ERROR,"Exception: ${exception}")
      .log(LoggingLevel.INFO,"Exchange Context: ${exchange.context}")
    .end()
    // update case
    .setBody(simple("${exchangeProperty.DemsRecordData}"))
    .unmarshal().json(JsonLibrary.Jackson, DemsRecordData.class)

    .process(new Processor() {
      @Override
      public void process(Exchange ex) {
        // check to see if the record with the doc id exists, if so, increment the document id
        DemsRecordData demsRecord = (DemsRecordData)ex.getIn().getBody(DemsRecordData.class);
        log.info("docId:"+ demsRecord.getDocumentId());
        String existingDocId = (String)ex.getProperty("existingDocumentId");
        log.info("existingDocId:"+ existingDocId);
        if(existingDocId != null && !existingDocId.isEmpty()) {
          demsRecord.updateDocumentId(existingDocId);
        }
        ex.getMessage().setBody(demsRecord);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsRecordData.class)
    .log(LoggingLevel.DEBUG,"New processing request: ${body}")

    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .log(LoggingLevel.INFO,"Update DEMS case record (dems_case_id = ${exchangeProperty.dems_case_id} record_id = ${exchangeProperty.recordId}) ...")
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
    .log(LoggingLevel.INFO,"dems_case_id: ${exchangeProperty.dems_case_id} dems_record_id: ${exchangeProperty.dems_record_id}")
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
        String boundryString = "multipart/form-data;boundary=" + boundary;
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "multipart/form-data;boundary=" + boundary);
        exchange.setProperty("contentType", boundryString);
        exchange.setProperty("multipartBody", multipartBody);
        exchange.getMessage().setBody(multipartBody);
      }
    })
    //.to("file:/tmp/output?fileName=${exchangeProperty.dems_case_id}-${exchangeProperty.dems_record_id}-jade.pdf")
    .to("direct:streamCaseRecordNative")

    // wait 3 seconds, before uploading the pdf version of the document
    .delay(3000)

    .to("direct:streamCaseRecordPdf")

    .end()
    ;
  }

  private void streamCaseRecordNative() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .doTry()
      .removeHeader("CamelHttpUri")
      .removeHeader("CamelHttpBaseUri")
      .removeHeaders("CamelHttp*")
      .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
      .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
      .log(LoggingLevel.INFO,"Uploading DEMS case record native file (caseId = ${exchangeProperty.dems_case_id} recordId = ${exchangeProperty.dems_record_id}) ...")
      .log(LoggingLevel.DEBUG, "headers: ${headers}")
      .log(LoggingLevel.DEBUG, "body: ${body}")
      .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/records/${exchangeProperty.dems_record_id}/Native?renditionAction=Regenerate")
      .log(LoggingLevel.INFO,"DEMS case record native file uploaded.")
    .endDoTry()
    .doCatch(HttpOperationFailedException.class)
      .log(LoggingLevel.ERROR,"Exception: ${exception}")
      //.log(LoggingLevel.ERROR,"Exception message: ${body}")
      .choice()
        .when().simple("${exception.statusCode} == 412")
          .log(LoggingLevel.ERROR, "Encountered import record conflict on Native record upload.  Wait additional 1.5 seconds to retry.")
          .delay(1500)

          .setBody(simple("${exchangeProperty.multipartBody}"))
          .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
          .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
          .log(LoggingLevel.INFO,"Retry uploading DEMS case record native file (caseId = ${exchangeProperty.dems_case_id} recordId = ${exchangeProperty.dems_record_id}) ...")
          .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/records/${exchangeProperty.dems_record_id}/Native?renditionAction=Regenerate")
          .log(LoggingLevel.INFO,"DEMS case record native file re-uploaded successfully.")
        .endChoice()
        .otherwise()
          .log(LoggingLevel.ERROR, "Unexpected error while uploading native document.")
          .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${exception.statusCode}"))
          .process(new Processor() {
            public void process(Exchange exchange) throws Exception {
              throw exchange.getException();
            }
          })
        .end()
      .log(LoggingLevel.ERROR, "Encountered exception while uploading native record")
    .endDoCatch()
    ;
  }

  private void streamCaseRecordPdf() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .doTry()
      .setBody(simple("${exchangeProperty.multipartBody}"))
      .removeHeader("CamelHttpUri")
      .removeHeader("CamelHttpBaseUri")
      .removeHeaders("CamelHttp*")
      .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
      .setHeader(Exchange.CONTENT_TYPE, simple("${exchangeProperty.contentType}"))
      .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
      .log(LoggingLevel.INFO,"Uploading DEMS case record pdf file (caseId = ${exchangeProperty.dems_case_id} recordId = ${exchangeProperty.dems_record_id}) ...")
      .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/records/${exchangeProperty.dems_record_id}/Pdf?renditionAction=Regenerate")
      .log(LoggingLevel.INFO,"DEMS case record pdf file uploaded.")
    .endDoTry()
    .doCatch(HttpOperationFailedException.class)
      .log(LoggingLevel.ERROR,"Exception: ${exception}")
      //.log(LoggingLevel.ERROR,"Exception message: ${body}")
      .choice()
        .when().simple("${exception.statusCode} == 412")
          .log(LoggingLevel.ERROR, "Encountered import record conflict on Pdf record upload.  Wait additional 1.5 seconds to retry.")
          .delay(1500)

          .setBody(simple("${exchangeProperty.multipartBody}"))
          .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
          .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
          .log(LoggingLevel.INFO,"Retry uploading DEMS case record pdf file (caseId = ${exchangeProperty.dems_case_id} recordId = ${exchangeProperty.dems_record_id}) ...")
          .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/records/${exchangeProperty.dems_record_id}/Pdf?renditionAction=Regenerate")
          .log(LoggingLevel.INFO,"DEMS case record pdf file re-uploaded successfully.")
        .endChoice()
        .otherwise()
          .log(LoggingLevel.ERROR, "Unexpected error while uploading pdf document.")
          .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${exception.statusCode}"))
          .process(new Processor() {
            public void process(Exchange exchange) throws Exception {
              throw exchange.getException();
            }
          })
        .end()
      .log(LoggingLevel.ERROR, "Encountered exception while uploading pdf record")
    .endDoCatch()
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
    .log(LoggingLevel.INFO,"sourceCaseId = ${header[sourceCaseId]} destinationCaseId = ${header[destinationCaseId]} prefixName = ${header[prefixName]}")

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
      .log(LoggingLevel.INFO, "Expected record migration count: ${exchangeProperty.length}")
      .choice()
        .when(simple("${header.CamelHttpResponseCode} == 200 && ${exchangeProperty.length} > 0 && ${header[destinationCaseId]} != ''"))
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
          .log(LoggingLevel.DEBUG, "Export report response: '${body}'")
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

      .log(LoggingLevel.DEBUG, "Inactivation json: ${body}")
      .removeHeader("CamelHttpUri")
      .removeHeader("CamelHttpBaseUri")
      .removeHeaders("CamelHttp*")
      .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
      .toD("https://{{dems.host}}/cases/${header.sourceCaseId}")

      // JADE-2671 - Search for all DEMS cases which have Primary Agency File of this recently inactivated case, and update those
      // as well, to the new target primary agency file.
      .log(LoggingLevel.INFO, "Updating related cases with new primary file id.")
      //"Primary Agency File ID"
      .log(LoggingLevel.INFO,"sourceRccId = ${exchangeProperty.sourceRccId}...")
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
      .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/cases/Primary Agency File ID:${exchangeProperty.sourceRccId}/id?throwExceptionOnFailure=false")

      .setProperty("length",jsonpath("$.length()"))
      .choice()
        .when(simple("${header.CamelHttpResponseCode} == 200 && ${exchangeProperty.length} > 0"))
          // Loop through list and update the primary id
          .split()
            .jsonpathWriteAsString("$.*")
            .setProperty("inactiveCaseId",jsonpath("$.id"))

            // get dest key for setting primary agency file
            .setProperty("id", simple("${exchangeProperty.inactiveCaseId}"))
            .to("direct:getCourtCaseStatusById")
            .setProperty("inactiveCaseName",jsonpath("$.name"))
            .setProperty("inactiveRccId",jsonpath("$.key"))
            .setProperty("inactiveStatus",jsonpath("$.status"))
            .choice()
              .when(simple("${exchangeProperty.inactiveStatus} == 'Inactive'"))
                .setBody(simple("{\"name\": \"${exchangeProperty.inactiveCaseName}\",\"key\": \"${exchangeProperty.inactiveRccId}\",\"status\": \"Inactive\", \"fields\": [{\"name\":\"Primary Agency File ID\",\"value\":\"${exchangeProperty.destRccId}\"}, {\"name\":\"Primary Agency File No.\",\"value\":\"${exchangeProperty.destAgencyFile}\"}]}"))

                .log(LoggingLevel.INFO, "Changing primary id for case id: ${exchangeProperty.inactiveCaseId}")
                .removeHeader("CamelHttpUri")
                .removeHeader("CamelHttpBaseUri")
                .removeHeaders("CamelHttp*")
                .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
                .toD("https://{{dems.host}}/cases/${exchangeProperty.inactiveCaseId}")
              .endChoice()
              .otherwise()
                .log(LoggingLevel.ERROR, "Case id: ${exchangeProperty.inactiveCaseId} has primary of an inactive case, but is active")
              .endChoice()
            .end()
          .end()
        .endChoice()
        .when(simple("${header.CamelHttpResponseCode} == 200"))
          .log(LoggingLevel.INFO,"No cases found with primary id of ${exchangeProperty.sourceRccId}.")
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

  //as part of jade 1750
  private void reassignParticipantCases() {
    // IN: header.fromPersonId
    // IN: header.toPersonId
    // IN: header.fromPartId
    // IN: header.toPartId
    // IN: header.fromMergedPartKeys
    // IN: header.toMergedPartKeys

    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"fromPersonId = ${header[fromPersonId]} ")
    .log(LoggingLevel.INFO,"toPersonId = ${header[toPersonId]}")
    .log(LoggingLevel.INFO,"fromPartId = ${header[fromPartId]} ")
    .log(LoggingLevel.INFO,"toPartId = ${header[toPartId]}")
    .log(LoggingLevel.INFO,"fromMergedPartKeys = ${header[fromMergedPartKeys]} ")
    .log(LoggingLevel.INFO,"toMergedPartKeys = ${header[toMergedPartKeys]}")

    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .doTry()
      .setHeader(Exchange.HTTP_METHOD, simple("POST"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
      // Add identifier record for inactivated person to indicate the new person record to refer to.
      .setBody(simple("{\"entityType\":\"Person\",\"entityId\":\"${header.fromPersonId}\",\"identifierType\":\"PrimaryId\",\"identifierValue\":\"${header.toPersonId}\"}"))
      .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/identifiers")
    .endDoTry()
    .doCatch(HttpOperationFailedException.class)
      .log(LoggingLevel.ERROR,"Exception: ${exception}")
      .log(LoggingLevel.ERROR,"Exception message: ${body}")
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

      .log(LoggingLevel.INFO,"Exchange Context: ${exchange.context}")

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

      .log(LoggingLevel.WARN, "Failed indentifier creation of associated merge: ${exchangeProperty.exception}")
      .log(LoggingLevel.ERROR,"CCMException: ${header.CCMException}")
    .end()

    // BCPSDEMS-1969 - set the Merged Participant Keys custom field.
    // first get any values that exist in the source (to be inactivated participant)
    // set the source's value to the target part id
    // get the values that exist in the target (primary participant)
    // append both the source value and target value together, in comma delimited string.

    //look-up source person to be inactivated and set the MergedParticipantKeys custom field to the primary part id.
    .doTry()
      .removeHeader("CamelHttpUri")
      .removeHeader("CamelHttpBaseUri")
      .removeHeaders("CamelHttp*")
      .setHeader(Exchange.HTTP_METHOD, simple("GET"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
      .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/persons/${header[fromPersonId]}")
      //.log(LoggingLevel.DEBUG,"Person in system: '${body}'")
      .setProperty("demspersondata", simple("${body}"))
      .unmarshal().json()
      .log(LoggingLevel.DEBUG, "Search for from person: ${header[fromPersonId]} response: ${header[CamelHttpResponseCode]}")

      .choice()
        .when(simple("${header.CamelHttpResponseCode} == 200"))
          .process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
              String toPartId = exchange.getMessage().getHeader("toPartId", String.class);
              Object d =(Object)exchange.getIn().getBody();
              exchange.getMessage().setBody(d);

              LinkedHashMap<String, Object> dataMap = (LinkedHashMap<String, Object>) d;

              ObjectMapper objectMapper = new ObjectMapper();
              String json = objectMapper.writeValueAsString(dataMap);

              Boolean present =false;
              ObjectMapper personDataMapper = new ObjectMapper();
              personDataMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
              DemsPersonData personData = personDataMapper.readValue(json, DemsPersonData.class);
              for(DemsFieldData fieldData : personData.getFields()) {
                log.info("Field Name: " + fieldData.getName());
                log.info("Field Value: " + fieldData.getValue());
                if(fieldData.getName().equalsIgnoreCase("MergedParticipantKeys")) {
                  present = true;
                  log.info("Updating existing participant keys to:" + toPartId);
                  fieldData.setValue(toPartId);
                  break;
                }
              }
              if(!present) {
                log.info("Adding the merged participant primary key: " + toPartId);
                personData.generateMergedParticipantKeys(toPartId);
              }

              exchange.getMessage().setBody(personData, DemsPersonData.class);
            }
          })
          .marshal().json(JsonLibrary.Jackson, DemsPersonData.class)
          .log(LoggingLevel.DEBUG,"DEMS-bound person data: '${body}'")
          .setProperty("update_data", simple("${body}"))

          // update person
          .removeHeader("CamelHttpUri")
          .removeHeader("CamelHttpBaseUri")
          .removeHeaders("CamelHttp*")
          .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
          .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
          .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
          .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/persons/${header[fromPersonId]}")
          .log(LoggingLevel.INFO,"Person updated.")

          .log(LoggingLevel.INFO, "There are cases with the person.")
        .endChoice()
        .otherwise()
          .log(LoggingLevel.INFO, "No cases associated to person.")
        .end()
    .endDoTry()
    .doCatch(HttpOperationFailedException.class)
      .log(LoggingLevel.ERROR,"Exception: ${exception}")
      .log(LoggingLevel.ERROR,"Exception message: ${body}")
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

      .log(LoggingLevel.INFO,"Exchange Context: ${exchange.context}")

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

      .log(LoggingLevel.WARN, "Failed indentifier creation of associated merge: ${exchangeProperty.exception}")
      .log(LoggingLevel.ERROR,"CCMException: ${header.CCMException}")
    .end()


    //look-up target person to be primary and set the MergedParticipantKeys custom field to the fromMergedPartKeys, toMergedPartKeys and source part id.
    .doTry()
      .removeHeader("CamelHttpUri")
      .removeHeader("CamelHttpBaseUri")
      .removeHeaders("CamelHttp*")
      .setHeader(Exchange.HTTP_METHOD, simple("GET"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
      .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/persons/${header[toPersonId]}")
      //.log(LoggingLevel.DEBUG,"Person in system: '${body}'")
      .setProperty("demspersondata", simple("${body}"))
      .unmarshal().json()

      .log(LoggingLevel.DEBUG, "Search to person: ${header[toPersonId]} response: ${header[CamelHttpResponseCode]}")
      .choice()
        .when(simple("${header.CamelHttpResponseCode} == 200"))
          .process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
              String fromPartId = exchange.getMessage().getHeader("fromPartId", String.class);
              String fromMergedPartKeys = exchange.getMessage().getHeader("fromMergedPartKeys", String.class);
              String toMergedPartKeys = exchange.getMessage().getHeader("toMergedPartKeys", String.class);

              StringBuffer participantKeys = new StringBuffer(fromPartId);
              if(fromMergedPartKeys != null && fromMergedPartKeys != "") {
                participantKeys.append(",");
                participantKeys.append(fromMergedPartKeys);
              }

              if(toMergedPartKeys != null && toMergedPartKeys != "") {
                participantKeys.append(",");
                participantKeys.append(toMergedPartKeys);
              }

              Object d =(Object)exchange.getIn().getBody();
              exchange.getMessage().setBody(d);
              LinkedHashMap<String, Object> dataMap = (LinkedHashMap<String, Object>) d;

              ObjectMapper objectMapper = new ObjectMapper();
              String json = objectMapper.writeValueAsString(dataMap);

              Boolean present =false;
              ObjectMapper personDataMapper = new ObjectMapper();
              personDataMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
              DemsPersonData personData = personDataMapper.readValue(json, DemsPersonData.class);
              for(DemsFieldData fieldData : personData.getFields()) {
                log.info("Field Name: " + fieldData.getName());
                log.info("Field Value: " + fieldData.getValue());
                if(fieldData.getName().equalsIgnoreCase("MergedParticipantKeys")) {
                  present = true;
                  log.info("Updating existing participant keys to:" + participantKeys.toString());
                  fieldData.setValue(participantKeys.toString());
                  break;
                }
              }
              if(!present) {
                log.info("Adding the merged participant keys: " + participantKeys.toString());
                personData.generateMergedParticipantKeys(participantKeys.toString());
              }

              exchange.getMessage().setBody(personData, DemsPersonData.class);
            }
          })
          .marshal().json(JsonLibrary.Jackson, DemsPersonData.class)
          .log(LoggingLevel.DEBUG,"DEMS-bound person data: '${body}'")
          .setProperty("update_data", simple("${body}"))

          // update case
          .setBody(simple("${exchangeProperty.update_data}"))
          .removeHeader("CamelHttpUri")
          .removeHeader("CamelHttpBaseUri")
          .removeHeaders("CamelHttp*")
          .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
          .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
          .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
          .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/persons/${header[toPersonId]}")
          .log(LoggingLevel.INFO,"Person updated.")
          .log(LoggingLevel.INFO, "There are cases with the person.")
        .endChoice()
        .otherwise()
          .log(LoggingLevel.INFO, "No cases associated to person.")
        .end()
    .endDoTry()
    .doCatch(HttpOperationFailedException.class)
      .log(LoggingLevel.ERROR,"Exception: ${exception}")
      .log(LoggingLevel.ERROR,"Exception message: ${body}")
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

      .log(LoggingLevel.INFO,"Exchange Context: ${exchange.context}")

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

      .log(LoggingLevel.WARN, "Failed indentifier creation of associated merge: ${exchangeProperty.exception}")
      .log(LoggingLevel.ERROR,"CCMException: ${header.CCMException}")
    .end()


    // Re-assign any straggling cases on the source person over to the now primary person.
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/persons/${header.fromPersonId}/reassign-cases/${header.toPersonId}")
    .log(LoggingLevel.INFO, "response: '${body}'")

    .log(LoggingLevel.INFO, "Checking for exceptions")
    .choice()
      .when(simple("${exchangeProperty.exception} != null"))
        .log(LoggingLevel.INFO, "There is an exception")
        .log(LoggingLevel.ERROR, "Exception: ${exchangeProperty.exception}")

        .process(new Processor() {
          public void process(Exchange exchange) throws Exception {

            Exception ex = (Exception)exchange.getProperty("exception");
            throw ex;
          }
        })
      .otherwise()
        .log(LoggingLevel.INFO, "No exception")
    .end()
    ;
  }

  private void getCaseRecordImageExistsByKey() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: header.number (rcc_id)
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
          .to("direct:getCaseRecordIdByDescriptionImageId")
        .endChoice()
      .end()
    ;
  }

  private void getCaseRecordIdByDescriptionImageId() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: property.courtCaseId
    // IN: header.reportType
    // IN: header.reportTitle
    // IN: header.imageId
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html

    .log(LoggingLevel.INFO,"courtCaseId = ${exchangeProperty.courtCaseId}...")
    .log(LoggingLevel.INFO,"reportType = ${header.reportType}...")
    .log(LoggingLevel.INFO,"reportTitle = ${header.reportTitle}...")
    .log(LoggingLevel.INFO,"imageId = ${header.imageId}...")

    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    // filter on descriptions and title
    // filter-out save version of Yes, and sort any No value first.
    .setProperty("queryUrl", simple("https://{{dems.host}}/cases/${exchangeProperty.courtCaseId}/records?filter=descriptions:\"${header.reportType}\" AND title:\"${header.reportTitle}\"&fields=cc_SaveVersion,cc_OriginalFileNumber,cc_JustinImageId&sort=cc_SaveVersion desc"))
    .log(LoggingLevel.DEBUG,"Query URL: ${exchangeProperty.queryUrl}")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.courtCaseId}/records?filter=descriptions:\"${header.reportType}\" AND title:\"${header.reportTitle}\"&fields=cc_SaveVersion,cc_OriginalFileNumber,cc_JustinImageId&sort=cc_SaveVersion desc")
    .log(LoggingLevel.DEBUG,"returned case records = ${body}...")
    .choice()
      .when(simple("${header.CamelHttpResponseCode} == 200"))
        .unmarshal().json(JsonLibrary.Jackson, DemsRecordSearchDataList.class)
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) {
            StringBuffer outputStringBuffer = new StringBuffer();
            DemsRecordSearchDataList rsl = exchange.getIn().getBody(DemsRecordSearchDataList.class);
            String justinImageId = exchange.getMessage().getHeader("imageId", String.class);

            for(DemsRecordSearchData record : rsl.getItems()) {
              String id = "";
              if(record.getEdtID() != null) {
                id = record.getEdtID();
              }
              String originalFileNumber = "";
              if(record.getCc_OriginalFileNumber() != null) {
                originalFileNumber = record.getCc_OriginalFileNumber();
              }
              String saveVersion = "";
              if(record.getCc_SaveVersion() != null) {
                saveVersion = record.getCc_SaveVersion();
              }
              String imageId = "";
              if(record.getCc_JUSTINImageID() != null) {
                imageId = record.getCc_JUSTINImageID();
              }

              if(justinImageId != null && justinImageId.equalsIgnoreCase(imageId)) {
                outputStringBuffer.append("{\"id\": \"");
                outputStringBuffer.append(id);
                outputStringBuffer.append("\", \"saveVersion\": \"");
                outputStringBuffer.append(saveVersion);
                outputStringBuffer.append("\", \"originalFileNumber\": \"");
                outputStringBuffer.append(originalFileNumber);
                outputStringBuffer.append("\", \"imageId\": \"");
                outputStringBuffer.append(imageId);
                outputStringBuffer.append("\"}");
                exchange.setProperty("id", id);
                break;
              }
            }
            if(outputStringBuffer.length() == 0) {
              outputStringBuffer.append("{\"id\": \"\", \"saveVersion\": \"\", \"originalFileNumber\": \"\", \"imageId\": \"\"}");
              exchange.setProperty("id", "");
              log.info("Case record not found.");
            }
            exchange.getMessage().setBody(outputStringBuffer.toString());

          }
        })
      .endChoice()
      .when(simple("${header.CamelHttpResponseCode} >= 300"))
        .log(LoggingLevel.WARN,"body = '${body}'.")
        .setProperty("id", simple(""))
        .setBody(simple("{\"id\": \"\", \"saveVersion\": \"\", \"originalFileNumber\": \"\", \"imageId\": \"\"}"))
        .setHeader("CamelHttpResponseCode", simple("200"))
        .log(LoggingLevel.INFO,"Ran into an error from EDT.")
      .endChoice()
    .end()
    .log(LoggingLevel.INFO, "${body}")
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
    // IN: header.documentId
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html

    .log(LoggingLevel.INFO,"courtCaseId = ${exchangeProperty.courtCaseId}...")
    .log(LoggingLevel.INFO,"reportType = ${header.reportType}...")
    .log(LoggingLevel.INFO,"reportTitle = ${header.reportTitle}...")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    // filter on descriptions and title
    // filter-out save version of Yes, and sort any No value first.
    .setProperty("queryUrl", simple("https://{{dems.host}}/cases/${exchangeProperty.courtCaseId}/records?filter=descriptions:\"${header.reportType}\" AND title:\"${header.reportTitle}\" AND SaveVersion:NOT Yes&fields=cc_SaveVersion,cc_OriginalFileNumber,cc_JustinImageId&sort=cc_SaveVersion desc"))
    .log(LoggingLevel.DEBUG,"Query URL: ${exchangeProperty.queryUrl}")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.courtCaseId}/records?filter=descriptions:\"${header.reportType}\" AND title:\"${header.reportTitle}\" AND SaveVersion:NOT Yes&fields=cc_SaveVersion,cc_OriginalFileNumber,cc_JustinImageId&sort=cc_SaveVersion desc")
    .log(LoggingLevel.DEBUG,"returned case records = ${body}...")
    .choice()
      .when(simple("${header.CamelHttpResponseCode} == 200"))
        .unmarshal().json(JsonLibrary.Jackson, DemsRecordSearchDataList.class)
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) {
            StringBuffer outputStringBuffer = new StringBuffer();
            DemsRecordSearchDataList rsl = exchange.getIn().getBody(DemsRecordSearchDataList.class);
            String documentId = exchange.getMessage().getHeader("documentId", String.class);
            log.info("Document id comparison: "+documentId);

            for(DemsRecordSearchData record : rsl.getItems()) {
              String id = "";
              if(record.getEdtID() != null) {
                id = record.getEdtID();
              }
              String originalFileNumber = "";
              if(record.getCc_OriginalFileNumber() != null) {
                originalFileNumber = record.getCc_OriginalFileNumber();
              }
              String saveVersion = "";
              if(record.getCc_SaveVersion() != null) {
                saveVersion = record.getCc_SaveVersion();
              }
              String imageId = "";
              if(record.getCc_JUSTINImageID() != null) {
                imageId = record.getCc_JUSTINImageID();
              }
              String edtDocId = "";
              if(record.getDocumentID() != null) {
                edtDocId = record.getDocumentID();
              }

              if(documentId != null && documentId.equalsIgnoreCase(edtDocId)) {
                log.info("Found existing match for "+edtDocId+" in record id: "+id);
                outputStringBuffer = new StringBuffer();
                outputStringBuffer.append("{\"id\": \"");
                outputStringBuffer.append(id);
                outputStringBuffer.append("\", \"saveVersion\": \"");
                outputStringBuffer.append(saveVersion);
                outputStringBuffer.append("\", \"originalFileNumber\": \"");
                outputStringBuffer.append(originalFileNumber);
                outputStringBuffer.append("\", \"imageId\": \"");
                outputStringBuffer.append(imageId);
                outputStringBuffer.append("\"}");
                exchange.setProperty("id", id);
                break;
              } else if(outputStringBuffer.length() == 0) {
                outputStringBuffer.append("{\"id\": \"");
                outputStringBuffer.append(id);
                outputStringBuffer.append("\", \"saveVersion\": \"");
                outputStringBuffer.append(saveVersion);
                outputStringBuffer.append("\", \"originalFileNumber\": \"");
                outputStringBuffer.append(originalFileNumber);
                outputStringBuffer.append("\", \"imageId\": \"");
                outputStringBuffer.append(imageId);
                outputStringBuffer.append("\"}");
                exchange.setProperty("id", id);
              }

            }

            if(outputStringBuffer.length() == 0) {
              outputStringBuffer.append("{\"id\": \"\", \"saveVersion\": \"\", \"originalFileNumber\": \"\", \"imageId\": \"\"}");
              exchange.setProperty("id", "");
              log.info("Case record not found.");
            }
            exchange.getMessage().setBody(outputStringBuffer.toString());

          }
        })
      .endChoice()
      .when(simple("${header.CamelHttpResponseCode} >= 300"))
        .log(LoggingLevel.WARN,"body = '${body}'.")
        .setProperty("id", simple(""))
        .setBody(simple("{\"id\": \"\", \"saveVersion\": \"\", \"originalFileNumber\": \"\", \"imageId\": \"\"}"))
        .setHeader("CamelHttpResponseCode", simple("200"))
        .log(LoggingLevel.WARN,"Ran into an error from EDT.")
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
      .to("direct:getCourtCaseDataByKey")
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
    .setProperty("queryUrl", simple("https://{{dems.host}}/cases/${exchangeProperty.courtCaseId}/records?filter=documentId:\"${header.documentId}\"&fields=cc_SaveVersion,cc_OriginalFileNumber,cc_JustinImageId"))
    .log(LoggingLevel.DEBUG,"Query URL: ${exchangeProperty.queryUrl}")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.courtCaseId}/records?filter=documentId:\"${header.documentId}\"&fields=cc_SaveVersion,cc_OriginalFileNumber,cc_JustinImageId")
    .log(LoggingLevel.DEBUG,"returned case records = ${body}...")

    .choice()
      .when(simple("${header.CamelHttpResponseCode} < 300"))
        .unmarshal().json(JsonLibrary.Jackson, DemsRecordSearchDataList.class)
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) {
            StringBuffer outputStringBuffer = new StringBuffer();
            DemsRecordSearchDataList rsl = exchange.getIn().getBody(DemsRecordSearchDataList.class);

            for(DemsRecordSearchData record : rsl.getItems()) {
              String id = "";
              if(record.getEdtID() != null) {
                id = record.getEdtID();
              }
              String originalFileNumber = "";
              if(record.getCc_OriginalFileNumber() != null) {
                originalFileNumber = record.getCc_OriginalFileNumber();
              }
              String saveVersion = "";
              if(record.getCc_SaveVersion() != null) {
                saveVersion = record.getCc_SaveVersion();
              }
              String imageId = "";
              if(record.getCc_JUSTINImageID() != null) {
                imageId = record.getCc_JUSTINImageID();
              }

              outputStringBuffer.append("{\"id\": \"");
              outputStringBuffer.append(id);
              outputStringBuffer.append("\", \"saveVersion\": \"");
              outputStringBuffer.append(saveVersion);
              outputStringBuffer.append("\", \"originalFileNumber\": \"");
              outputStringBuffer.append(originalFileNumber);
              outputStringBuffer.append("\", \"imageId\": \"");
              outputStringBuffer.append(imageId);
              outputStringBuffer.append("\"}");
              exchange.setProperty("id", id);
              break;
            }
            if(outputStringBuffer.length() == 0) {
              outputStringBuffer.append("{\"id\": \"\", \"saveVersion\": \"\", \"originalFileNumber\": \"\", \"imageId\": \"\"}");
              exchange.setProperty("id", "");
              log.info("Case record not found.");
            }
            exchange.getMessage().setBody(outputStringBuffer.toString());
          }
        })
      .endChoice()
      .when(simple("${header.CamelHttpResponseCode} >= 300"))
        .log(LoggingLevel.ERROR, "Unexpected Http Response Code: ${header.CamelHttpResponseCode}")
        .log(LoggingLevel.DEBUG,"body = '${body}'.")
        .setProperty("id", simple(""))
        .setBody(simple("{\"id\": \"\", \"saveVersion\": \"\", \"originalFileNumber\": \"\", \"imageId\": \"\"}"))
        .setHeader("CamelHttpResponseCode", simple("200"))
        .log(LoggingLevel.INFO,"Case record not found.")
      .endChoice()
    .end()
    .log(LoggingLevel.INFO, "${body}")
    ;
  }


  private void getCaseRecordDocIdByEdtId() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: property.courtCaseId
    // IN: property.recordId
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"courtCaseId = ${exchangeProperty.courtCaseId}...")
    .log(LoggingLevel.INFO,"recordId = ${exchangeProperty.recordId}...")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    // filter on descriptions and title
    // filter-out save version of Yes, and sort any No value first.
    .toD("https://{{dems.host}}/cases/${exchangeProperty.courtCaseId}/records?filter=edtID:\"${exchangeProperty.recordId}\"&fields=cc_SaveVersion,cc_OriginalFileNumber,cc_JustinImageId")
    .log(LoggingLevel.DEBUG,"returned case records = ${body}...")
    .choice()
      .when(simple("${header.CamelHttpResponseCode} == 200"))
        .unmarshal().json(JsonLibrary.Jackson, DemsRecordSearchDataList.class)
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) {
            DemsRecordSearchDataList rsl = exchange.getIn().getBody(DemsRecordSearchDataList.class);
            //String documentId = exchange.getMessage().getHeader("documentId", String.class);
            //log.info("List size: "+rsl.getItems().size());
            //log.info("Document id comparison: "+documentId);

            // Get the first record returned
            for(DemsRecordSearchData record : rsl.getItems()) {
              exchange.getMessage().setBody(record);
              break;
            }
          }
        })
      .endChoice()
    .end()
    .marshal().json(JsonLibrary.Jackson, DemsRecordSearchData.class)
    .log(LoggingLevel.DEBUG, "${body}")
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
      .setProperty("recordSourceText",jsonpath("$.cc_Source_Text"))
      .log(LoggingLevel.INFO,"Body: ${body}")
      .doTry()
        .choice()
          .when(simple("${exchangeProperty.recordSource} !contains 'BCPS Work' && ${exchangeProperty.recordSourceText} !contains 'BCPS Work'"))
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

  private void activateCase() {
     // use method name as route id
     String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

     //IN: header.number
     from("platform-http:/" + routeId)
     .routeId(routeId)
     .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
     .log(LoggingLevel.INFO,"looking to activate case id = ${header.case_id}...")

     .setProperty("dems_case_id", simple("${header.case_id}"))

     //.toD("direct:deleteJustinRecords")
     //.log(LoggingLevel.INFO,"DEMS case records deleted.  Return code of ${header.CamelHttpResponseCode}")
     .doTry()
       .choice()
         .when(simple("${header[case_id]} != ''"))
           .log(LoggingLevel.INFO, "Activate case")
           // inactivate the case.
           .setProperty("id", simple("${header.case_id}"))
           .to("direct:getCourtCaseStatusById")
           .setProperty("caseName",jsonpath("$.name"))
           .setProperty("rccId",jsonpath("$.key"))

           .setBody(simple("{\"name\": \"${exchangeProperty.caseName}\",\"key\": \"${exchangeProperty.rccId}\",\"status\": \"Active\"}"))
           //.log(LoggingLevel.INFO, "${body}")
           .removeHeader("CamelHttpUri")
           .removeHeader("CamelHttpBaseUri")
           .removeHeaders("CamelHttp*")
           .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
           .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
           .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
           .toD("https://{{dems.host}}/cases/${header.case_id}")
           .log(LoggingLevel.INFO, "Case activated.")
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
           .log(LoggingLevel.INFO, "Case not activated")
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

  private void inactivateCase() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: header.number
    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"looking to inactive case id = ${header.case_id}...")

    .setProperty("dems_case_id", simple("${header.case_id}"))

    //.toD("direct:deleteJustinRecords")
    //.log(LoggingLevel.INFO,"DEMS case records deleted.  Return code of ${header.CamelHttpResponseCode}")
    .doTry()
      .choice()
        .when(simple("${header[case_id]} != ''"))
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

  private void syncAccusedPersons() {
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: property =number - primary rcc_id
    //IN: property =accused - CaseAccusedList

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"syncAccusedPersons ${header.number}")
    .log(LoggingLevel.DEBUG,"Processing request: ${body}")
    .unmarshal().json(JsonLibrary.Jackson, CaseAccusedList.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        CaseAccusedList accusedPersonsList = exchange.getIn().getBody(CaseAccusedList.class);
        List<CaseAccused> accusedPersons = accusedPersonsList.getCaseAccused();
        exchange.setProperty("accusedPersons", accusedPersons);

        // BCPSDEMS-2313 - append all accused persons names to be set as new case name.
        String newCaseName = DemsChargeAssessmentCaseData.generateCaseName(accusedPersons);
        exchange.setProperty("newCaseName", newCaseName);
      }}
    )

    // Update with the new case name.
    .choice()
    .when(simple("${header.number}!= '' && ${body} != '' "))
      // get the primary rcc, based on the dems primary agency file id

      //.setHeader("number", simple("${header[rcc_id]}"))
      // look for current status of the dems case.
      // and set the rcc to the primary rcc
      .to("direct:getCourtCaseStatusByKey")
      .log(LoggingLevel.DEBUG, "Status: ${body}")
      .unmarshal().json()
      .setProperty("caseId").simple("${body[id]}")
      .setProperty("rccId").simple("${body[key]}")
      .setProperty("caseStatus").simple("${body[status]}")
      .setProperty("caseRccId").simple("${body[primaryAgencyFileId]}")

      .choice()
      // if there is a case, delete participants
      .when(simple("${exchangeProperty.caseId} != ''"))
        .log(LoggingLevel.INFO, "Update case name")
        .setProperty("caseName",jsonpath("$.name"))
        .setProperty("rccId",jsonpath("$.key"))
        .log(LoggingLevel.DEBUG, "Old case name: ${exchangeProperty.caseName}")
        .log(LoggingLevel.DEBUG, "New case name: ${exchangeProperty.newCaseName}")

        .setBody(simple("{\"name\": \"${exchangeProperty.newCaseName}\",\"key\": \"${exchangeProperty.rccId}\"}"))
        .log(LoggingLevel.DEBUG, "${body}")
        .removeHeader("CamelHttpUri")
        .removeHeader("CamelHttpBaseUri")
        .removeHeaders("CamelHttp*")
        .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
        .toD("https://{{dems.host}}/cases/${exchangeProperty.caseId}")
        .log(LoggingLevel.INFO, "Case name updated.")

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
        //.log(LoggingLevel.INFO,"SyncAccussedPersons body before call to Dems participants/sync: ${body}")
        .toD("https://{{dems.host}}/cases/${exchangeProperty.caseId}/participants/sync")
        .setBody(simple("${exchangeProperty.CourtCaseMetadata}"))
        // .log(LoggingLevel.INFO,"SyncAccussedPersons body after call to Dems participants/sync: ${body}")
        .doTry()
          .process(new Processor() {
            @Override
            public void process(Exchange exchange) {

              List<CaseAccused> accusedPersons = (ArrayList<CaseAccused>) exchange.getProperty("accusedPersons");
             // log.info("get accused persons from exchange : size : " + accusedPersons.size());
              exchange.getMessage().setBody(accusedPersons);
             // log.info("set exchange body to accussed persons");
            }
          })
          .marshal().json()
          .split()
            .jsonpathWriteAsString("$.*")
            .setHeader("key", jsonpath("$.identifier"))
            .setHeader("courtCaseId").simple("${exchangeProperty.caseId}")
            .log(LoggingLevel.INFO,"Updating accused participant ...")
            .log(LoggingLevel.DEBUG,"Case Id key = ${exchangeProperty.caseId}")
            .to("direct:processAccusedPerson")
            .log(LoggingLevel.INFO,"Accused participant updated.")
          .end()
        .endDoTry()
        .doCatch(HttpOperationFailedException.class)
          .log(LoggingLevel.ERROR,"Exception: ${exception}")
          .log(LoggingLevel.ERROR,"Exchange Context: ${exchange.context}")
          .choice()
            .when().simple("${exception.statusCode} == 504")
              .log(LoggingLevel.ERROR, "Encountered timeout.  Wait additional 30 seconds to continue.")
               // Sometimes EDT takes longer to create a case than their 30 second gateway timeout, so add a delay and continue on.
              .delay(30000)

              //jade 1747
              .log(LoggingLevel.INFO,"Retry call SyncCaseParticipants for case ${exchangeProperty.caseId}")
              .setProperty("ParticipantTypeFilter", simple("Accused"))
              .setProperty("Participants",simple(""))
              .removeHeader("CamelHttpUri")
              .removeHeader("CamelHttpBaseUri")
              .removeHeaders("CamelHttp*")
              .setHeader(Exchange.HTTP_METHOD, simple("POST"))
              .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
              .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
              .setBody(simple("{\"ParticipantTypeFilter\":\"${exchangeProperty.ParticipantTypeFilter}\",\"Participants\":[]}"))
              .log(LoggingLevel.DEBUG,"ParticipantTypeFilter: ${body}")
              .toD("https://{{dems.host}}/cases/${exchangeProperty.caseId}/participants/sync")
              .setBody(simple("${exchangeProperty.CourtCaseMetadata}"))
              .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentData.class)
              // Merge the accused persons from all related agency files into a unique list
              .process(new Processor() {
                @Override
                public void process(Exchange exchange) {
                  List<CaseAccused> accusedPersons = exchange.getIn().getBody(ArrayList.class);
                  exchange.getMessage().setBody(accusedPersons);
                }
              })

              .marshal().json()
              .split()
                .jsonpathWriteAsString("$.*")
                .setHeader("key", jsonpath("$.identifier"))
                .setHeader("courtCaseId").simple("${exchangeProperty.caseId}")
                .log(LoggingLevel.INFO,"Updating accused participant ...")
                .log(LoggingLevel.DEBUG,"Court case  id = ${exchangeProperty.caseId}")
                .to("direct:processAccusedPerson")
                .log(LoggingLevel.INFO,"Accused participant updated.")
              .end()

            .endChoice()
            .when().simple("${exception.statusCode} >= 400")
              .log(LoggingLevel.ERROR,"Client side error.  HTTP response code = ${exception.statusCode}")
              .log(LoggingLevel.ERROR, "Body: '${exception}'")
              .log(LoggingLevel.ERROR, "${exception.message}")
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
      .endChoice()
    .end();
  }

  private void http_syncAccusedPersons() {
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: property =number - primary rcc_id
    //IN: property =accused - List<CaseAccused>

    from("platform-http:/syncAccusedPersons?httpMethodRestrict=POST")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"syncAccusedPersons ${header.number}")
    .log(LoggingLevel.DEBUG,"Processing request: ${body}")

    .to("direct:syncAccusedPersons")
    .end();
  }

  private void deleteExistingCase() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header = id
    from("platform-http:/" + routeId + "?httpMethodRestrict=PUT")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"Searching for case with: rcc_id = ${header.rcc_id} ...")
    .setProperty("rcc_id", header("rcc_id"))
    .choice()
      .when(simple("${exchangeProperty.rcc_id} != null"))
        .log(LoggingLevel.INFO, "Look-up caseId.")
        .setHeader("number", simple("${header[rcc_id]}"))

        .to("direct:getCourtCaseStatusByKey")
        .unmarshal().json(JsonLibrary.Jackson, DemsCaseStatus.class)

        .process(new Processor() {
          @Override
          public void process(Exchange exchange) {
            DemsCaseStatus demsCaseStatus = (DemsCaseStatus)exchange.getIn().getBody(DemsCaseStatus.class);
            exchange.setProperty("dems_case_id", demsCaseStatus.getId());

            //Random r = new Random();
            //int low = 1;
            //int high = 9999;
            //int random = r.nextInt(high-low) + low;
            StringBuffer outputStringBuffer = new StringBuffer();

            outputStringBuffer.append("{\"id\": \"");
            outputStringBuffer.append(demsCaseStatus.getId());
            outputStringBuffer.append("\", \"name\": \"");
            outputStringBuffer.append(demsCaseStatus.getName());

            /*outputStringBuffer.append("\", \"key\": \"");
            outputStringBuffer.append("AUTO-DELETE-");
            outputStringBuffer.append(random);
            outputStringBuffer.append("-");
            outputStringBuffer.append(demsCaseStatus.getKey());
            outputStringBuffer.append("\", \"status\": \"");*/

            outputStringBuffer.append("\", \"key\": null");
            outputStringBuffer.append(", \"status\": \"");

            outputStringBuffer.append("Inactive");
            outputStringBuffer.append("\", \"fields\": [");

            outputStringBuffer.append("{ \"name\": \"Agency File ID\", \"value\": \"AUTO-DELETE\" },");
            outputStringBuffer.append("{ \"name\": \"Agency File No.\", \"value\": \"AUTO-DELETE\" },");
            outputStringBuffer.append("{ \"name\": \"Court File No.\", \"value\": \"AUTO-DELETE\" },");
            outputStringBuffer.append("{ \"name\": \"Court File Unique ID\", \"value\": \"AUTO-DELETE\" },");
            outputStringBuffer.append("{ \"name\": \"Primary Agency File ID\", \"value\": \"AUTO-DELETE\" },");
            outputStringBuffer.append("{ \"name\": \"Primary Agency File No.\", \"value\": \"AUTO-DELETE\" }");
            outputStringBuffer.append("]}");

            exchange.getMessage().setBody(outputStringBuffer.toString());
          }
        })

        .doTry()
          .choice()
            .when(simple("${exchangeProperty.dems_case_id} != ''"))
              // delete case
              .removeHeader("CamelHttpUri")
              .removeHeader("CamelHttpBaseUri")
              .removeHeaders("CamelHttp*")
              .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
              .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
              .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
              .log(LoggingLevel.INFO,"Deleting DEMS case (key = ${header.rcc_id} id = ${exchangeProperty.dems_case_id})... ${body}")
              .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}")
              .log(LoggingLevel.INFO,"DEMS case deleted.")
              .log(LoggingLevel.INFO,"Removing users")

              .process(new Processor() {
                @Override
                public void process(Exchange exchange) {
                  StringBuffer outputStringBuffer = new StringBuffer();

                  outputStringBuffer.append("{\"userIds\": [");
                  outputStringBuffer.append("]}");

                  exchange.getMessage().setBody(outputStringBuffer.toString());
                }
              })

              // sync case users
              .removeHeader("CamelHttpUri")
              .removeHeader("CamelHttpBaseUri")
              .removeHeaders("CamelHttp*")
              .setHeader(Exchange.HTTP_METHOD, simple("POST"))
              .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
              .setHeader("Authorization", simple("Bearer " + "{{dems.token}}"))
              .log(LoggingLevel.INFO,"Synchronizing case users ...")
              .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/users/sync")

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
        .log(LoggingLevel.ERROR, "No rcc_id provided.")
      .endChoice()
    .end()
    ;
  }

  private void updateExistingParticipantwithOTCV2() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    // IN: header = id
    from("platform-http:/" + routeId )
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"updateExistingParticipantwithOTC... pages: ${header.pageFrom} -> ${header.pageTo}")
    //.setProperty("pageFrom", header("pageFrom"))
    //.setProperty("pageTo", header("pageTo"))

    .setProperty("v2DemsHost", simple("{{dems.host}}"))
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        String v2DemsHost = (String)exchange.getProperty("v2DemsHost");
        v2DemsHost = v2DemsHost.replace("/v1", "/v2");
        exchange.setProperty("v2DemsHost", v2DemsHost);
      }
    })
    .log(LoggingLevel.INFO, "New URL: ${exchangeProperty.v2DemsHost}")

    .setProperty("pageSize").simple("500")
    .setProperty("maxRecordIncrements").simple("250")
    .setProperty("incrementCount").simple("1")
    .setProperty("continueLoop").simple("true")

    .choice()
      .when(simple("${header.pageFrom} != null && ${header.pageFrom} != ''"))
        .setProperty("incrementCount").simple("${header.pageFrom}")
      .endChoice()
    .end()

    .choice()
      .when(simple("${header.pageTo} != null && ${header.pageTo} != ''"))
        .setProperty("maxRecordIncrements").simple("${header.pageTo}")
      .endChoice()
    .end()

    // limit the number of times incremented to 250.
    .loopDoWhile(simple("${exchangeProperty.continueLoop} == 'true' && ${exchangeProperty.incrementCount} <= ${exchangeProperty.maxRecordIncrements}"))
      .log(LoggingLevel.INFO, "\n\nViewing page: ${exchangeProperty.incrementCount}")

      .removeHeader("CamelHttpUri")
      .removeHeader("CamelHttpBaseUri")
      .removeHeaders("CamelHttp*")
      .setHeader(Exchange.HTTP_METHOD, simple("GET"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
      //traverse through all persons in DEMS
      .toD("https://${exchangeProperty.v2DemsHost}/org-units/{{dems.org-unit.id}}/persons?page=${exchangeProperty.incrementCount}&pagesize=${exchangeProperty.pageSize}")
      //.log(LoggingLevel.DEBUG,"Person list: '${body}'")
      .setProperty("length",jsonpath("$.items.length()"))
      .log(LoggingLevel.INFO,"Person count: ${exchangeProperty.length}")

      .choice()
        .when(simple("${exchangeProperty.length} < 1"))
          .log(LoggingLevel.INFO, "End of pages.")
          .setProperty("continueLoop").simple("false")
        .endChoice()
        .when(simple("${header.CamelHttpResponseCode} == 200"))
          .to("direct:processParticipantsList")
        .endChoice()
      .end()

      // increment the loop count.
      .process(new Processor() {
        @Override
        public void process(Exchange ex) {
          Integer incrementCount = (Integer)ex.getProperty("incrementCount", Integer.class);
          incrementCount++;
          ex.setProperty("incrementCount", incrementCount);
        }
      })
    .end() // end loop
    .log(LoggingLevel.INFO,"end of updateExistingParticipantwithOTCV2.")
    ;
  }

  private void processCaseList() {
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    //.log(LoggingLevel.DEBUG,"Person list: '${body}'")
    .setProperty("length",jsonpath("$.items.length()"))
    .log(LoggingLevel.INFO,"Case count: ${exchangeProperty.length}")
    .removeProperty("mdoc")
    .removeProperty("caseRccId")
    .split()
      .jsonpathWriteAsString("$.items.*")
      .setProperty("id",jsonpath("$.id"))
      .to("direct:getCourtCaseStatusById")
      .setProperty("status",jsonpath("$.status"))
      .setProperty("caseRccId",jsonpath("$.key"))
      .setProperty("mdoc",jsonpath("$.courtFileId"))    
      .log(LoggingLevel.INFO,"Case mdoc: ${exchangeProperty.mdoc}, case rcc id : ${exchangeProperty.caseRccId}, status: ${exchangeProperty.status}")
      .choice()
      .when().simple("${exchangeProperty.status} == 'Active' && ${exchangeProperty.caseRccId} != ''") 
      .setHeader("splitRccId", simple("${exchangeProperty.caseRccId}"))
      .split()
      .tokenize(";", "splitRccId", false)
      .process(new Processor() {
        @Override
        public void process(Exchange ex) {
          String token = ex.getIn().getBody(String.class);
          token = token.replace(';', ' ');
          ex.getIn().setHeader("rccId", token);
          ex.setProperty("currentRccId", token);
          log.info("current rcc id : " + token);
        }})
         .to("http://ccm-lookup-service/getFileNote")
         .log(LoggingLevel.INFO, "file note in body : ${body}")
         //.setBody(simple("${body}"))
         .unmarshal().json(JsonLibrary.Jackson, FileNote.class)
         .process(new Processor() {
           @Override
           public void process(Exchange exchange) throws Exception {
             FileNote fileNote = (FileNote)exchange.getIn().getBody(FileNote.class);
             if (fileNote != null) {
               log.info("file not null");
               DemsRecordData demsRecordData = new DemsRecordData(fileNote);
               exchange.getMessage().setHeader("documentId", demsRecordData.getDocumentId());
               int fileNoteId = fileNote.getFile_note_id() != null && !fileNote.getFile_note_id().isBlank() ? Integer.parseInt(fileNote.getFile_note_id()) : 0;
               if (fileNoteId >= 0) {
                 exchange.setProperty("addFileNote", simple("true"));
                 exchange.getIn().setHeader("caseId", exchange.getProperty("id"));
                 fileNote.setRcc_id(exchange.getProperty("currentRccId", String.class));
                 exchange.setProperty("fileNoteToSend", fileNote);
                 exchange.setProperty("recordId", "");
                 exchange.getIn().setHeader("number", exchange.getProperty("currentRccId"));
               }
               else{
                 exchange.setProperty("addFileNote", simple("false"));
               }
               exchange.getIn().setBody(null);
             }
           }})
          
           .to("direct:getCaseDocIdExistsByKey")
            .unmarshal().json()
           .setProperty("recordId").simple("${body[id]}")
           .choice()
           .when(simple("${exchangeProperty.addFileNote} == 'true' && ${exchangeProperty.recordId} != ''"   ))
             // add file note to case
             .setBody(simple("${exchangeProperty.fileNoteToSend}"))
             .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
             .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
             .setHeader("caseId", simple("${exchangeProperty.id}"))
             .setHeader("recordId", simple("${exchangeProperty.recordId}"))
             .marshal().json(JsonLibrary.Jackson, FileNote.class)
             .to("direct:streamNoteRecord")
      .end() // end split for rcc-id
      .endChoice()
      .when().simple("${exchangeProperty.status} == 'Active' && ${exchangeProperty.mdoc} != ''") 
      .setHeader("splitMdoc", simple("${exchangeProperty.mdoc}"))
     // .log(LoggingLevel.INFO, "setting splitMdoc to : ${exchangeProperty.mdoc}")
      .split() // look at mdoc
      .tokenize(";", "splitMdoc", false)
      .process(new Processor() {
        @Override
        public void process(Exchange ex) {
          String token = ex.getIn().getBody(String.class);
          token = token.replace(';', ' ');
          ex.getIn().setHeader("mdocJustinNo", token);
          ex.setProperty("currentMdoc", token);
         log.info("current mdoc : " + token);
        }})
        .to("http://ccm-lookup-service/getFileNote")
        .log(LoggingLevel.INFO, "file note in body : ${body}")
        //.setBody(simple("${body}"))
        .unmarshal().json(JsonLibrary.Jackson, FileNote.class)
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            FileNote fileNote = (FileNote)exchange.getIn().getBody(FileNote.class);
            if (fileNote != null) {
              log.info("file not null");
              DemsRecordData demsRecordData = new DemsRecordData(fileNote);
              exchange.getMessage().setHeader("documentId", demsRecordData.getDocumentId());
              int fileNoteId = fileNote.getFile_note_id() != null && !fileNote.getFile_note_id().isBlank() ? Integer.parseInt(fileNote.getFile_note_id()) : 0;
              if (fileNoteId >= 0) {
                exchange.setProperty("addFileNote", simple("true"));
                exchange.getIn().setHeader("number", exchange.getProperty("mdoc"));
                exchange.getIn().setHeader("caseId", exchange.getProperty("id"));
                exchange.setProperty("fileNoteToSend", fileNote);
                //exchange.getIn().setHeader("number", exchange.getProperty("caseRccId"));
                exchange.setProperty("recordId", "");
              }
              else{
                exchange.setProperty("addFileNote", simple("false"));
              }
              exchange.getIn().setBody(null);
            }
          }})
          .setProperty("mdoc_justin_no", simple("${exchangeProperty.currentMdoc}"))
          .setHeader("event_key", simple("${exchangeProperty.currentMdoc}"))
          .setHeader("number", simple("${exchangeProperty.currentMdoc}"))
          .to("direct:compileRelatedCourtFiles")
            // re-set body to the metadata_data json.
          .setBody(simple("${exchangeProperty.metadata_data}"))
          .log(LoggingLevel.DEBUG, "metadata_data: ${body}")
          .unmarshal().json(JsonLibrary.Jackson, CourtCaseData.class)
          .process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
  
            CourtCaseData ccd = exchange.getIn().getBody(CourtCaseData.class);
            exchange.setProperty("agency_file_no", ccd.getPrimary_agency_file().getAgency_file_no());
            exchange.getMessage().setBody(ccd.getPrimary_agency_file(), ChargeAssessmentDataRef.class);
          }
        })
        .marshal().json(JsonLibrary.Jackson, ChargeAssessmentDataRef.class)
        .log(LoggingLevel.DEBUG, "Court File Primary Rcc: ${body}")
        .setBody(simple("${bodyAs(String)}"))
        .setProperty("mdoc_rcc_id", jsonpath("$.rcc_id"))
        .setProperty("primary_yn", jsonpath("$.primary_yn"))
        .setHeader("key").simple("${exchangeProperty.mdoc_rcc_id}")
        .setHeader("event_key",simple("${exchangeProperty.mdoc_rcc_id}"))
        .setHeader("number",simple("${exchangeProperty.mdoc_rcc_id}"))
        .to("direct:getCaseDocIdExistsByKey")
        .unmarshal().json()
        .setProperty("recordId").simple("${body[id]}")
        .choice()
          .when(simple("${exchangeProperty.addFileNote} == 'true' && ${exchangeProperty.recordId} != ''"   ))
       
            // add file note to case
          .setBody(simple("${exchangeProperty.fileNoteToSend}"))
          .marshal().json(JsonLibrary.Jackson, FileNote.class)
          .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
          .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
          .setHeader("caseId", simple("${exchangeProperty.id}"))
          .setHeader("recordId", simple("${exchangeProperty.recordId}"))
          .marshal().json(JsonLibrary.Jackson, FileNote.class)
           
            .to("direct:streamNoteRecord")
          .end() // end choice
          .end() // end split for mdoc
      .end() // end of choice
      //.end()
      .end() // end loop
      .log(LoggingLevel.INFO, "End of loop for case.")
    .end() // end route
    .log(LoggingLevel.INFO,"end of processCaseList.")
    ;
  }

  private void processParticipantsList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    //.log(LoggingLevel.DEBUG,"Person list: '${body}'")
    .setProperty("length",jsonpath("$.items.length()"))
    .log(LoggingLevel.INFO,"Person count: ${exchangeProperty.length}")
    .split()
      .jsonpathWriteAsString("$.items.*")
      .setProperty("personId",jsonpath("$.id"))
      .setProperty("personKey",jsonpath("$.key"))
      .setProperty("status",jsonpath("$.status"))
      .log(LoggingLevel.DEBUG,"Person Id: ${exchangeProperty.personId}, status: ${exchangeProperty.status}")
      .choice()
        .when().simple("${exchangeProperty.status} == 'Active' && ${exchangeProperty.personKey} != null")
          .to("direct:updateOtcParticipants")
        .endChoice()
      .end()
      .log(LoggingLevel.INFO, "End of loop for person.")
    .end() // end loop
    .log(LoggingLevel.INFO,"end of processParticipantsList.")
    ;
  }

  private void updateOtcParticipants() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html

    //look-up list of accused participants of each case
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    //traverse through all cases in DEMS
    .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/persons/${exchangeProperty.personId}")
    //.log(LoggingLevel.DEBUG,"Person in system: '${body}'")
    .setProperty("otcfieldexist").simple("false")
    .setProperty("demspersondata", simple("${body}"))
    .setProperty("caselength",jsonpath("$.cases.length()"))
    .setProperty("existingOtc",jsonpath("$.fields[?(@.name == 'OTC')]"))
    .unmarshal().json()
    .log(LoggingLevel.INFO, "Participant case length: ${exchangeProperty.caselength}")

    .log(LoggingLevel.INFO, "existingOtc: ${exchangeProperty.existingOtc}")
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        Object d =(Object)exchange.getIn().getBody();
        exchange.getMessage().setBody(d);
        LinkedHashMap<String, Object> dataMap = (LinkedHashMap<String, Object>) d;

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(dataMap);

        Boolean present =false;
        ObjectMapper personDataMapper = new ObjectMapper();
        personDataMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        DemsPersonData personData = personDataMapper.readValue(json, DemsPersonData.class);
        for(DemsFieldData fieldData : personData.getFields()) {
          //log.info("Field Name: " + fieldData.getName());
          //log.info("Field Value: " + fieldData.getValue());
          if(fieldData.getName().equalsIgnoreCase("OTC")) {
            present = true;
            exchange.setProperty("otcfieldexist", "true");
            break;
          }
        }
        if(!present) {
          log.info("Generating OTC for person.");
          personData.generateOTC();
        }

        exchange.getMessage().setBody(personData, DemsPersonData.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsPersonData.class)
    .setProperty("update_data", simple("${body}"))
    .log(LoggingLevel.INFO, "Check otc exist: ${exchangeProperty.otcfieldexist}")
    .choice()
      .when(simple("${exchangeProperty.otcfieldexist} == 'false'"))
        .log(LoggingLevel.INFO,"DEMS-bound person data: '${body}'")
        // update case
        .setBody(simple("${exchangeProperty.update_data}"))
        .setHeader("key", jsonpath("$.key"))
        .setHeader("id", jsonpath("$.id"))
        .log(LoggingLevel.INFO,"DEMS-bound person id: '${header[id]}' key: '${header[key]}'")
        .setHeader("key").simple("${header.key}")
        .setHeader("id").simple("${header.id}")
        .removeHeader("CamelHttpUri")
        .removeHeader("CamelHttpBaseUri")
        .removeHeaders("CamelHttp*")
        .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
        .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/persons/${header[id]}")
        .log(LoggingLevel.INFO,"Person updated.")
      .endChoice()
      .otherwise()
        .log(LoggingLevel.INFO,"OTC data already exists, skip updating person id: ${header[id]}.")
      .endChoice()
    .end()
    ;
  }

  private void destroyCaseRecords() {
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    // IN: header = id
    from("platform-http:/" + routeId )
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"Destroying records for case id = ${header.id}...")
    .setProperty("dems_case_id", header("id"))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("DELETE"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .log(LoggingLevel.INFO," DEMS case record (dems_case_id = ${exchangeProperty.dems_case_id}) ...")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/records")
    .log(LoggingLevel.INFO,"DEMS case record deleted.")

    // Go through list of associated rcc ids and make sure the records are destroyed in them as well.
    .setProperty("id", simple("${header.id}"))
    .to("direct:getCourtCaseStatusById")
    .setProperty("associatedRccIds",jsonpath("$.agencyFileId"))

    .process(new Processor() {
      public void process(Exchange exchange) {
        String associatedRccIds = (String)exchange.getProperty("associatedRccIds");
        String[] rccArray = associatedRccIds.split(";");
        ArrayList<String> associatedRccIdList = new ArrayList<String>(Arrays.asList(rccArray));
        exchange.getMessage().setBody(associatedRccIdList);
      }
    })
    .log(LoggingLevel.INFO, "Unprocessed agency file list: ${body}")
    .marshal().json()
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
          .setProperty("key", simple("${exchangeProperty.agencyFileId}"))
          .to("direct:getCourtCaseIdByKey")
          .setProperty("dems_case_id", jsonpath("$.id"))


          .log(LoggingLevel.INFO,"Destroying records for case id = ${exchangeProperty.dems_case_id}...")
          .removeHeader("CamelHttpUri")
          .removeHeader("CamelHttpBaseUri")
          .removeHeaders("CamelHttp*")
          .setHeader(Exchange.HTTP_METHOD, simple("DELETE"))
          .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
          .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
          .log(LoggingLevel.INFO," DEMS case record (dems_case_id = ${exchangeProperty.dems_case_id}) ...")
          .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/records")
          .log(LoggingLevel.INFO,"DEMS case record deleted.")

        .endChoice()
      .end()
    .end()
    .log(LoggingLevel.INFO, "End of destroy all records.")

    ;
  }

  private void processDeleteNoteRecord() throws HttpOperationFailedException {
     // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    // IN
  
    // property: filenote
    from("platform-http:/" + routeId + "?httpMethodRestrict=POST" )
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"body (before unmarshalling): '${body}'")
   
    .unmarshal().json(JsonLibrary.Jackson, FileNote.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        FileNote demsFileNote = exchange.getIn().getBody(FileNote.class);
        if (demsFileNote != null) {
          exchange.setProperty("file_note", demsFileNote);
          
          log.info("file note before making demsrecord data : " + demsFileNote.getFile_note_id());
          DemsRecordData demsRecord = new DemsRecordData(demsFileNote);
          exchange.getMessage().setHeader("documentId", demsRecord.getDocumentId());
          //log.info("DocId: " + demsRecord.getDocumentId());
          exchange.getMessage().setBody(demsRecord);
          exchange.setProperty("primaryRccId", demsFileNote.getRcc_id());
          exchange.setProperty("primaryMdocNum", demsFileNote.getMdoc_justin_no());
          // .setHeader("number", simple("${header[rcc_id]}"))
          exchange.getMessage().setHeader("number", demsFileNote.getRcc_id());
        }
      }
    })
    .choice()
    // .when(simple("${exchangeProperty.caseRccId} != ''"))
    .when(simple("${exchangeProperty.primaryRccId} != ''"))
    .setHeader("number",simple("${exchangeProperty.primaryRccId}"))
    .end()
    .choice()
    .when(simple("${exchangeProperty.primaryMdocNum} != ''"))
    .setHeader("number", simple("${exchangeProperty.primaryMdocNum}"))
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
       
        CourtCaseData cdd = ex.getIn().getBody(CourtCaseData.class);
        if (cdd != null && cdd.getPrimary_agency_file() != null) {
          ex.getMessage().setHeader("number", cdd.getPrimary_agency_file().getRcc_id());
        }
        else {
          ex.getMessage().setHeader("number", "0");
        }
		 }
    })
    .end()
    .to("direct:getCourtCaseStatusByKey")
    //.unmarshal().json()
    .setProperty("caseId", jsonpath("$.id"))
    //.setProperty("caseId").simple("${body[id]}")
    .setProperty("caseStatus",jsonpath("$.status"))
    //.log(LoggingLevel.INFO, "caseId: '${exchangeProperty.caseId}'")

    // now check this next value to see if there is a collision of this document
    .to("direct:getCaseDocIdExistsByKey")
    //.log(LoggingLevel.INFO, "returned key: ${body}")
    //.unmarshal().json()
    .setProperty("existingRecordId").jsonpath("$.id")
    // Make sure that it is an existing and active case, before attempting to add the record
    .choice()
      .when(simple("${exchangeProperty.existingRecordId} == '' && ${exchangeProperty.caseId} != '' "))
        .log(LoggingLevel.INFO, "Deleting file note in dems")
        .to("direct:deleteJustinFileNoteRecord")
     .end();
  }

  private void processNoteRecord() throws HttpOperationFailedException{
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    // IN
  
    // property: filenote
    from("platform-http:/" + routeId + "?httpMethodRestrict=POST" )
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"body (before unmarshalling): '${body}'")
   
    .unmarshal().json(JsonLibrary.Jackson, FileNote.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        FileNote demsFileNote = exchange.getIn().getBody(FileNote.class);
        exchange.setProperty("file_note", demsFileNote);

        log.info("file note before making demsrecord data : " + demsFileNote.getFile_note_id());
        DemsRecordData demsRecord = new DemsRecordData(demsFileNote);
        exchange.getMessage().setHeader("documentId", demsRecord.getDocumentId());
        log.info("DocId: " + demsRecord.getDocumentId());
        exchange.getMessage().setBody(demsRecord);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsRecordData.class)
    .log(LoggingLevel.DEBUG,"demsrecord = ${bodyAs(String)}.")
    .setBody(simple("${body}"))
    .setProperty("dems_record").simple("${bodyAs(String)}")

    .setProperty("key", simple("${header.number}"))
    // check to see if the court case exists, before trying to insert record to dems.
    .to("direct:getCourtCaseStatusByKey")
    .unmarshal().json()
    .setProperty("caseId").simple("${body[id]}")
    .setProperty("caseStatus").simple("${body[status]}")
    .log(LoggingLevel.INFO, "caseId: '${exchangeProperty.caseId}'")

    // now check this next value to see if there is a collision of this document
    .to("direct:getCaseDocIdExistsByKey")
    //.log(LoggingLevel.INFO, "returned key: ${body}")
    //.unmarshal().json()
    .setProperty("existingRecordId").jsonpath("$.id")
    //.log(LoggingLevel.INFO, "existingRecordId: '${exchangeProperty.existingRecordId}'")

    // Make sure that it is an existing and active case, before attempting to add the record
    .choice()
      .when(simple("${exchangeProperty.existingRecordId} == '' && ${exchangeProperty.caseId} != '' && ${exchangeProperty.caseStatus} == 'Active'"))
        .log(LoggingLevel.INFO, "Creating document record in dems")
        
        .setBody(simple("${exchangeProperty.dems_record}"))
        .log(LoggingLevel.DEBUG, "dems_record: '${exchangeProperty.dems_record}'")
        //.log(LoggingLevel.INFO,"Sending derived dems record: ${body}")

        // proceed to create record in dems, base on the caseid
        .setHeader(Exchange.HTTP_METHOD, simple("POST"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .to("direct:createCaseRecord")
        .log(LoggingLevel.DEBUG,"Created dems record: ${body}")
        .setProperty("recordId", jsonpath("$.edtId"))
        //.log(LoggingLevel.INFO, "recordId: '${exchangeProperty.recordId}'")
      .endChoice()
      .otherwise()
        .log(LoggingLevel.WARN, "Did not create case record due to existing record id: ${exchangeProperty.existingRecordId}, case id: ${exchangeProperty.caseId}, or case status: ${exchangeProperty.caseStatus}")
      .endChoice()
    .end()
    .choice()
      .when(simple("${exchangeProperty.caseId} != '' && ${exchangeProperty.recordId} != null && ${exchangeProperty.recordId} != ''"))
        .log(LoggingLevel.INFO, "attempt to stream the record's content.")
        // if inserting the record to dems was successful, then go ahead and stream the data to the record.
        .process(new Processor() {
          @Override
          public void process(Exchange ex) {
           
            FileNote fileNoteDoc = ex.getProperty("file_note", FileNote.class);
            ex.getMessage().setBody(fileNoteDoc);
            String caseId = (String)ex.getProperty("caseId", String.class);
            String recordId = (String)ex.getProperty("recordId", String.class);

            ex.getMessage().setHeader("caseId", caseId);
            ex.getMessage().setHeader("recordId", recordId);
          }

        })
        .marshal().json(JsonLibrary.Jackson, FileNote.class)
        //.log(LoggingLevel.INFO,"Sending file note record: ${body}")

        // proceed to create record in dems, base on the caseid
        .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
       .to("direct:streamNoteRecord")
      .endChoice()
    .end()
    .log(LoggingLevel.INFO, "end of processNoteRecord")
    ;
  }

  private void streamNoteRecord() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"Processing request streamNoteRecord: ${body}")
    .setProperty("NoteRecord", simple("${bodyAs(String)}"))
    .setProperty("dems_case_id", simple("${headers[caseId]}"))
    .setProperty("dems_record_id", simple("${headers[recordId]}"))
    .removeHeader(Exchange.CONTENT_TYPE)
    
    //.log(LoggingLevel.INFO,"dems_case_id: ${exchangeProperty.dems_case_id}")
    //.log(LoggingLevel.INFO,"dems_record_id: ${exchangeProperty.dems_record_id}")
    .unmarshal().json(JsonLibrary.Jackson, FileNote.class)
    .setHeader(Exchange.CONTENT_TYPE, constant("multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW"))
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception{
        FileNote b = exchange.getIn().getBody(FileNote.class);
        byte[] decodedBytes = b.getNote_txt().getBytes(StandardCharsets.UTF_8);
        exchange.getIn().setBody(decodedBytes);
      
        String fileName = "notes.txt";
        String boundary = "simpleboundary";
        String multipartHeader = "--" + boundary + "\r\n" + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" + "Content-Type: application/octet-stream\r\n" + "\r\n";
        String multipartFooter = "\r\n" + "--" + boundary + "--";
        byte[] headerBytes = multipartHeader.getBytes(StandardCharsets.UTF_8);
        byte[] footerBytes = multipartFooter.getBytes(StandardCharsets.UTF_8);
        byte[] multipartBody = new byte[headerBytes.length + decodedBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, multipartBody, 0, headerBytes.length);
        System.arraycopy(decodedBytes, 0, multipartBody, headerBytes.length, decodedBytes.length);
        System.arraycopy(footerBytes, 0, multipartBody, headerBytes.length + decodedBytes.length, footerBytes.length);
        //exchange.getMessage().setHeader("Content-Disposition", new ValueBuilder(simple("form-data; name=\"file\"; filename=\"${header.CamelFileName}\"")));
        exchange.getMessage().setHeader("Content-Disposition", "form-data; name=\"file\"; filename=\"" + fileName + "\"");
       // exchange.getMessage().setHeader("CamelHttpMethod", constant("PUT"));
        String boundryString = "multipart/form-data;boundary=" + boundary;
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "multipart/form-data;boundary=" + boundary);
        exchange.setProperty("contentType", boundryString);
        exchange.setProperty("multipartBody", multipartBody);
        exchange.getMessage().setBody(multipartBody);
      }
    })
    .to("direct:streamCaseRecordNative")
   .log(LoggingLevel.INFO,"DEMS case record File Note uploaded.")
    .end();
  }

  private void deleteJustinFileNoteRecord() {
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
      .setProperty("edtId",jsonpath("$.edtID"))
      .setProperty("recordSource",jsonpath("$.cc_Source"))
      .setProperty("recordSourceText",jsonpath("$.cc_Source_Text"))
     // .log(LoggingLevel.INFO,"Body: ${body}")
      .doTry()
      .choice()
          .when(simple("${exchangeProperty.recordSource} !contains 'BCPS Work' && ${exchangeProperty.recordSourceText} !contains 'BCPS Work'"))
            // As per BCPSDEMS-415, only delete the native/pdf, leave the metadata
            .process(new Processor() {
              @Override
              public void process(Exchange exchange) throws Exception{
                byte[] decodedBytes = null;
                exchange.getIn().setBody(decodedBytes);
                String fileName = "deleted.txt";
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
  .end();
  }

  private void updateExistingCaseFileNotes() {
     // use method name as route id
     String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
     // IN: header = id
     from("platform-http:/" + routeId + "?httpMethodRestrict=PUT" )
     .routeId(routeId)
     .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
     .log(LoggingLevel.INFO,"updateExistingCaseFileNotes... pages: ${header.pageFrom} -> ${header.pageTo}")
     //.setProperty("pageFrom", header("pageFrom"))
     //.setProperty("pageTo", header("pageTo"))
 
     .setProperty("v2DemsHost", simple("{{dems.host}}"))
     /*.process(new Processor() {
       @Override
       public void process(Exchange exchange) throws Exception {
         String v2DemsHost = (String)exchange.getProperty("v2DemsHost");
         v2DemsHost = v2DemsHost.replace("/v1", "/v2");
         exchange.setProperty("v2DemsHost", v2DemsHost);
       }
     })*/
     .log(LoggingLevel.INFO, "New URL: ${exchangeProperty.v2DemsHost}")
 
     .setProperty("pageSize").simple("500")
     .setProperty("maxRecordIncrements").simple("75")
     .setProperty("incrementCount").simple("1")
     .setProperty("continueLoop").simple("true")
 
     .choice()
       .when(simple("${header.pageFrom} != null && ${header.pageFrom} != ''"))
         .setProperty("incrementCount").simple("${header.pageFrom}")
       .endChoice()
     .end()
 
     .choice()
       .when(simple("${header.pageTo} != null && ${header.pageTo} != ''"))
         .setProperty("maxRecordIncrements").simple("${header.pageTo}")
       .endChoice()
     .end()
 
     // limit the number of times incremented to 250.
     .loopDoWhile(simple("${exchangeProperty.continueLoop} == 'true' && ${exchangeProperty.incrementCount} <= ${exchangeProperty.maxRecordIncrements}"))
       .log(LoggingLevel.INFO, "\n\nViewing page: ${exchangeProperty.incrementCount}")
 
       .removeHeader("CamelHttpUri")
       .removeHeader("CamelHttpBaseUri")
       .removeHeaders("CamelHttp*")
       .setHeader(Exchange.HTTP_METHOD, simple("GET"))
       .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
       .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
       //traverse through all cases in DEMS
       .toD("https://${exchangeProperty.v2DemsHost}/org-units/{{dems.org-unit.id}}/cases/list?page=${exchangeProperty.incrementCount}&pagesize=${exchangeProperty.pageSize}")
       //.log(LoggingLevel.DEBUG,"Person list: '${body}'")
       .setProperty("length",jsonpath("$.items.length()"))
       .log(LoggingLevel.INFO,"Case count: ${exchangeProperty.length}")
 
       .choice()
         .when(simple("${exchangeProperty.length} < 1"))
           .log(LoggingLevel.INFO, "End of pages.")
           .setProperty("continueLoop").simple("false")
         .endChoice()
         .when(simple("${header.CamelHttpResponseCode} == 200"))
           .to("direct:processCaseList")
         .endChoice()
       .end()
 
       // increment the loop count.
       .process(new Processor() {
         @Override
         public void process(Exchange ex) {
           Integer incrementCount = (Integer)ex.getProperty("incrementCount", Integer.class);
           incrementCount++;
           ex.setProperty("incrementCount", incrementCount);
         }
       })
     .end() // end loop
     .log(LoggingLevel.INFO,"end of updateExistingCaseFileNotes.")
     ;
  }

  private void inactivateActiveReturnedCases() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId + "?httpMethodRestrict=PUT" )
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"inactivateActiveReturnedCases...")

    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    //traverse through all cases in DEMS
    .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/cases/RCC Status:Return/id")
    .split()
      .jsonpathWriteAsString("$.*")
      .setProperty("caseId",jsonpath("$.id"))
      .setProperty("caseKey",jsonpath("$.key"))
      .setProperty("status",jsonpath("$.status"))

      .choice()
        .when(simple("${exchangeProperty.status} == 'Active'"))
          .log(LoggingLevel.INFO, "Active Returned Case: ${exchangeProperty.caseId} Key: ${exchangeProperty.caseKey} Status: ${exchangeProperty.status}")
          .choice()
            .when(simple("${header.testMode} != 'true'"))

              .log(LoggingLevel.INFO, "Inactivate case")
              // inactivate the case.
              .setProperty("id", simple("${exchangeProperty.caseId}"))
              .to("direct:getCourtCaseStatusById")
              .setProperty("caseName",jsonpath("$.name"))
              .setProperty("rccId",jsonpath("$.key"))

              .setBody(simple("{\"name\": \"${exchangeProperty.caseName}\",\"key\": \"${exchangeProperty.rccId}\",\"status\": \"Inactive\"}"))

              .removeHeader("CamelHttpUri")
              .removeHeader("CamelHttpBaseUri")
              .removeHeaders("CamelHttp*")
              .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
              .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
              .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
              .toD("https://{{dems.host}}/cases/${exchangeProperty.caseId}")
              .log(LoggingLevel.INFO, "Case inactivated.")

            .endChoice()
          .log(LoggingLevel.INFO, "--------------------------------------------------")
        .endChoice()
      .end()
    .end()

    .log(LoggingLevel.INFO,"end of inactivateActiveReturnedCases.")
    ;
  }

}