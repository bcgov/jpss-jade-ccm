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
import ccm.utils.DateTimeUtils;
import ccm.utils.KafkaComponentUtils;


public class CcmSplunkAdapter extends RouteBuilder {
  @Override
  public void configure() throws Exception {

    attachExceptionHandlers();
    processEventKPIs();
    publishEventKPIToSplunk();
  }

  private void attachExceptionHandlers() {

    errorHandler(deadLetterChannel("file:/{{doc.location}}/csv").onPrepareFailure(e->{
      e.getMessage()
      .setHeader(Exchange.FILE_NAME, e);
   })
     
     .useOriginalMessage()
     .logStackTrace(false)
     .maximumRedeliveries(0));

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
       
        ccm.models.common.event.Error error = new  ccm.models.common.event.Error();
        error.setError_dtm(DateTimeUtils.generateCurrentDtm());
        error.setError_code("HttpOperationFailed");
        error.setError_summary("Unable to process event.HttpOperationFailed exception raised");
        // KPI
        EventKPI kpi = new EventKPI(event, EventKPI.STATUS.EVENT_PROCESSING_FAILED);
        kpi.setEvent_topic_name((String)exchange.getProperty("kpi_event_topic_name"));
        kpi.setEvent_topic_offset(exchange.getProperty("kpi_event_topic_offset"));
        kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
        kpi.setComponent_route_name((String)exchange.getProperty("kpi_component_route_name"));
        kpi.setError(error);
        exchange.getMessage().setBody(kpi);

        // https://kafka.apache.org/30/javadoc/org/apache/kafka/clients/producer/RecordMetadata.html
        // extract the offset from response header.  Example format: "[some-topic-0@301]"
        String derived_event_offset = KafkaComponentUtils.extractOffsetFromRecordMetadata(
          exchange.getProperty("derived_event_recordmetadata"));
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
 
    onException(CamelException.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        BaseEvent event = (BaseEvent)exchange.getProperty("kpi_event_object");
       
        ccm.models.common.event.Error error = new  ccm.models.common.event.Error();
        error.setError_dtm(DateTimeUtils.generateCurrentDtm());
        error.setError_code("CamelException");
        error.setError_summary("Unable to process event, CamelException raised.");
       
       
        // KPI
        EventKPI kpi = new EventKPI(event, EventKPI.STATUS.EVENT_PROCESSING_FAILED);
        kpi.setEvent_topic_name((String)exchange.getProperty("kpi_event_topic_name"));
        kpi.setEvent_topic_offset(exchange.getProperty("kpi_event_topic_offset"));
        kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
        kpi.setComponent_route_name((String)exchange.getProperty("kpi_component_route_name"));
        kpi.setError(error);
        exchange.getMessage().setBody(kpi);

        // https://kafka.apache.org/30/javadoc/org/apache/kafka/clients/producer/RecordMetadata.html
        // extract the offset from response header.  Example format: "[some-topic-0@301]"
        String derived_event_offset = KafkaComponentUtils.extractOffsetFromRecordMetadata(
          exchange.getProperty("derived_event_recordmetadata"));
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
    .handled(true)
    .end();

    // General Exception
     onException(Exception.class)
     .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        BaseEvent event = (BaseEvent)exchange.getProperty("kpi_event_object");
       
        ccm.models.common.event.Error error = new  ccm.models.common.event.Error();
        error.setError_dtm(DateTimeUtils.generateCurrentDtm());
        error.setError_summary("Unable to process event., general Exception raised.");
        error.setError_code("General Exception");
        error.setError_details(event);
       
        // KPI
        EventKPI kpi = new EventKPI(event, EventKPI.STATUS.EVENT_PROCESSING_FAILED);
        kpi.setEvent_topic_name((String)exchange.getProperty("kpi_event_topic_name"));
        kpi.setEvent_topic_offset(exchange.getProperty("kpi_event_topic_offset"));
        kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
        kpi.setComponent_route_name((String)exchange.getProperty("kpi_component_route_name"));
        kpi.setError(error);
        exchange.getMessage().setBody(kpi);

        // https://kafka.apache.org/30/javadoc/org/apache/kafka/clients/producer/RecordMetadata.html
        // extract the offset from response header.  Example format: "[some-topic-0@301]"
        String derived_event_offset = KafkaComponentUtils.extractOffsetFromRecordMetadata(
          exchange.getProperty("derived_event_recordmetadata"));
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