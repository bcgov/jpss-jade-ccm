package ccm;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

// To run this integration use:
// kamel run CcmLookupService.java --property file:application.properties --profile openshift
//
// curl -H "user_id: 2" -H "court_case_number: 6" http://ccm-lookup-service/getCourtCaseDetails
//

// camel-k: language=java
// camel-k: dependency=mvn:org.apache.camel.quarkus
// camel-k: dependency=mvn:org.apache.camel.camel-quarkus-kafka
// camel-k: dependency=mvn:org.apache.camel.camel-quarkus-jsonpath
// camel-k: dependency=mvn:org.apache.camel.camel-jackson
// camel-k: dependency=mvn:org.apache.camel.camel-splunk-hec
// camel-k: dependency=mvn:org.apache.camel.camel-http
// camel-k: dependency=mvn:org.apache.camel.camel-http-common

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.HttpHostConnectException;

import com.fasterxml.jackson.databind.ObjectMapper;

import ccm.models.common.data.AuthUserList;
import ccm.models.common.event.BaseEvent;
import ccm.models.common.event.EventKPI;
import ccm.models.common.event.Error;
import ccm.utils.DateTimeUtils;

public class CcmLookupService extends RouteBuilder {
  @Override
  public void configure() throws Exception {

    attachExceptionHandlers();
    getCourtCaseExists();
    getCourtCaseStatusExists();
    getCourtCaseDetails();
    getCourtCaseAuthList();
    getCourtCaseJustinAuthList();
    getCourtCasePidpAuthList();
    getCourtCaseMetadata();
    getCourtCaseAppearanceSummaryList();
    getCourtCaseCrownAssignmentList();
    getImageData();
    getPersonExists();
    getCaseListByUserKey();
    getCaseHyperlink();
    getCaseListHyperlink();
    getFileDisp();
    getFileNote();
    getFileCloseData();
    getAgencyFileStatus();
    getPrimaryCourtCaseExists();

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
        .log(LoggingLevel.ERROR, "HttpOperationFailedException: ${exception.message}")
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            try {
              HttpOperationFailedException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);

              exchange.getMessage().setBody(cause.getResponseBody());
              log.error("Returned body : " + cause.getResponseBody());
              log.error("Returned headers : " + cause.getResponseHeaders());
              log.error("CCMException headers : " + cause.getResponseHeaders().get("CCMException"));
              exchange.getMessage().setHeader("CCMException", cause.getResponseHeaders().get("CCMException"));
              exchange.getMessage().setHeader("CCMExceptionEncoded", cause.getResponseHeaders().get("CCMExceptionEncoded"));
            } catch(Exception ex) {
              ex.printStackTrace();
            }
          }
        })
        .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${exception.statusCode}"))
      .end()

    .end();

    // Camel Exception
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

            log.debug("Camel caught, exception message : " + cause.getMessage() + " stack trace : " + cause.getStackTrace());
            log.error("Camel Exception event info : " + event.getEvent_source());

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
        .log(LoggingLevel.DEBUG,"Derived event KPI published.")
        .log("Caught CamelException exception")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
        .setProperty("error_event_object", body())
        .to("kafka:{{kafka.topic.kpis.name}}")
        .endChoice()
      .otherwise()
        .log(LoggingLevel.ERROR, "CamelException: ${exception.message}")
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
            error.setError_summary("Unable to process event., general Exception raised.");
            error.setError_code("General Exception");
            error.setError_details(cause.getMessage());

            log.debug("General Exception caught, exception message : " + cause.getMessage() + " stack trace : " + cause.getStackTrace());
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
        .log(LoggingLevel.DEBUG,"Derived event KPI published.")
        .log("Caught General exception exception")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
        .setProperty("error_event_object", body())
        .to("kafka:{{kafka.topic.general-errors.name}}")
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

  private void getCourtCaseExists() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: header.number

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    //.setProperty("name",simple("${header[number]}"))
    .log(LoggingLevel.DEBUG,"Processing getCourtCaseExists request... number = ${header[number]}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-dems-adapter/getCourtCaseExists")
    .log(LoggingLevel.DEBUG,"Lookup response = '${body}'")
    ;
  }

  private void getCourtCaseStatusExists() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: header.number

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    //.setProperty("name",simple("${header[number]}"))
    .log(LoggingLevel.DEBUG,"Processing getCourtCaseExists request... number = ${header[number]}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-dems-adapter/getCourtCaseStatusExists")
    .log(LoggingLevel.DEBUG,"Lookup response = '${body}'")
    ;
  }

  private void getCourtCaseDetails() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .log(LoggingLevel.DEBUG,"Processing request... number = ${header[number]}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-justin-out-adapter/getCourtCaseDetails")
    .log(LoggingLevel.DEBUG,"response from JUSTIN: ${body}")
    ;
  }

  private void deprecated_getCourtCaseAuthList() {

    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .log(LoggingLevel.DEBUG,"Processing request... number = ${header[number]}")
    .log(LoggingLevel.DEBUG, "Pre-headers: ${headers}")
    .process(new Processor(){
      public void process(Exchange exchange) throws Exception {
        AuthUserList userAuthList = new AuthUserList();

        // set up header
        Map<String,Object> headers = new HashMap<String,Object>();
        headers.put("number", exchange.getIn().getHeader("number"));

        ProducerTemplate prodTemplate = getContext().createProducerTemplate();
        String responseString = prodTemplate.requestBodyAndHeaders(
                                    "http://ccm-pidp-adapter/getCourtCaseAuthList",
                                    null, headers, String.class);

        AuthUserList pdipAuthUserList = null;
        if (responseString != null) {
          log.debug("PIDP List:"+responseString);
          pdipAuthUserList = new ObjectMapper().readValue(responseString, AuthUserList.class);
          if (pdipAuthUserList != null) {
            log.debug("Adding pidpAuthList"+pdipAuthUserList.getAuth_user_list().size());
            userAuthList.getAuth_user_list().addAll(pdipAuthUserList.getAuth_user_list());
          }
        }
        prodTemplate.stop();

        ProducerTemplate justinTemplate = getContext().createProducerTemplate();
        String justinResponse = justinTemplate.requestBodyAndHeaders(
                                   "http://ccm-justin-out-adapter/getCourtCaseAuthList",
                                   null, headers, String.class);

        AuthUserList justinUserList = null;
        if (justinResponse != null) {

          log.debug("JUSTIN List:"+justinResponse);
          justinUserList = new ObjectMapper().readValue(justinResponse, AuthUserList.class);
          if (justinUserList != null) {
            log.debug("Adding justinAuthList"+justinUserList.getAuth_user_list().size());
            userAuthList.getAuth_user_list().addAll(justinUserList.getAuth_user_list());
          }
        }
        justinTemplate.stop();
        exchange.getIn().setBody(userAuthList, AuthUserList.class);
      }
    }).to("mock:result")
    .marshal()
    .json(JsonLibrary.Jackson, AuthUserList.class)
    .log(LoggingLevel.DEBUG, "headers: ${headers}")
    .log(LoggingLevel.DEBUG, "Body: ${body}")
    .end();
  }

  private void getCourtCaseAuthList() {

    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html

    .setProperty("key", simple("${header[number]}"))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .removeHeader("kafka.HEADERS")
    .removeHeaders("x-amz*")
    .log(LoggingLevel.DEBUG,"Processing request... number = ${header[number]}")
    .log(LoggingLevel.DEBUG, "Pre-headers: ${headers}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-justin-out-adapter/getCourtCaseAuthList")
    .unmarshal().json(JsonLibrary.Jackson, AuthUserList.class)
    .setProperty("justinauthlist", body())

    .log(LoggingLevel.DEBUG, "Retreiving list from PIDP...")

    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-pidp-adapter/getCourtCaseAuthList")
    .unmarshal().json(JsonLibrary.Jackson, AuthUserList.class)
    .setProperty("pidpauthlist", body())

    .process(new Processor(){
      public void process(Exchange exchange) throws Exception {
        AuthUserList userAuthList = new AuthUserList();
        String rccId = (String)exchange.getProperty("key", String.class);
        userAuthList.setRcc_id(rccId);
        AuthUserList justinUserList = (AuthUserList)exchange.getProperty("justinauthlist", AuthUserList.class);
        AuthUserList pdipAuthUserList = (AuthUserList)exchange.getProperty("pidpauthlist", AuthUserList.class);

        userAuthList.getAuth_user_list().addAll(justinUserList.getAuth_user_list());
        userAuthList.getAuth_user_list().addAll(pdipAuthUserList.getAuth_user_list());

        exchange.getIn().setBody(userAuthList, AuthUserList.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, AuthUserList.class)
    // remove the pidp token header, as it causes bad requests from JUSTIN side.
    .removeHeaders("pidp*")
    .removeHeaders("Authorization*")
    .removeHeaders("X-*")
    .removeHeaders("Content-Security-Policy")
    .removeHeaders("Referrer-Policy")
    .removeHeaders("set-cookie")
    .removeHeaders("Strict-Transport-Security")
    .removeHeaders("transfer-encoding")
    .log(LoggingLevel.DEBUG, "headers: ${headers}")
    .log(LoggingLevel.DEBUG, "Body: ${body}")
    .end();
  }

  private void getCourtCaseJustinAuthList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html

    .setProperty("key", simple("${header[number]}"))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .removeHeader("kafka.HEADERS")
    .removeHeaders("x-amz*")
    .log(LoggingLevel.DEBUG,"Processing request... number = ${header[number]}")
    .log(LoggingLevel.DEBUG, "Pre-headers: ${headers}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-justin-out-adapter/getCourtCaseAuthList")
    .unmarshal().json(JsonLibrary.Jackson, AuthUserList.class)
    .setProperty("justinauthlist", body())

    .process(new Processor(){
      public void process(Exchange exchange) throws Exception {
        AuthUserList userAuthList = new AuthUserList();
        String rccId = (String)exchange.getProperty("key", String.class);
        userAuthList.setRcc_id(rccId);
        AuthUserList justinUserList = (AuthUserList)exchange.getProperty("justinauthlist", AuthUserList.class);
        userAuthList.getAuth_user_list().addAll(justinUserList.getAuth_user_list());

        exchange.getIn().setBody(userAuthList, AuthUserList.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, AuthUserList.class)
    // remove the pidp token header, as it causes bad requests from JUSTIN side.
    .removeHeaders("pidp*")
    .removeHeaders("Authorization*")
    .removeHeaders("X-*")
    .removeHeaders("Content-Security-Policy")
    .removeHeaders("Referrer-Policy")
    .removeHeaders("set-cookie")
    .removeHeaders("Strict-Transport-Security")
    .removeHeaders("transfer-encoding")
    .log(LoggingLevel.DEBUG, "headers: ${headers}")
    .log(LoggingLevel.DEBUG, "Body: ${body}")
    .end();
  }

  private void getCourtCasePidpAuthList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html

    .setProperty("key", simple("${header[number]}"))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .removeHeader("kafka.HEADERS")
    .removeHeaders("x-amz*")

    .log(LoggingLevel.DEBUG, "Retreiving list from PIDP...")

    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-pidp-adapter/getCourtCaseAuthList")
    .unmarshal().json(JsonLibrary.Jackson, AuthUserList.class)
    .setProperty("pidpauthlist", body())

    .process(new Processor(){
      public void process(Exchange exchange) throws Exception {
        AuthUserList userAuthList = new AuthUserList();
        String rccId = (String)exchange.getProperty("key", String.class);
        userAuthList.setRcc_id(rccId);
        AuthUserList pdipAuthUserList = (AuthUserList)exchange.getProperty("pidpauthlist", AuthUserList.class);
        userAuthList.getAuth_user_list().addAll(pdipAuthUserList.getAuth_user_list());

        exchange.getIn().setBody(userAuthList, AuthUserList.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, AuthUserList.class)
    // remove the pidp token header, as it causes bad requests from JUSTIN side.
    .removeHeaders("pidp*")
    .removeHeaders("Authorization*")
    .removeHeaders("X-*")
    .removeHeaders("Content-Security-Policy")
    .removeHeaders("Referrer-Policy")
    .removeHeaders("set-cookie")
    .removeHeaders("Strict-Transport-Security")
    .removeHeaders("transfer-encoding")
    .log(LoggingLevel.DEBUG, "headers: ${headers}")
    .log(LoggingLevel.DEBUG, "Body: ${body}")
    .end();
  }

  private void getCourtCaseMetadata() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .log(LoggingLevel.DEBUG,"Processing request... number = ${header[number]}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-justin-out-adapter/getCourtCaseMetadata")
    .log(LoggingLevel.DEBUG,"response from JUSTIN: ${body}")
    ;
  }

  private void getCourtCaseAppearanceSummaryList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .log(LoggingLevel.DEBUG,"Processing request... number = ${header[number]}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-justin-out-adapter/getCourtCaseAppearanceSummaryList")
    .log(LoggingLevel.DEBUG,"response from JUSTIN: ${body}")
    ;
  }

  private void getCourtCaseCrownAssignmentList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .log(LoggingLevel.DEBUG,"Processing request... number = ${header[number]}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-justin-out-adapter/getCourtCaseCrownAssignmentList")
    .log(LoggingLevel.DEBUG,"response from JUSTIN: ${body}")
    ;
  }

  private void getImageData() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .log(LoggingLevel.DEBUG,"Sending to JUSTIN: ${body}")
    .setProperty("image_request", body())
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-justin-out-adapter/getImageData")
    .log(LoggingLevel.DEBUG,"response from JUSTIN: ${body}")
    ;
  }

  private void getPersonExists() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .log(LoggingLevel.DEBUG,"Processing request... key = ${header[key]}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-dems-adapter/getPersonExists")
    .log(LoggingLevel.DEBUG,"Lookup response = '${body}'")
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
    .log(LoggingLevel.DEBUG,"Looking up case list by user key (${header.key}) ...")
    .to("http://ccm-dems-adapter/getCaseListByUserKey?throwExceptionOnFailure=false")
    .choice()
      .when().simple("${header.CamelHttpResponseCode} == 200")
        .log(LoggingLevel.DEBUG,"User found.")
        .endChoice()
      .when().simple("${header.CamelHttpResponseCode} == 404")
        .log(LoggingLevel.DEBUG,"User not found.  Error message from DEMS: ${body}")
        .endChoice()
    .end()
    ;
  }

  private void getCaseHyperlink() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.key
    // OUT: body as CaseHyperlinkData

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    //.setProperty("name",simple("${header[number]}"))
    .log(LoggingLevel.DEBUG,"Processing getCourtCaseExists request... key = ${header[key]}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))

    // attempt to retrieve case id using getCaseHyperlink DEMS adapter endpoint.
    .doTry()
      .to("http://ccm-dems-adapter/getCaseHyperlink")
      .endDoTry()
    .doCatch(HttpOperationFailedException.class)
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
          HttpOperationFailedException exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);
          exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, exception.getStatusCode());
          exchange.getMessage().setBody(exception.getResponseBody());
        }
      })
      .stop()
    .end()
    ;
  }

  //as part of jade 2425
  private void getCaseListHyperlink() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .log(LoggingLevel.DEBUG,"Processing getCaseListHyperlink request... key = ${body}")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setProperty("body_request", body())
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    // attempt to retrieve case id using getCaseListHyperlink DEMS adapter endpoint.
    .doTry()
      .to("http://ccm-dems-adapter/getCaseListHyperlink")
    .endDoTry()
    .doCatch(HttpOperationFailedException.class)
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .process(new Processor() {
       @Override
       public void process(Exchange exchange) throws Exception {
         HttpOperationFailedException exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);
         exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, exception.getStatusCode());
         exchange.getMessage().setBody(exception.getResponseBody());
       }
     })
     .stop()
    .end()
    ;
  }

  private void getFileCloseData() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .log(LoggingLevel.DEBUG,"Processing request... number = ${header[number]}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-justin-out-adapter/getFileCloseData")
    .log(LoggingLevel.DEBUG,"response from JUSTIN: ${body}")
    ;
  }

  private void getFileDisp() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .log(LoggingLevel.DEBUG,"Processing request... number = ${header[number]}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-justin-out-adapter/getFileDisp")
    .log(LoggingLevel.DEBUG,"response from JUSTIN: ${body}")
    ;
  }

  private void getFileNote() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    
    from("platform-http:/" + routeId)
  
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .log(LoggingLevel.INFO,"Processing request... number = ${header[number]}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-justin-out-adapter/getFileNote")
    .log(LoggingLevel.INFO,"response from JUSTIN: ${body}")
    ;
  }


  private void getAgencyFileStatus() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    
    from("platform-http:/" + routeId)
  
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .log(LoggingLevel.INFO,"Processing request... agencyIdCode = ${header.agencyIdCode} agnecyFileNumber= ${header.agencyFileNumber}}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-justin-out-adapter/getAgencyFileStatus?throwExceptionOnFailure=false")
    .log(LoggingLevel.INFO,"response from JUSTIN: ${body}");
  }

  // need to call dems adapter
  private void getPrimaryCourtCaseExists() {
     // use method name as route id
     String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    
     from("platform-http:/" + routeId)
   
     .routeId(routeId)
     .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
     .removeHeader("CamelHttpUri")
     .removeHeader("CamelHttpBaseUri")
     .removeHeaders("CamelHttp*")
     .log(LoggingLevel.INFO,"Processing request... rcc_id = ${header[rcc_id]}")
     .setHeader(Exchange.HTTP_METHOD, simple("GET"))
     .setHeader("key", simple("${header.rcc_id}"))
     .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
     .to("http://ccm-dems-adapter/getPrimaryCourtCaseExists")
     .log(LoggingLevel.INFO,"response from JUSTIN: ${body}")
     ;

  }
 
}