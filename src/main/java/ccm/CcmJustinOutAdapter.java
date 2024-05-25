package ccm;
// To run this integration use:
// kamel run CcmJustinAdapter.java --property file:application.properties --profile openshift
//
// recover the service location. If you're running on minikube, minikube service platform-http-server --url=true
// curl -d '{}' http://ccm-justin-adapter/courtFileCreated
//

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Base64;

import org.apache.camel.CamelException;

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
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.http.NoHttpResponseException;

import ccm.models.common.data.AuthUserList;
import ccm.models.common.data.CaseAppearanceSummaryList;
import ccm.models.common.data.CaseCrownAssignmentList;
import ccm.models.common.data.ChargeAssessmentData;
import ccm.models.common.data.CourtCaseData;
import ccm.models.common.data.FileCloseData;
import ccm.models.common.data.document.ReportDocumentList;
import ccm.models.common.event.BaseEvent;
import ccm.models.common.event.Error;
import ccm.models.common.event.EventKPI;
import ccm.models.common.versioning.Version;
import ccm.models.system.justin.JustinAgencyFile;
import ccm.models.system.justin.JustinAuthUsersList;
import ccm.models.system.justin.JustinCourtAppearanceSummaryList;
import ccm.models.system.justin.JustinCourtFile;
import ccm.models.system.justin.JustinCrownAssignmentList;
import ccm.models.system.justin.JustinDocumentList;
import ccm.models.system.justin.JustinFileClose;
import ccm.utils.DateTimeUtils;

public class CcmJustinOutAdapter extends RouteBuilder {
  @Override
  public void configure() throws Exception {
    attachExceptionHandlers();

    version();

    courtFileCreated();
    healthCheck();
    getCourtCaseDetails();
    getCourtCaseAuthList();
    getCourtCaseMetadata();
    getCourtCaseAppearanceSummaryList();
    getCourtCaseCrownAssignmentList();
    getImageData();
    getFileCloseData();

  }

  private void attachExceptionHandlers() {

    // handle network connectivity errors
    onException(ConnectException.class, SocketTimeoutException.class)
      .maximumRedeliveries(10).redeliveryDelay(45000)
      .backOffMultiplier(2)
      .log(LoggingLevel.ERROR,"onException(ConnectException, SocketTimeoutException) called.")
      .setBody(constant("An unexpected network error occurred"))
      .retryAttemptedLogLevel(LoggingLevel.ERROR)
      .handled(false)
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
    .maximumRedeliveries(3).redeliveryDelay(20000)
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
             error.setError_details(cause.getResponseBody());

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
        .log(LoggingLevel.INFO, "Request body: ${body}")
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

    .end();

    onException(CamelException.class)
    .choice()
      .when(simple("${exchangeProperty.kpi_event_object} != null"))
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
          BaseEvent event = (BaseEvent)exchange.getProperty("kpi_event_object");
          Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
          Error error = new Error();
          error.setError_dtm(DateTimeUtils.generateCurrentDtm());

          error.setError_summary("Unable to process event, CamelException raised.");
          error.setError_details(cause.getMessage());
          log.debug("HttpOperationException caught, exception message : " + cause.getMessage() + " stack trace : " + cause.getStackTrace());
          log.error("HttpOperation Exception event info : " + event.getEvent_source());

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
        .log(LoggingLevel.DEBUG,"Derived event KPI published.")
        .log("Caught CamelException exception")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
        .setProperty("error_event_object", body())
        .to("kafka:{{kafka.topic.kpis.name}}")
        .endChoice()
      .otherwise()
        .log(LoggingLevel.ERROR, "Camel Exception thrown: ${exception.message}")
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
          Throwable caused = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);

          Error error = new Error();
          error.setError_dtm(DateTimeUtils.generateCurrentDtm());
          error.setError_summary("Unable to process event, general exception raised.");
          error.setError_code("General Exception");
          error.setError_details(caused.getMessage());

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
        .log(LoggingLevel.DEBUG,"Derived event KPI published.")
        .log("Caught General exception exception")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
        .setProperty("error_event_object", body())
        .to("kafka:{{kafka.topic.kpis.name}}")
        .endChoice()
      .otherwise()
        .log(LoggingLevel.ERROR, "General Exception thrown: ${exception.message}")
        .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("500"))
        .setBody(simple("{\"error\": \"${exception.message}\"}"))
        .transform().simple("Error reported: ${exception.message} - cannot process this message.")
        .setHeader(Exchange.HTTP_RESPONSE_TEXT, simple("{\"error\": \"${exception.message}\"}"))
        .setHeader("CCMException", simple("{\"error\": \"${exception.message}\"}"))
        .end()
    .end();

  }

  private void version() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header = id
    from("platform-http:/" + routeId + "?httpMethodRestrict=GET")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        exchange.getMessage().setBody(Version.V1_0.toString());
      }
    })
    ;
  }

  private void courtFileCreated() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId + "?httpMethodRestrict=POST")
    .routeId(routeId)
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .log(LoggingLevel.DEBUG,"body (before unmarshalling): '${body}'")
    .unmarshal().json()
    .transform(simple("{\"number\": \"${body[number]}\", \"status\": \"created\", \"sensitive_content\": \"${body[sensitive_content]}\", \"public_content\": \"${body[public_content]}\", \"created_datetime\": \"${body[created_datetime]}\"}"))
    .log(LoggingLevel.DEBUG,"body (after unmarshalling): '${body}'")
    .to("kafka:{{kafka.topic.chargeassessments.name}}");
  }

  private void healthCheck() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/v1/health?httpMethodRestrict=GET")
    .routeId(routeId)
    .removeHeaders("CamelHttp*")
    .log(LoggingLevel.DEBUG,"/v1/health request received")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{justin.token}}")
    .to("https://{{justin.host}}/health");

  }

  private void getCourtCaseDetails() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId + "?httpMethodRestrict=GET")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"getCourtCaseDetails request received. number = ${header[number]}")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{justin.token}}")
    .removeHeader("rcc_id")
    .toD("https://{{justin.host}}/agencyFile?rcc_id=${header[number]}")
    .log(LoggingLevel.DEBUG,"Received response from JUSTIN: '${body}'")
    .unmarshal().json(JsonLibrary.Jackson, JustinAgencyFile.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        JustinAgencyFile j = exchange.getIn().getBody(JustinAgencyFile.class);
        ChargeAssessmentData b = new ChargeAssessmentData(j);
        exchange.getMessage().setBody(b, ChargeAssessmentData.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, ChargeAssessmentData.class)
    .log(LoggingLevel.DEBUG,"Converted response (from JUSTIN to Business model): '${body}'")
    ;
  }

  private void getCourtCaseAuthList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId + "?httpMethodRestrict=GET")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"getCourtCaseAuthList request received. rcc_id = ${header.number}")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{justin.token}}")
    .toD("https://{{justin.host}}/authUsers?rcc_id=${header.number}")
    .log(LoggingLevel.DEBUG,"Received response from JUSTIN: '${body}'")
    .unmarshal().json(JsonLibrary.Jackson, JustinAuthUsersList.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        JustinAuthUsersList j = exchange.getIn().getBody(JustinAuthUsersList.class);
        AuthUserList b = new AuthUserList(j);
        exchange.getMessage().setBody(b, AuthUserList.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, AuthUserList.class)
    .log(LoggingLevel.DEBUG,"Converted response (from JUSTIN to Business model): '${body}'")
    ;
  }

  private void getCourtCaseAppearanceSummaryList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"getCourtCaseAppearanceSummaryList request received. mdoc_no = ${header.number}")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{justin.token}}")
    .toD("https://{{justin.host}}/apprSummary?mdoc_justin_no=${header.number}")
    .log(LoggingLevel.DEBUG,"Received response from JUSTIN: '${body}'")
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
    .log(LoggingLevel.DEBUG,"Converted response (from JUSTIN to Business model): '${body}'")
    ;
  }

  private void getCourtCaseMetadata() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"getCourtCaseMetadata request received. mdoc_no = ${header.number}")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{justin.token}}")
    .toD("https://{{justin.host}}/courtFile?mdoc_justin_no=${header.number}")
    .log(LoggingLevel.DEBUG,"Received response from JUSTIN: '${body}'")
    .unmarshal().json(JsonLibrary.Jackson, JustinCourtFile.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        JustinCourtFile j = exchange.getIn().getBody(JustinCourtFile.class);
        CourtCaseData b = new CourtCaseData(j);
        exchange.getMessage().setBody(b, CourtCaseData.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, CourtCaseData.class)
    .log(LoggingLevel.DEBUG,"Converted response (from JUSTIN to Business model): '${body}'")
    ;
  }

  private void getCourtCaseCrownAssignmentList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"getCourtCaseCrownAssignmentList request received. mdoc_no = ${header.number}")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{justin.token}}")
    .toD("https://{{justin.host}}/crownAssignments?mdoc_justin_no=${header.number}")
    .log(LoggingLevel.DEBUG,"Received response from JUSTIN: '${body}'")
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
    .log(LoggingLevel.DEBUG,"Converted response (from JUSTIN to Business model): '${body}'")
    ;
  }

  private void getImageData() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"getImageData request received.")
    .log(LoggingLevel.DEBUG,"Request to justin: '${body}'")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{justin.token}}")
    .toD("https://{{justin.host}}/imageDataGet")
    .log(LoggingLevel.DEBUG,"Received response from JUSTIN: '${body}'")
    .unmarshal().json(JsonLibrary.Jackson, JustinDocumentList.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        JustinDocumentList j = exchange.getIn().getBody(JustinDocumentList.class);
        ReportDocumentList rd = new ReportDocumentList(j);
        exchange.getMessage().setBody(rd, ReportDocumentList.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, ReportDocumentList.class)
    .log(LoggingLevel.DEBUG,"Converted response (from JUSTIN to Business model): '${body}'")
    ;
  }

  private void getFileCloseData() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"file close request received.")
    .log(LoggingLevel.DEBUG,"Request to justin: '${body}'")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{justin.token}}")
    .log(LoggingLevel.INFO, "LookupService calling Justin file close")
    .toD("https://{{justin.host}}/fileClose?mdoc_justin_no=${header.number}")
    .log(LoggingLevel.DEBUG,"Received response from JUSTIN: '${body}'")
    
    .unmarshal().json(JsonLibrary.Jackson, JustinFileClose.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        JustinFileClose j = exchange.getIn().getBody(JustinFileClose.class);
        FileCloseData fileCloseData = new FileCloseData(j.getMdoc_justin_no(),j.getRms_event_type(), j.getRms_event_date());
       exchange.getMessage().setBody(fileCloseData);
      }
    })
    .marshal().json(JsonLibrary.Jackson, FileCloseData.class)
    .log(LoggingLevel.DEBUG,"Converted response (from JUSTIN to Business model): '${body}'")
    ;

  }

}