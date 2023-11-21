package ccm;

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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.apache.camel.CamelException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.model.dataformat.JsonLibrary;

import com.fasterxml.jackson.databind.ObjectMapper;

import ccm.models.common.data.ChargeAssessmentData;
import ccm.models.common.data.ChargeAssessmentDataRef;
import ccm.models.common.data.CourtCaseData;
import ccm.models.common.data.document.ChargeAssessmentDocumentData;
import ccm.models.common.data.document.CourtCaseDocumentData;
import ccm.models.common.data.document.ImageDocumentData;
import ccm.models.common.data.document.ReportDocument;
import ccm.models.common.event.BaseEvent;
import ccm.models.common.event.EventKPI;
import ccm.models.common.event.ReportEvent;
import ccm.models.system.dems.DemsRecordData;
import ccm.models.system.justin.JustinDocumentKeyList;
import ccm.utils.DateTimeUtils;
import ccm.models.common.event.Error;

public class CcmReportsProcessor extends RouteBuilder {

  private static int POOL_SIZE = 3;

    @Override
    public void configure() throws Exception {
      attachExceptionHandlers(); 
      processReportEvents();
       publishEventKPI();
       processUnknownStatus();
       processNonStaticDocuments();
       processDocumentRecord();
       
    }

     private void attachExceptionHandlers() {


    // handle network connectivity errors
    onException(ConnectException.class, SocketTimeoutException.class)
      .maximumRedeliveries(3).redeliveryDelay(10000)
      .log(LoggingLevel.ERROR,"onException(ConnectException, SocketTimeoutException) called.")
      .setBody(constant("An unexpected network error occurred"))
      .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("500"))
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
    private void processReportEvents() {
        // use method name as route id
        String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    
        from("kafka:{{kafka.topic.reports.name}}?groupId=ccm-reports-processor&maxPollRecords=30&maxPollIntervalMs=600000")
        .routeId(routeId)
        .threads(POOL_SIZE)
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
        .log(LoggingLevel.INFO, "rcc_id = ${header[rcc_id]} part_id = ${header[part_id]} mdoc_justin_no = ${header[mdoc_justin_no]} rcc_ids = ${header[rcc_ids]} image_id = ${header[image_id]} filtered_yn = ${header[filtered_yn]}")
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

    // For cases like witness statement, there can be multiple docs returned.
    // This will split through each of the documents and process them individually.
    .log(LoggingLevel.INFO,"Parsing through report documents of count: ${exchangeProperty.length}")
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
          .to("http://ccm-dems-adapter/getCourtCaseStatusExists")
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

          .to("http://ccm-dems-adapter/createDocumentRecordHttp")
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
              .to("http://cc-dems-adapter/getCourtCaseStatusExists")
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
            .doCatch(Exception.class)
              .log(LoggingLevel.ERROR,"General Exception thrown.")
              .log(LoggingLevel.ERROR,"${exception}")
            .end()
          .end()

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
            .to("http://ccm-dems-adapter/changeDocumentRecordHttp")
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
          .to("http://ccm-dems-adapter/getCourtCaseStatusExists")
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
          .to("http://ccm-dems-adapter/getCaseRecordImageExistsByKeyHttp")
          .unmarshal().json()
          .choice()
            .when(simple("${body[id]} == ''"))
              // BCPSDEMS-1141 - Send all INFORMATION docs. If there is a doc id collision, increment.
              .setProperty("maxRecordIncrements").simple("10")
              .to("http://ccm-dems-adapter/checkIncrementRecordDocIdHttp")
              // set back body to dems record
              .setBody(simple("${exchangeProperty.dems_record}"))
              .log(LoggingLevel.DEBUG, "${body}")
              .marshal().json(JsonLibrary.Jackson, DemsRecordData.class)
              .to("http://ccm-dems-adapter/createDocumentRecordHttp")
            .endChoice()
            .otherwise()
              .log(LoggingLevel.WARN, "Skipped adding image document.  One with image id already exists.")
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
        .to("http://ccm-dems-adapter/changeDocumentRecordHttp")
      .endChoice()
    .end()

   .log(LoggingLevel.INFO, "end of processNonStaticDocuments")
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
