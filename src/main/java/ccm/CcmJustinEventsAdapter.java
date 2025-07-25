package ccm;
// To run this integration use:
// kamel run CcmJustinAdapter.java --property file:application.properties --profile openshift
//
// recover the service location. If you're running on minikube, minikube service platform-http-server --url=true
// curl -d '{}' http://ccm-justin-adapter/courtFileCreated
//

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.List;

import org.apache.camel.CamelException;

// camel-k: language=java
// camel-k: dependency=mvn:org.apache.camel.quarkus
// camel-k: dependency=mvn:org.apache.camel.component.kafka
// camel-k: dependency=mvn:org.apache.camel.camel-quarkus-kafka
// camel-k: dependency=mvn:org.apache.camel.camel-quarkus-jsonpath
// camel-k: dependency=mvn:org.apache.camel.camel-jackson
// camel-k: dependency=mvn:org.apache.camel.camel-splunk-hec
// camel-k: dependency=mvn:org.apache.camel.camel-splunk
// camel-k: dependency=mvn:org.apache.camel.camel-http
// camel-k: dependency=mvn:org.apache.camel.camel-http-common

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.HttpHostConnectException;

import ccm.models.common.event.BaseEvent;
import ccm.models.common.event.CaseUserEvent;
import ccm.models.common.event.ChargeAssessmentEvent;
import ccm.models.common.event.CourtCaseEvent;
import ccm.models.common.event.Error;
import ccm.models.common.event.EventKPI;
import ccm.models.common.event.FileNoteEvent;
import ccm.models.common.event.ReportEvent;
import ccm.models.common.event.ParticipantMergeEvent;
import ccm.models.common.versioning.Version;
import ccm.models.system.justin.JustinEvent;
import ccm.utils.DateTimeUtils;
import ccm.utils.KafkaComponentUtils;

public class CcmJustinEventsAdapter extends RouteBuilder {
  @Override
  public void configure() throws Exception {
    attachExceptionHandlers();

    version();

    courtFileCreated();
    http_stopJustinEvents();
    stopJustinEvents();
    http_startJustinEvents();
    startJustinEvents();
    //cronJustinEventsReconnection();
    requeueJustinEvent();
    requeueJustinEventRange();
    processJustinEventsMainTimer();
    processJustinEventsBulkTimer();
    processJustinMainEvents();
    processJustinBulkEvents();
    processBulkBatchStartedEvent();
    processBulkBatchEndedEvent();

    processAgenFileEvent();
    processAuthListEvent();
    processCourtFileEvent();
    processApprEvent();
    processCrnAssignEvent();
    processUserProvEvent();
    processUserDProvEvent();
    processReportEvents();
    processUnknownEvent();
    processFileClose();
    processFileNote();
    processDeleteFileNote();
    processPartMergeEvents();

    confirmEventProcessed();

    processCaseUserEvents();
    processCaseUserAccountCreated();

    preprocessAndPublishEventCreatedKPI();
    publishEventKPI();
    publishBodyAsEventKPI();
    publishUnknownEventKPIError();
    publishJustinEventKPIError();
  }

  private void attachExceptionHandlers() {

    // handle network connectivity errors
    onException(ConnectException.class, SocketTimeoutException.class, HttpHostConnectException.class)
      .maximumRedeliveries(10).redeliveryDelay(45000)
      .backOffMultiplier(2)
      .log(LoggingLevel.ERROR,"onException(ConnectException, SocketTimeoutException) called.")
      .setBody(constant("An unexpected network error occurred"))
      .retryAttemptedLogLevel(LoggingLevel.ERROR)
      .handled(false)
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
        .log(LoggingLevel.INFO, "Request body: ${body}")
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

    .end();

    onException(CamelException.class)
    .choice()
      .when(simple("${exchangeProperty.kpi_event_object} != null"))
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
          BaseEvent event = (BaseEvent)exchange.getProperty("kpi_event_object");
          Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
          Error error = new Error();
          error.setError_dtm(DateTimeUtils.generateCurrentDtm());

          error.setError_summary("Unable to process event, CamelException raised.");
          error.setError_details(cause.getMessage());
          log.debug("HttpOperationException caught, exception message : " + cause.getMessage() + " stack trace : " + cause.getStackTrace());
          log.error("HttpOperation Exception event info : " + event.getEvent_source());

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
        .log(LoggingLevel.DEBUG,"Derived event KPI published.")
        .log("Caught CamelException exception")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
        .setProperty("error_event_object", body())
        .to("kafka:{{kafka.topic.kpis.name}}")
        .endChoice()
      .otherwise()
        .log(LoggingLevel.ERROR, "Camel Exception thrown: ${exception.message}")
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
          Throwable caused = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);

          Error error = new Error();
          error.setError_dtm(DateTimeUtils.generateCurrentDtm());
          error.setError_summary("Unable to process event, general exception raised.");
          error.setError_code("General Exception");
          error.setError_details(caused.getMessage());

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
        .log(LoggingLevel.DEBUG,"Derived event KPI published.")
        .log("Caught General exception exception")
        .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
        .setProperty("error_event_object", body())
        .to("kafka:{{kafka.topic.kpis.name}}")
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

  private void version() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header = id
    from("platform-http:/" + routeId + "?httpMethodRestrict=GET")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        exchange.getMessage().setBody(Version.V1_0.toString());
        String version = System.getProperty("java.version");
        log.info("Java version:"+version);
      }
    })
    ;
  }

  private void courtFileCreated() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId + "?httpMethodRestrict=POST")
    .routeId(routeId)
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .log(LoggingLevel.DEBUG,"body (before unmarshalling): '${body}'")
    .unmarshal().json()
    .transform(simple("{\"number\": \"${body[number]}\", \"status\": \"created\", \"sensitive_content\": \"${body[sensitive_content]}\", \"public_content\": \"${body[public_content]}\", \"created_datetime\": \"${body[created_datetime]}\"}"))
    .log(LoggingLevel.DEBUG,"body (after unmarshalling): '${body}'")
    .to("kafka:{{kafka.topic.chargeassessments.name}}");


  }

  private void http_stopJustinEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header = id
    from("platform-http:/stopJustinEvents?httpMethodRestrict=PUT")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .to("direct:stopJustinEvents")
    ;
  }

  private void stopJustinEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header = id
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"Pausing Justin pulls from justin queue.")
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {

        List<Route> routeList = exchange.getContext().getRoutes();
        for (Route rte : routeList ) {
          log.info("ROUTES: " + rte.getId());
        }
        exchange.getContext().getRouteController().suspendRoute("processJustinEventsMainTimer");
        exchange.getContext().getRouteController().suspendRoute("processJustinEventsBulkTimer");

        /*exchange.getContext().getRouteController().stopRoute("processJustinEventsMainTimer");
        exchange.getContext().getRouteController().stopRoute("processJustinEventsBulkTimer");*/
      }
    })
    .log(LoggingLevel.INFO,"Justin adapter queue stopped")
    ;
  }

  private void http_startJustinEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header = id
    from("platform-http:/startJustinEvents?httpMethodRestrict=PUT")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .to("direct:startJustinEvents")
    ;
  }

  private void startJustinEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header = id
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"Restarting pulls from justin queue.")
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {

        List<Route> routeList = exchange.getContext().getRoutes();
        for (Route rte : routeList ) {
          log.info("ROUTES: " + rte.getId());
        }
        exchange.getContext().getRouteController().resumeRoute("processJustinEventsMainTimer");
        exchange.getContext().getRouteController().resumeRoute("processJustinEventsBulkTimer");

        /*Route mainTimer = exchange.getContext().getRoute("processJustinEventsMainTimer");
        Route bulkTimer = exchange.getContext().getRoute("processJustinEventsBulkTimer");
        ServiceHelper.startService(mainTimer.getConsumer());
        ServiceHelper.startService(bulkTimer.getConsumer());*/
      }
    })

    .log(LoggingLevel.INFO,"Justin adapter queue started")
    ;
  }

  private void cronJustinEventsReconnection() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //from("cron:tab?schedule=0/1+1+*+*+*+?")
    from("cron:tab?schedule=0 05 4 * * ?") // run 4am every day
    .routeId(routeId)
    .log(LoggingLevel.WARN,"Cron job restart of Justin Events pull started.")
    .to("direct:stopJustinEvents")
    .delay(25000)
    .to("direct:startJustinEvents")

    .log(LoggingLevel.WARN,"Cron job restart of Justin Events pull completed.")
    ;
  }

  private void requeueJustinEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header = id
    from("platform-http:/" + routeId + "?httpMethodRestrict=PUT")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"Re-queueing JUSTIN event: id = ${header.id} ...")
    .setProperty("id", header("id"))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{justin.token}}")
    .toD("https://{{justin.host}}/requeueEventById?id=${exchangeProperty.id}")
    .log(LoggingLevel.INFO,"Event re-queued. Return code: ${header.CamelHttpResponseCode}")
    ;
  }

  private void requeueJustinEventRange() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header = id
    from("platform-http:/" + routeId + "?httpMethodRestrict=PUT")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"Re-queueing JUSTIN events: from id ${header.id} to id ${header.idEnd} ...")
    .setProperty("id", simple("${header.id}"))
    .setProperty("idEnd", simple("${header.idEnd}"))
    .loopDoWhile(simple("${exchangeProperty.id} <= ${exchangeProperty.idEnd}"))
      .log(LoggingLevel.INFO,"Requeuing JUSTIN event ${exchangeProperty.id} ...")
      .removeHeader("CamelHttpUri")
      .removeHeader("CamelHttpBaseUri")
      .removeHeaders("CamelHttp*")
      .setHeader("id", simple("${exchangeProperty.id}"))
      .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .to("http://ccm-justin-adapter/requeueJustinEvent")
      .log(LoggingLevel.INFO,"JUSTIN event ${exchangeProperty.id} requeued.")
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
          String id = (String)exchange.getProperty("id");
          Long nextId = Long.parseLong(id) + 1;
          exchange.setProperty("id", nextId.toString());
        }
      })
    .end()
    .log(LoggingLevel.INFO,"All JUSTIN events requeued.")
    ;
  }

  private void processJustinEventsMainTimer() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("timer://simpleTimer?period={{justin.queue.notification.check.frequency}}&synchronous=true")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{justin.token}}")
    .toD("https://{{justin.host}}/newEventsBatch?system={{justin.queue.main.name}}") // mark all new events as "in progress"
       //.log(LoggingLevel.DEBUG,"Marking all new events in JUSTIN as 'in progress': ${body}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader("Authorization").simple("Bearer " + "{{justin.token}}")
    .toD("https://{{justin.host}}/inProgressEvents?system={{justin.queue.main.name}}") // retrieve all "in progress" events
    .log(LoggingLevel.DEBUG,"Processing in progress events from JUSTIN: ${body}")

    // process events
    .setProperty("numOfEvents")
      .jsonpath("$.events.length()")
    .loopDoWhile(simple("${exchangeProperty.numOfEvents} > 0"))
      .to("direct:processJustinMainEvents")

      // check to see if there are more events to process
      .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .setHeader("Authorization").simple("Bearer " + "{{justin.token}}")
      .toD("https://{{justin.host}}/newEventsBatch?system={{justin.queue.main.name}}") // mark all new events as "in progress"
      .setProperty("numOfEvents")
        .jsonpath("$.events.length()")
    .end()
    ;
  }

  private void processJustinEventsBulkTimer() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("timer://simpleTimer?period={{justin.queue.notification.check.frequency}}&synchronous=true")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{justin.token}}")
    .toD("https://{{justin.host}}/newEventsBatch?system={{justin.queue.bulk.name}}") // mark all new events as "in progress"
       //.log(LoggingLevel.DEBUG,"Marking all new events in JUSTIN as 'in progress': ${body}")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader("Authorization").simple("Bearer " + "{{justin.token}}")
    .toD("https://{{justin.host}}/inProgressEvents?system={{justin.queue.bulk.name}}") // retrieve all "in progress" events
    .log(LoggingLevel.DEBUG,"Processing in progress events from JUSTIN: ${body}")

    // set initial event count
    .setProperty("numOfEvents")
      .jsonpath("$.events.length()")
    .setProperty("totalNumOfEvents", simple("${exchangeProperty.numOfEvents}"))
    .log(LoggingLevel.DEBUG,"Total num events: ${exchangeProperty.numOfEvents}")

    // preserve the pulled events from JUSTIN in the body to a property value
    .setProperty("justin_event_data", simple("${body}"))
    .choice()
      .when(simple("${exchangeProperty.totalNumOfEvents} > 0"))

        // generate batch-started event
        .log(LoggingLevel.INFO,"Start processing ${exchangeProperty.totalNumOfEvents} bulk event(s) from JUSTIN.")
        .to("direct:processBulkBatchStartedEvent")
      .endChoice()
    .end()

    // Restore the pulled justin events
    .setBody(simple("${exchangeProperty.justin_event_data}"))

    // process events
    .loopDoWhile(simple("${exchangeProperty.numOfEvents} > 0"))
      .to("direct:processJustinBulkEvents")

      // check to see if there are more events to process
      .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .setHeader("Authorization").simple("Bearer " + "{{justin.token}}")
      .toD("https://{{justin.host}}/newEventsBatch?system={{justin.queue.bulk.name}}") // mark all new events as "in progress"
      .setProperty("numOfEvents")
        .jsonpath("$.events.length()")
      .process(exchange -> {
        Integer totalNumOfEvents = exchange.getProperty("totalNumOfEvents", Integer.class);
        Integer numOfEvents = exchange.getProperty("numOfEvents", Integer.class);
        exchange.setProperty("totalNumOfEvents", totalNumOfEvents + numOfEvents);
      })
    .end()

    .choice()
      .when(simple("${exchangeProperty.totalNumOfEvents} > 0"))
        // generate batch-ended event
        .log(LoggingLevel.INFO,"Processed ${exchangeProperty.totalNumOfEvents} bulk event(s) from JUSTIN.")
        .wireTap("direct:processBulkBatchEndedEvent")
      .endChoice()
    .end()
    ;
  }

  private void processReportEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("report_event").body()
    .setProperty("kpi_component_route_name", simple(routeId))
    .log(LoggingLevel.INFO,"Processing Report event: ${exchangeProperty.justin_event}")
    .doTry()
      .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
      .log(LoggingLevel.INFO, "attempting to unmarshal Justin Event")
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
          // Insert code that gets executed *before* delegating
          // to the next processor in the chain.
          JustinEvent je = exchange.getIn().getBody(JustinEvent.class);
          ReportEvent be = new ReportEvent(je);

          exchange.getMessage().setBody(be, ReportEvent.class);
          exchange.getMessage().setHeader("kafka.KEY", be.getEvent_key());
        }})
      .log(LoggingLevel.INFO,"Set kpi event object")
      .setProperty("kpi_event_object", body())
      .marshal().json(JsonLibrary.Jackson, ReportEvent.class)
      .log(LoggingLevel.DEBUG,"Generate converted business event: ${body}")
      .removeHeader("CamelHttpUri")
      .removeHeader("CamelHttpBaseUri")
      .removeHeaders("CamelHttp*")

      .to("kafka:{{kafka.topic.reports.name}}")    // ---- > Error produced here -TWuolle
      .setProperty("kpi_event_topic_name", simple("{{kafka.topic.reports.name}}"))
      .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
      .setProperty("kpi_component_route_name", simple(routeId))
      .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
      .to("direct:preprocessAndPublishEventCreatedKPI")
    .doCatch(Exception.class)
      .log(LoggingLevel.DEBUG,"General Exception thrown.")
      .log(LoggingLevel.DEBUG,"${exception}")
      .setProperty("error_event_object", body())
      .setProperty("kpi_event_topic_name",simple("{{kafka.topic.general-errors.name}}"))
      .to("direct:publishJustinEventKPIError")
      .process(new Processor() {
        public void process(Exchange exchange) throws Exception {
          throw exchange.getException();
        }
      })
    .doFinally()
      .log(LoggingLevel.DEBUG,"finally, send confirmation for report event")
      .setBody(simple("${exchangeProperty.report_event}"))
      .setProperty("event_message_id")
        .jsonpath("$.event_message_id")
      .to("direct:confirmEventProcessed")
    .end()
    ;

  }
  private void processJustinMainEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    /*
     * To kick off processing, execute the following on the 'service/ccm-justin-adapter' pod:
     *    cp /etc/camel/resources/eventBatch-oneRCC.json /tmp
     */
    //from("timer://simpleTimer?period={{notification.check.frequency}}")
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    //from("file:/etc/camel/resources/?fileName=eventBatch-oneRCC.json&noop=true&exchangePattern=InOnly&readLock=none")
    //from("file:/etc/camel/resources/?fileName=eventBatch-empty.json&noop=true&exchangePattern=InOnly&readLock=none")
    //from("file:/etc/camel/resources/?fileName=eventBatch.json&noop=true&exchangePattern=InOnly&readLock=none")
    //.to("splunk-hec://hec.monitoring.ag.gov.bc.ca:8088/services/collector/f38b6861-1947-474b-bf6c-a743f2c6a413?")
    // .to("https://{{justin.host}}/inProgressEvents")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    //.to("direct:processNewJUSTINEvents");
    //.log(LoggingLevel.DEBUG,"Processing new JUSTIN events: ${body}")
    //.unmarshal().json(JsonLibrary.Jackson, JustinEventBatch.class)
    .setProperty("numOfEvents")
   
      .jsonpath("$.events.length()")
      .setProperty("disableFileNoteProcess").simple("{{justin-disable-file-notes-processing}}")
    .choice()
      .when(simple("${exchangeProperty.numOfEvents} > 0"))
        .log(LoggingLevel.INFO,"Main event batch count: ${exchangeProperty.numOfEvents}")
        .endChoice()
      .end()
    .setProperty("justin_events")
      .jsonpath("$.events")
    .split()
      .jsonpathWriteAsString("$.events")  // https://stackoverflow.com/questions/51124978/splitting-a-json-array-with-camel
      .setProperty("message_event_type_cd")
        .jsonpath("$.message_event_type_cd")
      .setProperty("event_message_id")
        .jsonpath("$.event_message_id")
      .log(LoggingLevel.INFO,"Main event batch record: (id=${exchangeProperty.event_message_id}, type=${exchangeProperty.message_event_type_cd})")
      
      .choice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.AGEN_FILE))
          .to("direct:processAgenFileEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.MANU_FILE))
          .to("direct:processAgenFileEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.MANU_CFILE))
          .to("direct:processCourtFileEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.AUTH_LIST))
          .to("direct:processAuthListEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.COURT_FILE))
          .to("direct:processCourtFileEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.APPR))
          .to("direct:processApprEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.CRN_ASSIGN))
          .to("direct:processCrnAssignEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.USER_DPROV))
          .to("direct:processUserDProvEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.REPORT))
          .to("direct:processReportEvents")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.DOCM))
          .to("direct:processReportEvents")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.INFO_DOCM))
          .to("direct:processReportEvents")
          .endChoice()
          .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.PART_MERGE))
          .to("direct:processPartMergeEvents")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.FILE_CLOSE))
          .to("direct:processFileClose")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.FILE_NOTE))
          .choice()
            .when(simple("${exchangeProperty.disableFileNoteProcess} == 'false'"))
              .log(LoggingLevel.INFO, "going to call process file note")
              .to("direct:processFileNote")
            .otherwise()
              .log(LoggingLevel.INFO, "file note processing disabled")
              .to("direct:processUnknownEvent")
            .end()
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.DEL_FNOTE)  )
          .choice()
            .when(simple("${exchangeProperty.disableFileNoteProcess} == 'false'"))
              .log(LoggingLevel.INFO, "going to call delete file note")
              .to("direct:processDeleteFileNote")
            .otherwise()
              .log(LoggingLevel.INFO,"file note processing disabled")
              .to("direct:processUnknownEvent")
            .end()
          .endChoice()
        .otherwise()
          .log(LoggingLevel.INFO,"message_event_type_cd = ${exchangeProperty.message_event_type_cd}")
          .to("direct:processUnknownEvent")
          .endChoice()
        .end()
    ;
  }

  private void processJustinBulkEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    /*
     * To kick off processing, execute the following on the 'service/ccm-justin-adapter' pod:
     *    cp /etc/camel/resources/eventBatch-oneRCC.json /tmp
     */
    //from("timer://simpleTimer?period={{notification.check.frequency}}")
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    //from("file:/etc/camel/resources/?fileName=eventBatch-oneRCC.json&noop=true&exchangePattern=InOnly&readLock=none")
    //from("file:/etc/camel/resources/?fileName=eventBatch-empty.json&noop=true&exchangePattern=InOnly&readLock=none")
    //from("file:/etc/camel/resources/?fileName=eventBatch.json&noop=true&exchangePattern=InOnly&readLock=none")
    //.to("splunk-hec://hec.monitoring.ag.gov.bc.ca:8088/services/collector/f38b6861-1947-474b-bf6c-a743f2c6a413?")
    // .to("https://{{justin.host}}/inProgressEvents")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    //.to("direct:processNewJUSTINEvents");
    //.log(LoggingLevel.DEBUG,"Processing new JUSTIN events: ${body}")
    //.unmarshal().json(JsonLibrary.Jackson, JustinEventBatch.class)
    .setProperty("numOfEvents")
      .jsonpath("$.events.length()")
    .choice()
      .when(simple("${exchangeProperty.numOfEvents} > 0"))
        .log(LoggingLevel.INFO,"Bulk event batch count: ${exchangeProperty.numOfEvents}")
        .endChoice()
      .end()
    .setProperty("justin_events")
      .jsonpath("$.events")
    .split()
      .jsonpathWriteAsString("$.events")  // https://stackoverflow.com/questions/51124978/splitting-a-json-array-with-camel
      .setProperty("message_event_type_cd")
        .jsonpath("$.message_event_type_cd")
      .setProperty("event_message_id")
        .jsonpath("$.event_message_id")
      .log(LoggingLevel.INFO,"Bulk event batch record: (id=${exchangeProperty.event_message_id}, type=${exchangeProperty.message_event_type_cd})")
      .choice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.AGEN_FILE))
          .to("direct:processAgenFileEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.MANU_FILE))
          .to("direct:processAgenFileEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.MANU_CFILE))
          .to("direct:processCourtFileEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.AUTH_LIST))
          .to("direct:processAuthListEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.COURT_FILE))
          .to("direct:processCourtFileEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.APPR))
          .to("direct:processApprEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.CRN_ASSIGN))
          .to("direct:processCrnAssignEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.USER_PROV))
          .to("direct:processUserProvEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.USER_DPROV))
          .to("direct:processUserDProvEvent")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.REPORT))
          .to("direct:processReportEvents")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.DOCM))
          .to("direct:processReportEvents")
          .endChoice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.FILE_CLOSE))
          .to("direct:processFileClose")
          .endChoice()
        .otherwise()
          .log(LoggingLevel.INFO,"message_event_type_cd = ${exchangeProperty.message_event_type_cd}")
          .to("direct:processUnknownEvent")
          .endChoice()
        .end()
    ;
  }

  // part of jade 1750
  private void processPartMergeEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("justin_event").body()
    .setProperty("kpi_component_route_name", simple(routeId))
    .log(LoggingLevel.DEBUG,"Processing PART_MERGE event: ${exchangeProperty.justin_event}")
    .doTry()
      .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {

          JustinEvent je = exchange.getIn().getBody(JustinEvent.class);

          ParticipantMergeEvent be = new ParticipantMergeEvent(je);
          exchange.getMessage().setBody(be, ParticipantMergeEvent.class);
          exchange.getMessage().setHeader("kafka.KEY", be.getEvent_key());
        }})
      .setProperty("kpi_event_object", body())
      .marshal().json(JsonLibrary.Jackson, ParticipantMergeEvent.class)
      .setProperty("business_event", body())
      .log(LoggingLevel.DEBUG,"Generate converted business event: ${body}")
      .to("kafka:{{kafka.topic.participant.name}}")
      .setProperty("kpi_event_topic_name", simple("{{kafka.topic.participant.name}}"))
      .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
      .setProperty("kpi_component_route_name", simple(routeId))
      .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
      .to("direct:preprocessAndPublishEventCreatedKPI")
    .doCatch(Exception.class)
      .log(LoggingLevel.DEBUG,"General Exception thrown.")
      .log(LoggingLevel.DEBUG,"${exception}")
      .setProperty("error_event_object", body())
      .to("kafka:{{kafka.topic.justin-event-retry.name}}")
      .setProperty("kpi_event_topic_name",simple("{{kafka.topic.justin-event-retry.name}}"))
      .log(LoggingLevel.DEBUG, "${exchangeProperty.kpi_event_topic_name}")
      .to("direct:publishJustinEventKPIError")
      .process(new Processor() {
        public void process(Exchange exchange) throws Exception {

          throw exchange.getException();
        }
      })
    .doFinally()
      .choice()
        .when(exchangeProperty("kpi_event_topic_name").isNotNull())
        .log(LoggingLevel.DEBUG,"finally, send confirmation for justin event")
        .setBody(simple("${exchangeProperty.justin_event}"))
        .setProperty("event_message_id")
          .jsonpath("$.event_message_id")
        .to("direct:confirmEventProcessed")
      .end()
    .end()
    ;
  }

  private void processAgenFileEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("justin_event").body()
    .setProperty("kpi_component_route_name", simple(routeId))
    .log(LoggingLevel.DEBUG,"Processing AGEN_FILE event: ${exchangeProperty.justin_event}")
    .doTry()
      .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
          // Insert code that gets executed *before* delegating
          // to the next processor in the chain.

          JustinEvent je = exchange.getIn().getBody(JustinEvent.class);

          ChargeAssessmentEvent be = new ChargeAssessmentEvent(je);

          exchange.getMessage().setBody(be, ChargeAssessmentEvent.class);
          exchange.getMessage().setHeader("kafka.KEY", be.getEvent_key());
        }})
      .log(LoggingLevel.DEBUG,"Set kpi event object")
      .setProperty("kpi_event_object", body())
      .marshal().json(JsonLibrary.Jackson, ChargeAssessmentEvent.class)
      .log(LoggingLevel.DEBUG,"Generate converted business event: ${body}")
      .choice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.MANU_FILE))
          .to("kafka:{{kafka.topic.chargeassessments-priority.name}}") // -> JADE-2896-MANU events get a priority topic
          .setProperty("kpi_event_topic_name", simple("{{kafka.topic.chargeassessments-priority.name}}"))
        .endChoice()
        .otherwise()
          .to("kafka:{{kafka.topic.chargeassessments.name}}")    // ---- > Error produced here -TWuolle
          .setProperty("kpi_event_topic_name", simple("{{kafka.topic.chargeassessments.name}}"))
        .endChoice()
      .end()
      .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
      .setProperty("kpi_component_route_name", simple(routeId))
      .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
      .to("direct:preprocessAndPublishEventCreatedKPI")
    .endDoTry()
    .doCatch(Exception.class)
      .log(LoggingLevel.DEBUG,"General Exception thrown.")
      .log(LoggingLevel.DEBUG,"${exception}")
      .setProperty("error_event_object", body())
      .to("kafka:{{kafka.topic.justin-event-retry.name}}")
      .setProperty("kpi_event_topic_name",simple("{{kafka.topic.justin-event-retry.name}}"))
      .log(LoggingLevel.DEBUG, "${exchangeProperty.kpi_event_topic_name}")
    //  .setProperty("kpi_event_topic_name",simple("{{kafka.topic.general-errors.name}}"))
      .to("direct:publishJustinEventKPIError")
      .process(new Processor() {
        public void process(Exchange exchange) throws Exception {

          throw exchange.getException();
        }
      })
    .doFinally()
    .log(LoggingLevel.INFO,"${exchangeProperty.kpi_status} + ${exchangeProperty.event_message_id}")
    .log(LoggingLevel.INFO, "${exchangeProperty.kpi_event_topic_name}")
      .choice()
      .when(exchangeProperty("kpi_event_topic_name").isNotNull())
        .log(LoggingLevel.DEBUG,"finally, send confirmation for justin event")
        .setBody(simple("${exchangeProperty.justin_event}"))
        .setProperty("event_message_id")
          .jsonpath("$.event_message_id")
        .to("direct:confirmEventProcessed")
      .end()
    .end()
    ;
  }

  private void processAuthListEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("justin_event").body()
    .setProperty("kpi_component_route_name", simple(routeId))
    .log(LoggingLevel.DEBUG,"Processing AUTH_LIST event: ${exchangeProperty.justin_event}")
    .doTry()
      .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
          // Insert code that gets executed *before* delegating
          // to the next processor in the chain.

          JustinEvent je = exchange.getIn().getBody(JustinEvent.class);

          ChargeAssessmentEvent be = new ChargeAssessmentEvent(je);

          exchange.getMessage().setBody(be, ChargeAssessmentEvent.class);
          exchange.getMessage().setHeader("kafka.KEY", be.getEvent_key());
        }})
      .setProperty("kpi_event_object", body())
      .marshal().json(JsonLibrary.Jackson, ChargeAssessmentEvent.class)
      .setProperty("business_event", body())
      .log(LoggingLevel.DEBUG,"Generate converted business event: ${body}")
      .to("kafka:{{kafka.topic.chargeassessments.name}}")
      .setProperty("kpi_event_topic_name", simple("{{kafka.topic.chargeassessments.name}}"))
      .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
      .setProperty("kpi_component_route_name", simple(routeId))
      .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
      .to("direct:preprocessAndPublishEventCreatedKPI")
    .doCatch(Exception.class)
      .log(LoggingLevel.DEBUG,"General Exception thrown.")
      .log(LoggingLevel.DEBUG,"${exception}")
      .setProperty("error_event_object", body())
      .to("kafka:{{kafka.topic.justin-event-retry.name}}")
      .setProperty("kpi_event_topic_name",simple("{{kafka.topic.justin-event-retry.name}}"))
      .log(LoggingLevel.DEBUG, "${exchangeProperty.kpi_event_topic_name}")
      //.setProperty("kpi_event_topic_name",simple("{{kafka.topic.general-errors.name}}"))
      .to("direct:publishJustinEventKPIError")
      .process(new Processor() {
        public void process(Exchange exchange) throws Exception {

          throw exchange.getException();
        }
      })
    .doFinally()
      .choice()
        .when(exchangeProperty("kpi_event_topic_name").isNotNull())
        .log(LoggingLevel.DEBUG,"finally, send confirmation for justin event")
        .setBody(simple("${exchangeProperty.justin_event}"))
        .setProperty("event_message_id")
          .jsonpath("$.event_message_id")
        .to("direct:confirmEventProcessed")
      .end()
    .end()
    ;
  }

  private void processUserProvEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("justin_event").body()
    .setProperty("kpi_component_route_name", simple(routeId))
    .log(LoggingLevel.DEBUG,"Processing USER_PROV event: ${exchangeProperty.justin_event}")
    .doTry()
      .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
          // Insert code that gets executed *before* delegating
          // to the next processor in the chain.

          JustinEvent je = exchange.getIn().getBody(JustinEvent.class);

          CaseUserEvent event = new CaseUserEvent(je);

          exchange.getMessage().setBody(event, CaseUserEvent.class);
          exchange.getMessage().setHeader("kafka.KEY", event.getEvent_key());
        }})
      .setProperty("kpi_event_object", body())
      .marshal().json(JsonLibrary.Jackson, CaseUserEvent.class)
      .setProperty("business_event", body())
      .log(LoggingLevel.DEBUG,"Generate converted business event: ${body}")
      .to("kafka:{{kafka.topic.bulk-caseusers.name}}")
      .setProperty("kpi_event_topic_name", simple("{{kafka.topic.bulk-caseusers.name}}"))
      .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
      .setProperty("kpi_component_route_name", simple(routeId))
      .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
      .to("direct:preprocessAndPublishEventCreatedKPI")
    .doCatch(Exception.class)
      .log(LoggingLevel.DEBUG,"General Exception thrown.")
      .log(LoggingLevel.DEBUG,"${exception}")
      .setProperty("error_event_object", body())
      .setProperty("kpi_event_topic_name",simple("{{kafka.topic.general-errors.name}}"))
      .to("direct:publishJustinEventKPIError")
      .process(new Processor() {
        public void process(Exchange exchange) throws Exception {

          throw exchange.getException();
        }
      })
    .doFinally()
      .log(LoggingLevel.DEBUG,"finally, send confirmation for justin event")
      .setBody(simple("${exchangeProperty.justin_event}"))
      .setProperty("event_message_id")
        .jsonpath("$.event_message_id")
      .to("direct:confirmEventProcessed")
    .end()
    ;
  }

  private void processUserDProvEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("justin_event").body()
    .setProperty("kpi_component_route_name", simple(routeId))
    .log(LoggingLevel.DEBUG,"Processing USER_DPROV event: ${exchangeProperty.justin_event}")
    .doTry()
      .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
          // Insert code that gets executed *before* delegating
          // to the next processor in the chain.

          JustinEvent je = exchange.getIn().getBody(JustinEvent.class);

          CaseUserEvent event = new CaseUserEvent(je);

          // JADE-1795 bug: JUSTIN returning null part_id; hard code user key for initial testing
          //be.setEvent_key("122201.0734");

          exchange.getMessage().setBody(event, CaseUserEvent.class);
          exchange.getMessage().setHeader("kafka.KEY", event.getEvent_key());
        }})
      .setProperty("kpi_event_object", body())
      .marshal().json(JsonLibrary.Jackson, CaseUserEvent.class)
      .setProperty("business_event", body())
      .log(LoggingLevel.DEBUG,"Generate converted business event: ${body}")
      .to("kafka:{{kafka.topic.bulk-caseusers.name}}")
      .setProperty("kpi_event_topic_name", simple("{{kafka.topic.bulk-caseusers.name}}"))
      .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
      .setProperty("kpi_component_route_name", simple(routeId))
      .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
      .to("direct:preprocessAndPublishEventCreatedKPI")
    .doCatch(Exception.class)
      .log(LoggingLevel.DEBUG,"General Exception thrown.")
      .log(LoggingLevel.DEBUG,"${exception}")
      .setProperty("error_event_object", body())
      .to("kafka:{{kafka.topic.justin-event-retry.name}}")
      .setProperty("kpi_event_topic_name",simple("{{kafka.topic.justin-event-retry.name}}"))
      .log(LoggingLevel.DEBUG, "${exchangeProperty.kpi_event_topic_name}")
      //.setProperty("kpi_event_topic_name",simple("{{kafka.topic.general-errors.name}}"))
      .to("direct:publishJustinEventKPIError")
      .process(new Processor() {
        public void process(Exchange exchange) throws Exception {

          throw exchange.getException();
        }
      })
    .doFinally()
    .choice()
      .when(exchangeProperty("kpi_event_topic_name").isNotNull())
      .log(LoggingLevel.DEBUG,"finally, send confirmation for justin event")
      .setBody(simple("${exchangeProperty.justin_event}"))
      .setProperty("event_message_id")
        .jsonpath("$.event_message_id")
      .to("direct:confirmEventProcessed")
      .end()
    .end()
    ;
  }

  private void processBulkBatchStartedEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("kpi_component_route_name", simple(routeId))
    .log(LoggingLevel.DEBUG,"Creating case user 'batch-started' event")
    .doTry()
      .process(exchange -> {
          CaseUserEvent event = new CaseUserEvent();
          event.setEvent_status(CaseUserEvent.STATUS.EVENT_BATCH_STARTED.name());
          event.setEvent_source(CaseUserEvent.SOURCE.JADE_CCM.name());

          exchange.getMessage().setBody(event, CaseUserEvent.class);
          exchange.getMessage().setHeader("kafka.KEY", event.getEvent_key());
        })
      .setProperty("kpi_event_object", body())
      .marshal().json(JsonLibrary.Jackson, CaseUserEvent.class)
      .setProperty("business_event", body())
      .log(LoggingLevel.DEBUG,"Generate converted business event: ${body}")
      .to("kafka:{{kafka.topic.bulk-caseusers.name}}")
      .setProperty("kpi_event_topic_name", simple("{{kafka.topic.bulk-caseusers.name}}"))
      .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
      .setProperty("kpi_component_route_name", simple(routeId))
      .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
      .to("direct:preprocessAndPublishEventCreatedKPI")
    .doCatch(Exception.class)
      .log(LoggingLevel.DEBUG,"General Exception thrown.")
      .log(LoggingLevel.DEBUG,"${exception}")
      .setProperty("error_event_object", body())
      .setProperty("kpi_event_topic_name",simple("{{kafka.topic.general-errors.name}}"))
      .to("direct:publishJustinEventKPIError")
      .process(new Processor() {
        public void process(Exchange exchange) throws Exception {

          throw exchange.getException();
        }
      })
    .end()
    ;
  }

  private void processBulkBatchEndedEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("kpi_component_route_name", simple(routeId))
    .delay(65000)
    .log(LoggingLevel.DEBUG,"Creating case user 'batch-ended' event")
    .doTry()
      .process(exchange -> {
          CaseUserEvent event = new CaseUserEvent();
          event.setEvent_status(CaseUserEvent.STATUS.EVENT_BATCH_ENDED.name());
          event.setEvent_source(CaseUserEvent.SOURCE.JADE_CCM.name());

          exchange.getMessage().setBody(event, CaseUserEvent.class);
          exchange.getMessage().setHeader("kafka.KEY", event.getEvent_key());
        })
      .setProperty("kpi_event_object", body())
      .marshal().json(JsonLibrary.Jackson, CaseUserEvent.class)
      .setProperty("business_event", body())
      .log(LoggingLevel.DEBUG,"Generate converted business event: ${body}")
      .to("kafka:{{kafka.topic.bulk-caseusers.name}}")
      .setProperty("kpi_event_topic_name", simple("{{kafka.topic.bulk-caseusers.name}}"))
      .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
      .setProperty("kpi_component_route_name", simple(routeId))
      .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
      .to("direct:preprocessAndPublishEventCreatedKPI")
    .doCatch(Exception.class)
      .log(LoggingLevel.DEBUG,"General Exception thrown.")
      .log(LoggingLevel.DEBUG,"${exception}")
      .setProperty("error_event_object", body())
      .setProperty("kpi_event_topic_name",simple("{{kafka.topic.general-errors.name}}"))
      .to("direct:publishJustinEventKPIError")
      .process(new Processor() {
        public void process(Exchange exchange) throws Exception {

          throw exchange.getException();
        }
      })
    .end()
    ;
  }

  private void processCourtFileEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("justin_event").body()
    .setProperty("kpi_component_route_name", simple(routeId))
    .log(LoggingLevel.DEBUG,"Processing COURT_FILE event: ${exchangeProperty.justin_event}")
    .doTry()
      .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
          // Insert code that gets executed *before* delegating
          // to the next processor in the chain.

          JustinEvent je = exchange.getIn().getBody(JustinEvent.class);

          CourtCaseEvent be = new CourtCaseEvent(je);

          exchange.getMessage().setBody(be, CourtCaseEvent.class);
          exchange.getMessage().setHeader("kafka.KEY", be.getEvent_key());
        }})
      .setProperty("kpi_event_object", body())
      .marshal().json(JsonLibrary.Jackson, CourtCaseEvent.class)
      .log(LoggingLevel.DEBUG,"Generate converted business event: ${body}")
      .choice()
        .when(header("message_event_type_cd").isEqualTo(JustinEvent.STATUS.MANU_CFILE))
          .to("kafka:{{kafka.topic.courtcases-priority.name}}") // -> JADE-2896-MANU events get a priority topic
          .setProperty("kpi_event_topic_name", simple("{{kafka.topic.courtcases-priority.name}}"))
        .endChoice()
        .otherwise()
          .to("kafka:{{kafka.topic.courtcases.name}}")
          .setProperty("kpi_event_topic_name", simple("{{kafka.topic.courtcases.name}}"))
        .endChoice()
      .end()
      .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
      .setProperty("kpi_component_route_name", simple(routeId))
      .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
      .to("direct:preprocessAndPublishEventCreatedKPI")
    .endDoTry()
    .doCatch(Exception.class)
      .log(LoggingLevel.DEBUG,"General Exception thrown.")
      .log(LoggingLevel.DEBUG,"${exception}")
      .setProperty("error_event_object", body())
      .to("kafka:{{kafka.topic.justin-event-retry.name}}")
      .setProperty("kpi_event_topic_name",simple("{{kafka.topic.justin-event-retry.name}}"))
      .log(LoggingLevel.DEBUG, "${exchangeProperty.kpi_event_topic_name}")
      //.setProperty("kpi_event_topic_name",simple("{{kafka.topic.general-errors.name}}"))
      .to("direct:publishJustinEventKPIError")
      .process(new Processor() {
        public void process(Exchange exchange) throws Exception {

          throw exchange.getException();
        }
      })
    .doFinally()
    .choice()
      .when(exchangeProperty("kpi_event_topic_name").isNotNull())
      .log(LoggingLevel.DEBUG,"finally, send confirmation for justin event")
      .setBody(simple("${exchangeProperty.justin_event}"))
      .setProperty("event_message_id")
        .jsonpath("$.event_message_id")
      .to("direct:confirmEventProcessed")
      .end()
    .end()
    //.setProperty("kpi_component_route_name", simple(routeId))
    //.setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
    //.to("direct:preprocessAndPublishEventCreatedKPI")
    ;
  }

  private void processApprEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("justin_event").body()
    .setProperty("kpi_component_route_name", simple(routeId))
    .log(LoggingLevel.DEBUG,"Processing APPR event: ${body}")
    .doTry()
      .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
          // Insert code that gets executed *before* delegating
          // to the next processor in the chain.

          JustinEvent je = exchange.getIn().getBody(JustinEvent.class);

          CourtCaseEvent be = new CourtCaseEvent(je);

          exchange.getMessage().setBody(be, CourtCaseEvent.class);
          exchange.getMessage().setHeader("kafka.KEY", be.getEvent_key());
        }})
      .setProperty("kpi_event_object", body())
      .marshal().json(JsonLibrary.Jackson, CourtCaseEvent.class)
      .log(LoggingLevel.DEBUG,"Generate converted business event: ${body}")
      .to("kafka:{{kafka.topic.courtcases.name}}")
      .setProperty("kpi_event_topic_name", simple("{{kafka.topic.courtcases.name}}"))
      .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
      .setProperty("kpi_component_route_name", simple(routeId))
      .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
      .to("direct:preprocessAndPublishEventCreatedKPI")
    .doCatch(Exception.class)
      .log(LoggingLevel.DEBUG,"General Exception thrown.")
      .log(LoggingLevel.DEBUG,"${exception}")
      .setProperty("error_event_object", body())
      .to("kafka:{{kafka.topic.justin-event-retry.name}}")
      .setProperty("kpi_event_topic_name",simple("{{kafka.topic.justin-event-retry.name}}"))
      .log(LoggingLevel.DEBUG, "${exchangeProperty.kpi_event_topic_name}")
      //.setProperty("kpi_event_topic_name",simple("{{kafka.topic.general-errors.name}}"))
      .to("direct:publishJustinEventKPIError")
      .process(new Processor() {
        public void process(Exchange exchange) throws Exception {

          throw exchange.getException();
        }
      })
    .doFinally()
    .choice()
    .when(exchangeProperty("kpi_event_topic_name").isNotNull())
    .log(LoggingLevel.DEBUG,"finally, send confirmation for justin event")
      .setBody(simple("${exchangeProperty.justin_event}"))
      .setProperty("event_message_id")
        .jsonpath("$.event_message_id")
      .to("direct:confirmEventProcessed")
      .end()
    .end()
    ;
  }

  private void processCrnAssignEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("justin_event").body()
    .setProperty("kpi_component_route_name", simple(routeId))
    .log(LoggingLevel.DEBUG,"Processing CRN_ASSIGN event: ${body}")
    .doTry()
      .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
          // Insert code that gets executed *before* delegating
          // to the next processor in the chain.

          JustinEvent je = exchange.getIn().getBody(JustinEvent.class);

          CourtCaseEvent be = new CourtCaseEvent(je);

          exchange.getMessage().setBody(be, CourtCaseEvent.class);
          exchange.getMessage().setHeader("kafka.KEY", be.getEvent_key());
        }})
      .setProperty("kpi_event_object", body())
      .marshal().json(JsonLibrary.Jackson, CourtCaseEvent.class)
      .log(LoggingLevel.DEBUG,"Generate converted business event: ${body}")
      .to("kafka:{{kafka.topic.courtcases.name}}")
      .setProperty("kpi_event_topic_name", simple("{{kafka.topic.courtcases.name}}"))
      .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
      .setProperty("kpi_component_route_name", simple(routeId))
      .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
      .to("direct:preprocessAndPublishEventCreatedKPI")
    .doCatch(Exception.class)
      .log(LoggingLevel.DEBUG,"General Exception thrown.")
      .log(LoggingLevel.DEBUG,"${exception}")
      .setProperty("error_event_object", body())
      .to("kafka:{{kafka.topic.justin-event-retry.name}}")
      .setProperty("kpi_event_topic_name",simple("{{kafka.topic.justin-event-retry.name}}"))
      .log(LoggingLevel.DEBUG, "${exchangeProperty.kpi_event_topic_name}")
      //.setProperty("kpi_event_topic_name",simple("{{kafka.topic.general-errors.name}}"))
      .to("direct:publishJustinEventKPIError")
      .process(new Processor() {
        public void process(Exchange exchange) throws Exception {

          throw exchange.getException();
        }
      })
    .doFinally()
    .choice()
      .when(exchangeProperty("kpi_event_topic_name").isNotNull())
      .log(LoggingLevel.DEBUG,"finally, send confirmation for justin event")
      .setBody(simple("${exchangeProperty.justin_event}"))
      .setProperty("event_message_id")
        .jsonpath("$.event_message_id")
      .to("direct:confirmEventProcessed")
      .end()
    .end()
    ;
  }

  private void processUnknownEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG,"Ignoring unknown event: ${body}")
    .setProperty("justin_event", body())
    .setProperty("kpi_component_route_name", simple(routeId))
    .setProperty("kpi_event_topic_name",simple("{{kafka.topic.general-errors.name}}"))
    .doTry()
      .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) {
          JustinEvent je = exchange.getIn().getBody(JustinEvent.class);

          Error error = new Error();
          error.setError_dtm(DateTimeUtils.generateCurrentDtm());
          error.setError_summary("Unable to process unknown JUSTIN event.");
          error.setError_details(je);

          // KPI
          EventKPI kpi = new EventKPI(EventKPI.STATUS.EVENT_UNKNOWN);
          kpi.setError(error);
          kpi.setEvent_topic_name((String)exchange.getProperty("kpi_event_topic_name"));
          kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
          kpi.setComponent_route_name(routeId);
          kpi.setEvent_details(je);
          exchange.getMessage().setBody(kpi, EventKPI.class);
        }
      })
      .setProperty("kpi_object", body())
      .marshal().json(JsonLibrary.Jackson, EventKPI.class)
      // send to the general errors topic
      .to("kafka:{{kafka.topic.general-errors.name}}")
      // mark JUSTIN event as processed
    .doCatch(Exception.class)
      .log(LoggingLevel.DEBUG,"General Exception thrown.")
      .log(LoggingLevel.DEBUG,"${exception}")
      .setProperty("error_event_object", body())
      .setProperty("kpi_event_topic_name",simple("{{kafka.topic.general-errors.name}}"))
      .to("direct:publishJustinEventKPIError")
      .process(new Processor() {
        public void process(Exchange exchange) throws Exception {

          throw exchange.getException();
        }
      })
    .doFinally()
      .log(LoggingLevel.DEBUG,"finally, send confirmation for justin event")
      .setBody(simple("${exchangeProperty.justin_event}"))
      .setProperty("event_message_id")
        .jsonpath("$.event_message_id")
      .to("direct:confirmEventProcessed")
    .end()
    // send KPI to kpis topic
    .to("direct:publishUnknownEventKPIError")
    ;
  }

  private void confirmEventProcessed() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeaders("*")
    .setProperty("event_message_id")
      .jsonpath("$.event_message_id")
    .setProperty("message_event_type_cd")
      .jsonpath("$.message_event_type_cd")
    .log(LoggingLevel.DEBUG,"Marking event ${exchangeProperty.event_message_id} (${exchangeProperty.message_event_type_cd}) as processed.")
    //.removeHeader("message_event_type_cd")
    //.removeHeader("event_message_id")
    //.removeHeader("is_success")
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{justin.token}}")
    //.toD("https://{{justin.host}}/eventStatus?event_message_id=${header.custom_event_message_id}&is_success=T")
    //.to("https://{{justin.host}}/eventStatus")
    .doTry()
      //.to("https://{{justin.host}}/eventStatus")
      .toD("https://{{justin.host}}/eventStatus?event_message_id=${exchangeProperty.event_message_id}&is_success=T")
    .doCatch(Exception.class)
      .log(LoggingLevel.ERROR,"Message Id: ${exchangeProperty.event_message_id} Exception: ${exception}")
      .log(LoggingLevel.DEBUG,"Exchange Context: ${exchange.context}")
      .choice()
        //.when(header(Exchange.HTTP_RESPONSE_CODE).isEqualTo("404"))
        .when().simple("${exception.statusCode} == 400")
          .log(LoggingLevel.INFO,"Bad request.  HTTP response code = ${exception.statusCode}")
          .log(LoggingLevel.DEBUG,"Exception: '${exception}'")
          .log(LoggingLevel.DEBUG,"Headers: '${headers}'")
        .endChoice()
        .otherwise()
          .log(LoggingLevel.ERROR,"Unknown error.  HTTP response code = ${exception.statusCode}")
          .log(LoggingLevel.DEBUG,"Headers: '${headers}'")
        .endChoice()
      .end()
    .end()
    ;
  }


  private void processCaseUserEvents() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("kafka:{{kafka.topic.caseusers.name}}?groupId=ccm-justin-adapter&maxPollRecords=1&maxPollIntervalMs=2400000")
    .routeId(routeId)
    .log(LoggingLevel.INFO,"Event from Kafka {{kafka.topic.caseusers.name}} topic (offset=${headers[kafka.OFFSET]}): ${body}\n" +
    "    on the topic ${headers[kafka.TOPIC]}\n" +
    "    on the partition ${headers[kafka.PARTITION]}\n" +
    "    with the offset ${headers[kafka.OFFSET]}\n" +
    "    with the key ${headers[kafka.KEY]}")
    .log(LoggingLevel.DEBUG,"body = ${body}")
    .setProperty("event_status", jsonpath("$.event_status"))
    .unmarshal().json(JsonLibrary.Jackson, CaseUserEvent.class)
    .setProperty("event_object", body())
    .setProperty("event_topic_name", simple("${headers[kafka.TOPIC]}"))
    .setProperty("event_topic_offset", simple("${headers[kafka.OFFSET]}"))
    .setProperty("event_topic_partition", simple("${headers[kafka.PARTITION]}"))
    .setProperty("event_key", simple("${headers[kafka.KEY]}"))
    .log(LoggingLevel.DEBUG,"event_key=${exchangeProperty.event_key}.")
    .marshal().json(JsonLibrary.Jackson, CaseUserEvent.class)
    .choice()
      .when(exchangeProperty("event_status").isEqualTo(CaseUserEvent.STATUS.ACCOUNT_CREATED.name()))
        .log(LoggingLevel.INFO,"Processing case user event: offset=${exchangeProperty.event_topic_offset}, event_status = ${exchangeProperty.event_status} ...")
        .to("direct:processCaseUserAccountCreated")
        .log(LoggingLevel.INFO,"Case user event processed.")
        .endChoice()
      .end()
    ;
  }

  private void processCaseUserAccountCreated() {
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN:
    //   exchange properties:
    //     event_object: case user event object
    //     event_key: case user event key (aka PART_ID)
    //     event_topic_name: event topic name
    //     event_topic_offset: event topic offset
    //     event_topic_partition: event topic partition

    from("direct:" + routeId)
    .routeId(routeId)
    // publish event KPI - processing started
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        CaseUserEvent event = exchange.getProperty("event_object", CaseUserEvent.class);
        String event_topic_name = exchange.getProperty("event_topic_name", String.class);
        String event_topic_offset = exchange.getProperty("event_topic_offset", String.class);
        String event_topic_partition = exchange.getProperty("event_topic_partition", String.class);

        EventKPI event_kpi = new EventKPI(
          event,
          EventKPI.STATUS.EVENT_PROCESSING_STARTED
        );

        event_kpi.setComponent_route_name(routeId);
        event_kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
        event_kpi.setEvent_topic_name(event_topic_name);
        event_kpi.setEvent_topic_offset(event_topic_offset);
        event_kpi.setEvent_topic_partition(event_topic_partition);

        exchange.setProperty("event_kpi_object", event_kpi);
        exchange.getMessage().setBody(event_kpi);
      }
    })
    .marshal().json(JsonLibrary.Jackson, EventKPI.class)
    .to("direct:publishBodyAsEventKPI")
    .delay(60000)

    // update JUSTIN user status
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setBody(simple("{\"part_id\": \"${exchangeProperty.event_key}\"}"))
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{justin.token}}")
    .log("Setting user DEMS flag (${exchangeProperty.event_key}) in JUSTIN ...")
    .toD("https://{{justin.host}}/demsUserSet?part_id=${exchangeProperty.event_key}")
    .log("User DEMS flag updated in JUSTIN. Return code: ${header.CamelHttpResponseCode}")

    // publish event KPI - processing completed
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        EventKPI event_kpi = exchange.getProperty("event_kpi_object", EventKPI.class);

        event_kpi.setKpi_dtm(DateTimeUtils.generateCurrentDtm());
        event_kpi.setKpi_status(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name());

        exchange.getMessage().setBody(event_kpi);
      }
    })
    .marshal().json(JsonLibrary.Jackson, EventKPI.class)
    .to("direct:publishBodyAsEventKPI")
    ;
  }

  private void preprocessAndPublishEventCreatedKPI() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: property = kpi_event_topic_recordmetadata
    //---------
    //IN: property = kpi_event_object
    //IN: property = kpi_event_topic_name
    //IN: property = kpi_status
    //IN: property = kpi_component_route_name
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    // extract kpi_event_topic_offset
    // extract kpi_event_topic_partition
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        // extract the offset from response header.  Example format: "[some-topic-0@301]"

        try {
          // https://kafka.apache.org/30/javadoc/org/apache/kafka/clients/producer/RecordMetadata.html
          Object o = (Object)exchange.getProperty("kpi_event_topic_recordmetadata");
          String recordMetadata = o.toString();

          String event_offset = KafkaComponentUtils.extractOffsetFromRecordMetadata(recordMetadata);
          String event_partition = KafkaComponentUtils.extractPartitionFromRecordMetadata(recordMetadata);
          exchange.setProperty("kpi_event_topic_offset", event_offset);
          exchange.setProperty("kpi_event_topic_partition", event_partition);
        } catch (Exception e) {
          // failed to retrieve offset. Do nothing.
        }
      }})
    .to("direct:publishEventKPI")
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

  private void publishBodyAsEventKPI() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: body = EventKPI json
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG,"Publishing Event KPI to Kafka ...")
    .log(LoggingLevel.DEBUG,"body: ${body}")
    .to("kafka:{{kafka.topic.kpis.name}}")
    .log(LoggingLevel.DEBUG,"Event KPI published.")
    ;
  }

  private void publishUnknownEventKPIError() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: property = kpi_object
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setBody(simple("${exchangeProperty.kpi_object}"))
    .marshal().json(JsonLibrary.Jackson, EventKPI.class)
    .log(LoggingLevel.DEBUG,"Event kpi: ${body}")
    .to("kafka:{{kafka.topic.kpis.name}}")
    ;
  }

  private void publishJustinEventKPIError() {
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
        error.setError_summary("Unable to process JUSTIN event.");
        error.setError_details(je);

        // KPI
        EventKPI kpi = new EventKPI(EventKPI.STATUS.EVENT_UNKNOWN);
        kpi.setError(error);
        kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
        kpi.setComponent_route_name((String)exchange.getProperty("kpi_component_route_name"));
        exchange.getMessage().setBody(kpi, EventKPI.class);
      }})

    .setProperty("kpi_object", body())
    .marshal().json(JsonLibrary.Jackson, EventKPI.class)
    .log(LoggingLevel.DEBUG,"Generate kpi event: ${body}")
    // send to the general errors topic
    .to("kafka:{{kafka.topic.general-errors.name}}")
    .log(LoggingLevel.DEBUG,"kpi event added to general errors topic")
    .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
    .setBody(simple("${exchangeProperty.kpi_object}"))
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        EventKPI kpi = exchange.getIn().getBody(EventKPI.class);
        // extract the offset from response header.  Example format: "[some-topic-0@301]"
        String expectedTopicName = (String)exchange.getProperty("kpi_event_topic_name");
        System.out.println(expectedTopicName);

        try {
          // https://kafka.apache.org/30/javadoc/org/apache/kafka/clients/producer/RecordMetadata.html
          Object o = (Object)exchange.getProperty("kpi_event_topic_recordmetadata");
          String recordMetadata = o.toString();
          System.out.println("recordMetadata:"+recordMetadata);
          String event_offset = KafkaComponentUtils.extractOffsetFromRecordMetadata(recordMetadata);
          String event_partition = KafkaComponentUtils.extractPartitionFromRecordMetadata(recordMetadata);
          exchange.setProperty("kpi_event_topic_offset", event_offset);
          exchange.setProperty("kpi_event_topic_partition", event_partition);
          kpi.setEvent_topic_offset(event_offset);
          kpi.setEvent_topic_partition(event_partition);
          kpi.setEvent_topic_name(expectedTopicName);
        } catch (Exception e) {
          // failed to retrieve offset. Do nothing.
        }
        exchange.getMessage().setBody(kpi, EventKPI.class);
      }})
    .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
    .marshal().json(JsonLibrary.Jackson, EventKPI.class)
    .log(LoggingLevel.DEBUG,"Event kpi: ${body}")
    .to("kafka:{{kafka.topic.kpis.name}}")
    ;
  }
  private void processFileClose() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("justin_event").body()
    .setProperty("kpi_component_route_name", simple(routeId))
    .log(LoggingLevel.DEBUG,"Processing File Close event: ${body}")
    .doTry()
      .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
          // Insert code that gets executed *before* delegating
          // to the next processor in the chain.

          JustinEvent je = exchange.getIn().getBody(JustinEvent.class);

          CourtCaseEvent be = new CourtCaseEvent(je);
          
          exchange.getMessage().setBody(be, CourtCaseEvent.class);
          exchange.getMessage().setHeader("kafka.KEY", be.getEvent_key());

        }})
      .setProperty("kpi_event_object", body())
      .marshal().json(JsonLibrary.Jackson, CourtCaseEvent.class)
      .log(LoggingLevel.DEBUG,"Generate converted business event: ${body}")
      .to("kafka:{{kafka.topic.courtcases.name}}")
      .setProperty("kpi_event_topic_name", simple("{{kafka.topic.courtcases.name}}"))
      .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
      .setProperty("kpi_component_route_name", simple(routeId))
      .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
      .to("direct:preprocessAndPublishEventCreatedKPI")
    .doCatch(Exception.class)
      .log(LoggingLevel.DEBUG,"General Exception thrown.")
      .log(LoggingLevel.DEBUG,"${exception}")
      .setProperty("error_event_object", body())
      .to("kafka:{{kafka.topic.justin-event-retry.name}}")
      .setProperty("kpi_event_topic_name",simple("{{kafka.topic.justin-event-retry.name}}"))
      .log(LoggingLevel.DEBUG, "${exchangeProperty.kpi_event_topic_name}")
      //.setProperty("kpi_event_topic_name",simple("{{kafka.topic.general-errors.name}}"))
      .to("direct:publishJustinEventKPIError")
      .process(new Processor() {
        public void process(Exchange exchange) throws Exception {

          throw exchange.getException();
        }
      })
    .doFinally()
    .choice()
      .when(exchangeProperty("kpi_event_topic_name").isNotNull())
        .log(LoggingLevel.DEBUG,"finally, send confirmation for justin event")
        .setBody(simple("${exchangeProperty.justin_event}"))
        .setProperty("event_message_id")
          .jsonpath("$.event_message_id")
        .to("direct:confirmEventProcessed")
      .end()
    .end()
    ;
  }

  private void processFileNote() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("justin_event").body()
    .setProperty("kpi_component_route_name", simple(routeId))
    .log(LoggingLevel.DEBUG,"Processing File Note event: ${body}")

    .doTry()
      .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
          // Insert code that gets executed *before* delegating
          // to the next processor in the chain.

          JustinEvent je = exchange.getIn().getBody(JustinEvent.class);

          FileNoteEvent be = new FileNoteEvent(je);
          
          exchange.getMessage().setBody(be, FileNoteEvent.class);
          exchange.getMessage().setHeader("kafka.KEY", be.getEvent_key());

        }})
      .setProperty("kpi_event_object", body())
      .marshal().json(JsonLibrary.Jackson, FileNoteEvent.class)
      .log(LoggingLevel.DEBUG,"Generate converted business event: ${body}")
      .to("kafka:{{kafka.topic.file.notes}}")
      .setProperty("kpi_event_topic_name", simple("{{kafka.topic.file.notes}}"))
      .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
      .setProperty("kpi_component_route_name", simple(routeId))
      .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
      .to("direct:preprocessAndPublishEventCreatedKPI")
    .doCatch(Exception.class)
      .log(LoggingLevel.DEBUG,"General Exception thrown.")
      .log(LoggingLevel.DEBUG,"${exception}")
      .setProperty("error_event_object", body())
      .to("kafka:{{kafka.topic.justin-event-retry.name}}")
      .setProperty("kpi_event_topic_name",simple("{{kafka.topic.justin-event-retry.name}}"))
      .log(LoggingLevel.DEBUG, "${exchangeProperty.kpi_event_topic_name}")
      //.setProperty("kpi_event_topic_name",simple("{{kafka.topic.general-errors.name}}"))
      .to("direct:publishJustinEventKPIError")
      .process(new Processor() {
        public void process(Exchange exchange) throws Exception {

          throw exchange.getException();
        }
      })
    .doFinally()
    .choice()
      .when(exchangeProperty("kpi_event_topic_name").isNotNull())
        .log(LoggingLevel.DEBUG,"finally, send confirmation for justin event")
        .setBody(simple("${exchangeProperty.justin_event}"))
        .setProperty("event_message_id")
          .jsonpath("$.event_message_id")
        .to("direct:confirmEventProcessed")
      .end()
    .end()
    ;
  }

  private void processDeleteFileNote() {
     // use method name as route id
     String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

     from("direct:" + routeId)
     .routeId(routeId)
     .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
     .setProperty("justin_event").body()
     .setProperty("kpi_component_route_name", simple(routeId))
     .log(LoggingLevel.DEBUG,"Processing Delete File Note event: ${body}")
     .doTry()
       .unmarshal().json(JsonLibrary.Jackson, JustinEvent.class)
       .process(new Processor() {
         @Override
         public void process(Exchange exchange) throws Exception {
           // Insert code that gets executed *before* delegating
           // to the next processor in the chain.
 
           JustinEvent je = exchange.getIn().getBody(JustinEvent.class);
 
           FileNoteEvent be = new FileNoteEvent(je);
           
           exchange.getMessage().setBody(be, FileNoteEvent.class);
           exchange.getMessage().setHeader("kafka.KEY", be.getEvent_key());
 
         }})
       .setProperty("kpi_event_object", body())
       .marshal().json(JsonLibrary.Jackson, FileNoteEvent.class)
       .log(LoggingLevel.INFO,"Generate converted business event: ${body}")
       .to("kafka:{{kafka.topic.file.notes}}")
       .setProperty("kpi_event_topic_name", simple("{{kafka.topic.file.notes}}"))
       .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
       .setProperty("kpi_component_route_name", simple(routeId))
       .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_CREATED.name()))
       .to("direct:preprocessAndPublishEventCreatedKPI")
     .doCatch(Exception.class)
       .log(LoggingLevel.DEBUG,"General Exception thrown.")
       .log(LoggingLevel.DEBUG,"${exception}")
       .setProperty("error_event_object", body())
       .to("kafka:{{kafka.topic.justin-event-retry.name}}")
       .setProperty("kpi_event_topic_name",simple("{{kafka.topic.justin-event-retry.name}}"))
       .log(LoggingLevel.DEBUG, "${exchangeProperty.kpi_event_topic_name}")
       //.setProperty("kpi_event_topic_name",simple("{{kafka.topic.general-errors.name}}"))
       .to("direct:publishJustinEventKPIError")
       .process(new Processor() {
         public void process(Exchange exchange) throws Exception {
 
           throw exchange.getException();
         }
       })
     .doFinally()
     .choice()
      .when(exchangeProperty("kpi_event_topic_name").isNotNull())
        .log(LoggingLevel.INFO,"finally, send confirmation for justin event")
        .setBody(simple("${exchangeProperty.justin_event}"))
        .setProperty("event_message_id")
          .jsonpath("$.event_message_id")
        .to("direct:confirmEventProcessed")
       .end()
     .end()
     ;
  }
}