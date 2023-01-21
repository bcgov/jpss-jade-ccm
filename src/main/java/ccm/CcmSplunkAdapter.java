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

import ccm.models.common.event.BaseEvent;
import ccm.models.common.event.EventKPI;
import ccm.models.system.splunk.SplunkEventLog;


public class CcmSplunkAdapter extends RouteBuilder {
  @Override
  public void configure() throws Exception {

    attachExceptionHandlers();
    processEventKPIs();
    publishEventKPIToSplunk();
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
      .handled(true)
      .end();

    // HttpOperation Failed
    onException(HttpOperationFailedException.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        BaseEvent event = (BaseEvent)exchange.getProperty("kpi_event_object");
        Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        log.error("HttpOperationException caught, exception message : " + cause.getMessage() + " stack trace : " + cause.getStackTrace());
        log.error("HttpOperation Exception event info : " + event.getEvent_source());
      }
    })
    .handled(true)
    .end();
 
    // Camel Exception
    onException(CamelException.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        BaseEvent event = (BaseEvent)exchange.getProperty("kpi_event_object");
        Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        log.error("CamelException caught, exception message : " + cause.getMessage());
        log.error("CamelException Exception event info : " + event.getEvent_source());
      }
    })
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
        log.error("General Exception event info : " + event.getEvent_source());
      }
    })
    .handled(true)
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
    .setProperty("namespace",simple("{{env:NAMESPACE}}"))
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
}