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
// camel-k:dependency=mvn:org.apache.camel:camel-jaxb

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.StringTokenizer;

import org.apache.camel.CamelException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.http.NoHttpResponseException;

import ccm.models.common.event.BaseEvent;
import ccm.models.common.event.EventKPI;
import ccm.models.common.event.ReportEvent;
import ccm.models.system.justin.JustinDocumentKeyList;
import ccm.utils.DateTimeUtils;
import ccm.models.common.event.Error;

public class CcmReportsProcessor extends RouteBuilder {

  @Override
  public void configure() throws Exception {
    attachExceptionHandlers(); 
    processReportEvents();
    createRccStaticReportsEvent();
    publishEventKPI();
    processUnknownStatus();
      
  }

  private void attachExceptionHandlers() {

    // handle network connectivity errors
    onException(ConnectException.class, SocketTimeoutException.class)
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

            if(cause != null && !cause.getResponseBody().isEmpty()) {
              error.setError_details(cause.getResponseBody());
            } else if(cause != null && cause.getResponseHeaders().get("CCMExceptionEncoded") != null) {
              byte[] decodedException = Base64.getDecoder().decode(cause.getResponseHeaders().get("CCMExceptionEncoded"));
              String decodedString = new String(decodedException);
              log.error(decodedString);
              error.setError_details(decodedString);
            } else if(cause != null && cause.getResponseHeaders().get("CCMException") != null) {
              log.error(cause.getResponseHeaders().get("CCMException"));
              error.setError_details(cause.getResponseHeaders().get("CCMException"));
            }

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

    //re-queue based on the event type
    /*.choice()
      .when(simple("${exchangeProperty.error_status_code} == '503'"))
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            BaseEvent event = (BaseEvent)exchange.getProperty("kpi_event_object");
            log.error("toString event: " + event.toString());

            String kafkaTopic = getKafkaTopicByEventType(event.getEvent_type());
            exchange.setProperty("kafka_topic_name", kafkaTopic);
          }
        })
        .log(LoggingLevel.INFO, "retry the message")
        .log(LoggingLevel.INFO, "${exchangeProperty.kafka_topic_name}")
        .setBody(header("event"))
            .to("kafka:{{kafka.topic.caseusers.name}}")

      .end()*/

    .end();

    // Handle Camel Exception
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
            if(cause != null) {
              cause.printStackTrace();
            }

            log.debug("CamelException caught, exception message : " + cause.getMessage() + " stack trace : " + cause.getStackTrace());
            log.error("CamelException Exception event info : " + event.getEvent_source());
            // KPI
            EventKPI kpi = new EventKPI(event, EventKPI.STATUS.EVENT_PROCESSING_FAILED);
            // KPI

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
        .log(LoggingLevel.INFO,"Derived event KPI published.")
        .log("Caught CamelException exception")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
        .setProperty("error_event_object", body())
        .to("kafka:{{kafka.topic.kpis.name}}")
        .endChoice()
      .otherwise()
        .log(LoggingLevel.ERROR, "CamelException thrown: ${exception.message}")
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
            error.setError_summary(cause.getMessage());
            error.setError_code("General Exception");
            error.setError_details(cause.toString());
            if(cause != null) {
              cause.printStackTrace();
            }

            log.error("General Exception caught, exception message : " + cause.getMessage());
            log.error("General Exception event info : " + event.getEvent_source());
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
        .log(LoggingLevel.INFO,"Derived event KPI published.")
        .log("Caught General exception exception")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
        .setProperty("error_event_object", body())
        .to("kafka:{{kafka.topic.kpis.name}}")
        .endChoice()
      .otherwise()
        .log(LoggingLevel.ERROR, "General Exception thrown: ${exception.message}")
        .log("Body: ${body}")
        .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("500"))
        .setBody(simple("{\"error\": \"${exception.message}\"}"))
        .transform().simple("Error reported: ${exception.message} - cannot process this message.")
        .setHeader(Exchange.HTTP_RESPONSE_TEXT, simple("{\"error\": \"${exception.message}\"}"))
        .setHeader("CCMException", simple("{\"error\": \"${exception.message}\"}"))
      .end()
   .end()
   ;
  }

  private void processReportEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("kafka:{{kafka.topic.reports.name}}?groupId=ccm-dems-adapter&maxPollRecords=1&maxPollIntervalMs=4800000")
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
    .setHeader("force_update")
      .jsonpath("$.force_update") // force_update to force static rcc reports to update.
    .setHeader("event_message_id")
      .jsonpath("$.justin_event_message_id")
    .setProperty("rcc_ids", simple("${headers[rcc_ids]}"))
    .setHeader("event").simple("${body}")
    .unmarshal().json(JsonLibrary.Jackson, ReportEvent.class)
    .setProperty("kpi_event_object", body())
    .setProperty("kpi_event_topic_name", simple("${headers[kafka.TOPIC]}"))
    .setProperty("kpi_event_topic_offset", simple("${headers[kafka.OFFSET]}"))
    .setProperty("kpi_event_topic_partition", simple("${headers[kafka.PARTITION]}"))
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
        .log(LoggingLevel.DEBUG, "Pre-headers: ${headers}")
        //.removeHeaders("CamelHttp*")
        .removeHeader("kafka.HEADERS")
        .removeHeader("Accept-Encoding")
        .removeHeader("Content-Encoding")
        .setHeader(Exchange.HTTP_METHOD, simple("POST"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .log(LoggingLevel.DEBUG, "headers: ${headers}")
        .log(LoggingLevel.INFO,"Lookup message: '${body}'")
        .to("http://ccm-dems-adapter/processDocumentRecord")
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

  private void createRccStaticReportsEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header = id
    from("platform-http:/" + routeId + "?httpMethodRestrict=PUT")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"Re-queueing static reports event: rcc_id = ${header.rcc_id} ...")
    .setProperty("rcc_id", header("rcc_id"))
    .choice()
      .when(simple("${exchangeProperty.rcc_id} != null"))
        .log(LoggingLevel.INFO, "Create ReportEvent for Static reports")
        // create Report Event for static type reports.
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) {
            String rcc_id = exchange.getMessage().getHeader("rcc_id", String.class);
            StringBuilder reportTypesSb = new StringBuilder("");
            reportTypesSb.append(ReportEvent.REPORT_TYPES.NARRATIVE.name() + ",");
            reportTypesSb.append(ReportEvent.REPORT_TYPES.SYNOPSIS.name() + ",");
            reportTypesSb.append(ReportEvent.REPORT_TYPES.CPIC.name() + ",");
            reportTypesSb.append(ReportEvent.REPORT_TYPES.WITNESS_STATEMENT.name() + ",");
            reportTypesSb.append(ReportEvent.REPORT_TYPES.DV_IPV_RISK.name() + ",");
            reportTypesSb.append(ReportEvent.REPORT_TYPES.DM_ATTACHMENT.name() + ",");
            reportTypesSb.append(ReportEvent.REPORT_TYPES.SUPPLEMENTAL.name() + ",");
            reportTypesSb.append(ReportEvent.REPORT_TYPES.ACCUSED_INFO.name() + ",");
            reportTypesSb.append(ReportEvent.REPORT_TYPES.VEHICLE.name());

            ReportEvent re = new ReportEvent();
            re.setJustin_rcc_id(rcc_id);
            re.setEvent_status(ReportEvent.STATUS.REPORT.name());
            re.setEvent_source(ReportEvent.SOURCE.JADE_CCM.name());
            re.setJustin_message_event_type_cd(ReportEvent.STATUS.REPORT.name());
            re.setReport_type(reportTypesSb.toString());
            exchange.getMessage().setBody(re, ReportEvent.class);
          }
        })
        .setProperty("kpi_event_object", body())
        .marshal().json(JsonLibrary.Jackson, ReportEvent.class)
        .to("kafka:{{kafka.topic.reports.name}}")
        .setProperty("kpi_event_topic_name", simple("{{kafka.topic.reports.name}}"))
        .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))

        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            // extract the offset from response header.  Example format: "[some-topic-0@301]"
            String expectedTopicName = (String)exchange.getProperty("kpi_event_topic_name");

            try {
              // https://kafka.apache.org/30/javadoc/org/apache/kafka/clients/producer/RecordMetadata.html
              Object o = (Object)exchange.getProperty("kpi_event_topic_recordmetadata");
              String recordMetadata = o.toString();

              StringTokenizer tokenizer = new StringTokenizer(recordMetadata, "[@]");

              if (tokenizer.countTokens() == 2) {
                // get first token
                String topicAndPartition = tokenizer.nextToken();

                if (topicAndPartition.startsWith(expectedTopicName)) {
                  // this is the metadata we are looking for
                  Long offset = Long.parseLong(tokenizer.nextToken());
                  exchange.setProperty("kpi_event_topic_offset", offset);
                }
              }
            } catch (Exception e) {
              // failed to retrieve offset. Do nothing.
            }
          }
        })
        .setProperty("kpi_component_route_name", simple(routeId))
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
        .to("direct:publishEventKPI")
      .endChoice()
      .otherwise()
        .log(LoggingLevel.ERROR, "No rcc_id provided.")
      .endChoice()
    .end()
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
    //IN: property = kpi_event_topic_partition
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
        kpi.setEvent_topic_partition(exchange.getProperty("kpi_event_topic_partition"));
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
