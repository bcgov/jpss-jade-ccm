package ccm;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import org.apache.camel.CamelException;

// To run this integration use:
// kamel run CcmSplunkAdapter.java --property file:ccmSplunkAdapter.properties --profile openshift
// 

// camel-k: language=java
// camel-k: dependency=mvn:org.apache.camel.quarkus
// camel-k: dependency=mvn:org.apache.camel.camel-quarkus-kafka
// camel-k: dependency=mvn:org.apache.camel.camel-quarkus-jsonpath
// camel-k: dependency=mvn:org.apache.camel.camel-jackson
// camel-k: dependency=mvn:org.apache.camel.camel-splunk-hec
// camel-k: dependency=mvn:org.apache.camel.camel-http
// camel-k: dependency=mvn:org.apache.camel.camel-http-common


import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.http.NoHttpResponseException;

import ccm.models.common.event.Error;
import ccm.models.common.event.BaseEvent;
import ccm.models.common.event.EventKPI;
import ccm.models.system.splunk.SplunkEventLog;
import ccm.utils.DateTimeUtils;


public class CcmSplunkAdapter extends RouteBuilder {
  @Override
  public void configure() throws Exception {
    retryExceptionHandler();
    //attachExceptionHandlers();
    processEventKPIs();
    publishEventKPIToSplunk();
    publishSplunkEventKPIError();
  }

  private void retryExceptionHandler() {
    onException(ConnectException.class, SocketTimeoutException.class, NoHttpResponseException.class, HttpOperationFailedException.class)
    .maximumRedeliveries(5).redeliveryDelay(10000)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        log.error("NoHttpResponseException Exception caught, exception message : " + cause.getMessage());
        log.error("NoHttpResponseException Exception class and local msg : " + cause.getClass().getName() + " message : " + cause.getLocalizedMessage());
        log.error("NoHttpResponseException Exception body: " + exchange.getMessage().getBody());
        for(StackTraceElement trace : cause.getStackTrace())
        {
         log.error(trace.toString());
        }
      }
    })
    .log(LoggingLevel.INFO, "Headers: ${headers}")
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
        log.error("HttpOperationFailedException caught, exception message : " + cause.getMessage());
        log.error("HttpOperationFailedException class and local msg : " + cause.getClass().getName() + " message : " + cause.getLocalizedMessage());
        if(event != null) {
          log.error("HttpOperation Exception event info : " + event.getEvent_source());
        }
        for(StackTraceElement trace : cause.getStackTrace())
        {
         log.error(trace.toString());
        }
      }
    })
    .log(LoggingLevel.INFO, "Headers: ${headers}")
    .maximumRedeliveries(2)
    .redeliveryDelay(1000)
    .backOffMultiplier(2)
    .end();
 

    // General Exception
    onException(Exception.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        log.error("General Exception caught, exception message : " + cause.getMessage());
        log.error("General Exception class and local msg : " + cause.getClass().getName() + " message : " + cause.getLocalizedMessage());
        log.error("General Exception body: " + exchange.getMessage().getBody());
        for(StackTraceElement trace : cause.getStackTrace())
        {
         log.error(trace.toString());
        }
      }
    })
    .log(LoggingLevel.INFO, "Headers: ${headers}")
    //.maximumRedeliveries(5).redeliveryDelay(10000)
      .handled(true)
    .end();
  }

  private void deadLetterChannelExample() {
    errorHandler(deadLetterChannel("file:/{{doc.location}}/csv").onPrepareFailure(e->{
      e.getMessage()
      .setHeader(Exchange.FILE_NAME, e);
   })
     
     .useOriginalMessage()
     .logStackTrace(false)
     .maximumRedeliveries(0));
  }

  private void attachExceptionHandlers() {

    // handle network connectivity errors
    onException(ConnectException.class, SocketTimeoutException.class)
      .backOffMultiplier(2)
      .log(LoggingLevel.ERROR,"onException(ConnectException, SocketTimeoutException) called.")
      .setBody(constant("An unexpected network error occurred"))
      .retryAttemptedLogLevel(LoggingLevel.ERROR)
      .log(LoggingLevel.INFO, "Headers: ${headers}")
      .handled(true)
      .end();

    // HttpOperation Failed
    onException(HttpOperationFailedException.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        BaseEvent event = (BaseEvent)exchange.getProperty("kpi_event_object");
        HttpOperationFailedException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);
        log.error("HttpOperationFailedException caught, exception message : " + cause.getMessage());
        log.error("HttpOperationFailedException class and local msg : " + cause.getClass().getName() + " message : " + cause.getLocalizedMessage());
        if(event != null) {
          log.error("HttpOperation Exception event info : " + event.getEvent_source());
        }
        for(StackTraceElement trace : cause.getStackTrace())
        {
         log.error(trace.toString());
        }
      }
    })
    .log(LoggingLevel.INFO, "Headers: ${headers}")
    .maximumRedeliveries(2)
    .redeliveryDelay(1000)
    .backOffMultiplier(2)
    .end();
 
    // Camel Exception
    onException(CamelException.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        BaseEvent event = (BaseEvent)exchange.getProperty("kpi_event_object");
        CamelException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, CamelException.class);
        log.error("CamelException caught, exception message : " + cause.getMessage());
        log.error("CamelException class and local msg : " + cause.getClass().getName() + " message : " + cause.getLocalizedMessage());

        log.error("CamelException Exception body: " + exchange.getMessage().getBody());
        if(event != null) {
          log.error("CamelException Exception event info : " + event.getEvent_source());
        }
      }
    })
    .log(LoggingLevel.INFO, "Headers: ${headers}")
    .handled(true)
    .end();

    // General Exception
     onException(Exception.class)
     .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        BaseEvent event = (BaseEvent)exchange.getProperty("kpi_event_object");
        Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        log.error("General Exception caught, exception message : " + cause.getMessage());
        log.error("General Exception class and local msg : " + cause.getClass().getName() + " message : " + cause.getLocalizedMessage());
        log.error("General Exception body: " + exchange.getMessage().getBody());
        if(event != null) {
          log.error("General Exception event info : " + event.getEvent_source());
        }
        for(StackTraceElement trace : cause.getStackTrace())
        {
         log.error(trace.toString());
        }
      }
    })
    .log(LoggingLevel.INFO, "Headers: ${headers}")
    .maximumRedeliveries(2)
    .redeliveryDelay(3000)
    .end();
  }

  private void processEventKPIs() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("kafka:{{kafka.topic.kpis.name}}?groupId=ccm-splunk-adapter")
    .routeId(routeId)
    .log(LoggingLevel.DEBUG,"Event from Kafka {{kafka.topic.kpis.name}} topic:\n" + 
      "    on the topic ${headers[kafka.TOPIC]}\n" +
      "    on the partition ${headers[kafka.PARTITION]}\n" +
      "    with the offset ${headers[kafka.OFFSET]}\n" + 
      "    and key ${headers[kafka.KEY]}")
    //.log(LoggingLevel.DEBUG,"Event KPI: ${body}")
    .to("direct:publishEventKPIToSplunk")
    ;

  }

  private void publishEventKPIToSplunk() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .unmarshal().json(JsonLibrary.Jackson, EventKPI.class)
    .setProperty("namespace",simple("{{env:NAMESPACE:ccm-configs:NAMESPACE}}"))
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        EventKPI kpiEvent = (EventKPI)exchange.getMessage().getBody();
        SplunkEventLog splunkLog = new SplunkEventLog((String)exchange.getProperty("namespace"),kpiEvent);
        exchange.getMessage().setBody(splunkLog);
      }
    })
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .marshal().json(JsonLibrary.Jackson, SplunkEventLog.class)
    .log(LoggingLevel.INFO,"Publishing event KPI to splunk. Body = ${body} ...")
    .setHeader("Authorization", simple("Splunk {{splunk.token}}"))
    //.to("{{splunk.host}}")
    .to("https://{{splunk.host}}")
    .log(LoggingLevel.INFO,"Event KPI published.")
    ;

  }


  private void publishSplunkEventKPIError() {
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
        error.setError_summary("Unable to process SPLUNK event.");
        error.setError_details(je);

        // KPI
        EventKPI kpi = new EventKPI(EventKPI.STATUS.EVENT_PROCESSING_FAILED);
        kpi.setError(error);
        kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
        kpi.setComponent_route_name((String)exchange.getProperty("kpi_component_route_name"));
        exchange.getMessage().setBody(kpi, EventKPI.class);
      }})

    .setProperty("kpi_object", body())
    .marshal().json(JsonLibrary.Jackson, EventKPI.class)
    .log(LoggingLevel.ERROR,"Generate kpi event: ${body}")
    // send to the general errors topic
    .to("kafka:{{kafka.topic.general-errors.name}}")
    .log(LoggingLevel.INFO,"kpi event added to general errors topic")
    ;
  }

}