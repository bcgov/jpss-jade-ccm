package ccm;

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
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

import ccm.models.common.event.EventKPI;
import ccm.models.system.splunk.SplunkEventLog;


public class CcmSplunkAdapter extends RouteBuilder {
  @Override
  public void configure() throws Exception {

    processEventKPIs();
    publishEventKPIToSplunk();
  }

  private void processEventKPIs() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("kafka:{{kafka.topic.kpis.name}}?groupId=ccm-splunk-adapter")
    .routeId(routeId)
    .log("Event from Kafka {{kafka.topic.kpis.name}} topic:\n" + 
      "    on the topic ${headers[kafka.TOPIC]}\n" +
      "    on the partition ${headers[kafka.PARTITION]}\n" +
      "    with the offset ${headers[kafka.OFFSET]}\n" + 
      "    and key ${headers[kafka.KEY]}")
    //.log("Event KPI: ${body}")
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
    .log("Publishing event KPI to splunk. Body = ${body} ...")
    .setHeader("Authorization", simple("Splunk {{splunk.token}}"))
    //.to("{{splunk.host}}")
    .to("https://{{splunk.host}}")
    .log("Event KPI published.")
    ;

  }
}