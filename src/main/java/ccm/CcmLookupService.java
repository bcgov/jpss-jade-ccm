package ccm;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

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

import java.util.Calendar;

import org.apache.camel.CamelException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;

import ccm.models.common.event.BaseEvent;
import ccm.models.common.event.EventKPI;
import ccm.utils.DateTimeUtils;

public class CcmLookupService extends RouteBuilder {
  @Override
  public void configure() throws Exception {

    attachExceptionHandlers();
    getCourtCaseDetails_old();
    getCourtCaseExists();
    getCourtCaseDetails();
    getCourtCaseAuthList();
    getCourtCaseMetadata();
    getCourtCaseAppearanceSummaryList();
    getCourtCaseCrownAssignmentList();
    getPersonExists();
    getCaseListByUserKey();
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
       Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
       ccm.models.common.event.Error error = new ccm.models.common.event.Error();
       error.setError_dtm(DateTimeUtils.generateCurrentDtm());
       error.setError_code("HttpOperationFailed");
       error.setError_summary("Unable to process event.HttpOperationFailed exception raised");

       log.debug("HttpOperationException caught, exception message : " + cause.getMessage() + " stack trace : " + cause.getStackTrace());
       log.error("HttpOperation Exception event info : " + event.getEvent_source());
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
   .log("Caught HttpOperationFailed exception")
   .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
   .setProperty("error_event_object", body())
   .handled(true)
   .to("kafka:{{kafka.topic.kpis.name}}")
   .end();

   // Camel Exception
   onException(CamelException.class)
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
   .to("kafka:{{kafka.topic.general-errors.name}}")
   .handled(true)
   .end();

   // General Exception
    onException(Exception.class)
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
       error.setError_details(event);
      
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
   .handled(true)
   .end();

 }
 
  private void getCourtCaseDetails_old() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/getCourtCaseDetails_old?httpMethodRestrict=GET")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .process(new Processor() {
      public void process(Exchange ex) {
        // https://stackoverflow.com/questions/12008472/get-and-format-yesterdays-date-in-camels-expression-language
        Calendar createdCal = Calendar.getInstance();
        createdCal.add(Calendar.DATE, 0);
        ex.getIn().setHeader("audit_datetime", createdCal.getTime());
      }
    })
    .transform(simple("{\"audit_type\": \"get_court_case_details\", \"user_id\": \"${header.user_id}\", \"court_case_number\": \"${header.court_case_number}\", \"audit_datetime\": \"${header.audit_datetime}\"}"))
    .log(LoggingLevel.DEBUG,"body (after transform): '${body}'")
    .to("kafka:{{kafka.topic.name}}")
    ;
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
    .to("http://ccm-justin-adapter/getCourtCaseDetails")
    .log(LoggingLevel.DEBUG,"response from JUSTIN: ${body}")
    ;
  }

  private void getCourtCaseAuthList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .log(LoggingLevel.DEBUG,"Processing getCourtCaseAuthList request... number = ${header[number]}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://ccm-justin-adapter/getCourtCaseAuthList")
    .log(LoggingLevel.DEBUG,"response from JUSTIN: ${body}")
    ;
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
    .to("http://ccm-justin-adapter/getCourtCaseMetadata")
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
    .to("http://ccm-justin-adapter/getCourtCaseAppearanceSummaryList")
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
    .to("http://ccm-justin-adapter/getCourtCaseCrownAssignmentList")
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
}