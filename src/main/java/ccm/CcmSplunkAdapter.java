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

import ccm.models.common.*;
import ccm.models.system.splunk.SplunkEventLog;


public class CcmSplunkAdapter extends RouteBuilder {
  @Override
  public void configure() throws Exception {

    processKpiEvents();
    postLogToSplunk();
  }

  private void processKpiEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("kafka:{{kafka.topic.kpis.name}}?groupId=ccm-splunk-adapter")
    .routeId(routeId)
    .log("Event from Kafka {{kafka.topic.kpis.name}} topic (offset=${headers[kafka.OFFSET]}): ${body}\n" + 
      "    on the topic ${headers[kafka.TOPIC]}\n" +
      "    on the partition ${headers[kafka.PARTITION]}\n" +
      "    with the offset ${headers[kafka.OFFSET]}")
    .to("direct:postLogToSplunk")
    ;

  }

  private void postLogToSplunk() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .unmarshal().json(JsonLibrary.Jackson, CommonEventKPI.class)
    .log("Processing kpi event data: ${body}")
    .setProperty("namespace",simple("{{env:NAMESPACE}}"))
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        CommonEventKPI kpiEvent = (CommonEventKPI)exchange.getMessage().getBody();

        SplunkEventLog splunkLog = new SplunkEventLog((String)exchange.getProperty("namespace"),kpiEvent);

        System.out.println(splunkLog);

        exchange.getMessage().setBody(splunkLog);
      }
    })
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .marshal().json(JsonLibrary.Jackson, SplunkEventLog.class)
    .log("Logging event to splunk body: ${body}")
    .setHeader("Authorization", simple("Splunk {{splunk.token}}"))
    //.to("{{splunk.host}}")
    .to("https://{{splunk.host}}")
    .log("Event logged.")
    ;

  }
}