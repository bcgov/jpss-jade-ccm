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
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.model.dataformat.JsonLibrary;
import ccm.models.common.event.BaseEvent;
import ccm.models.common.event.EventKPI;
import ccm.models.common.event.ReportEvent;
import ccm.models.system.justin.JustinDocumentKeyList;



public class CcmReportsProcessor extends RouteBuilder {

    @Override
    public void configure() throws Exception {
       processReportEvents();
       publishEventKPI();
       processUnknownStatus();
    }

    private void processReportEvents() {
        // use method name as route id
        String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    
        from("kafka:{{kafka.topic.reports.name}}?groupId=ccm-dems-adapter&maxPollRecords=30&maxPollIntervalMs=600000")
        .routeId(routeId)
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
            .to("http://ccm-dems-adapter/processDocumentRecordHttp")
           
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
