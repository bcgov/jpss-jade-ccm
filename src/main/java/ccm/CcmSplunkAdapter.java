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


public class CcmSplunkAdapter extends RouteBuilder {
  @Override
  public void configure() throws Exception {

    from("kafka:{{kafka.topic.kpis.name}}?groupId=ccm-splunk-adapter")
    .routeId("processSplunkEvents")
    .log("Event from Kafka {{kafka.topic.kpis.name}} topic (offset=${headers[kafka.OFFSET]}): ${body}\n" + 
      "    on the topic ${headers[kafka.TOPIC]}\n" +
      "    on the partition ${headers[kafka.PARTITION]}\n" +
      "    with the offset ${headers[kafka.OFFSET]}")
    .to("direct:processSplunkEvent")
    ;


    from("direct:processSplunkEvent")
    .routeId("processSplunkEvent")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing splunk data: ${body}")
    .unmarshal().json(JsonLibrary.Jackson, CommonSplunkEvent.class)
    .process(new Processor() {
      @Override
      public void process(Exchange ex) {
        CommonSplunkEvent se = ex.getIn().getBody(CommonSplunkEvent.class);
        CommonSplunkData bd = new CommonSplunkData(se);
        ex.getMessage().setBody(bd);
      }
    })
    .marshal().json(JsonLibrary.Jackson, CommonSplunkData.class)
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader("Authorization", simple("Splunk {{splunk.token}}"))
    .log("Generating derived data: ${body}")
    .toD("{{splunk.host}}")
    ;

/*

    from("direct:logSplunkEvent")
    .routeId("logSplunkEvent")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setBody().simple("{\n  \"event\": \"${body}\",\n  \"sourcetype\": \"manual\"\n}")
    .log("Logging event to splunk body: ${body}")
    .setHeader("Authorization", simple("Splunk {{splunk.token}}"))
    .to("{{splunk.host}}")
    ;

 */




  }
}