package ccm;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
// camel-k: dependency=mvn:io.strimzi:kafka-oauth-client:0.10.0
// camel-k: dependency=mvn:io.strimzi:kafka-oauth-common:0.10.0
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-kafka
// camel-k: dependency=mvn:io.quarkus:quarkus-apicurio-registry-avro
// camel-k: dependency=mvn:io.apicurio:apicurio-registry-serdes-avro-serde

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;

import ccm.models.common.data.AuthUser;
import ccm.models.common.data.AuthUserList;

/*
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConfiguration;
//import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
*/

import ccm.models.common.event.BaseEvent;
import ccm.models.common.event.CaseUserEvent;
import ccm.models.common.event.Error;
import ccm.models.common.event.EventKPI;
import ccm.models.system.pidp.PIDPAuthUserList;
import ccm.models.system.pidp.PidpUserModificationEvent;
import ccm.utils.DateTimeUtils;
import ccm.utils.KafkaComponentUtils;
//import io.confluent.kafka.serializers.KafkaAvroDeserializer;


public class CcmPidpAdapter extends RouteBuilder {
  @Override
  public void configure() throws Exception {
    
    //attachExceptionHandlers();
    processCaseUserAccountCreated();
    publishBodyAsEventKPI();
    getCourtCaseAuthList();
    getKafkaToken();
  }

  private void attachExceptionHandlers() {
/*
    // HttpOperation Failed
    onException(HttpOperationFailedException.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        BaseEvent event = (BaseEvent)exchange.getProperty("kpi_event_object");
        Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        Error error = new Error();
        error.setError_dtm(DateTimeUtils.generateCurrentDtm());
        error.setError_code("HttpOperationFailed");
        error.setError_summary("Unable to process event.HttpOperationFailed exception raised");

        log.debug("HttpOperationFailed caught, exception message : " + cause.getMessage() + " stack trace : " + cause.getStackTrace());
        log.error("HttpOperationFailed Exception event info : " + event.getEvent_source());
        // KPI
        EventKPI kpi = new EventKPI(event, EventKPI.STATUS.EVENT_PROCESSING_FAILED);
        kpi.setEvent_topic_name((String)exchange.getProperty("kpi_event_topic_name"));
        kpi.setEvent_topic_offset(exchange.getProperty("kpi_event_topic_offset"));
        kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
        kpi.setComponent_route_name((String)exchange.getProperty("kpi_component_route_name"));
        kpi.setError(error);
        exchange.getMessage().setBody(kpi);
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
 */
    // Camel Exception
    onException(CamelException.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        BaseEvent event = (BaseEvent)exchange.getProperty("kpi_event_object");
        Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        Error error = new Error();
        error.setError_dtm(DateTimeUtils.generateCurrentDtm());
        error.setError_code("CamelException");
        error.setError_summary("Unable to process event, CamelException raised.");
       
        log.debug("CamelException caught, exception message : " + cause.getMessage() + " stack trace : " + cause.getStackTrace());
        log.error("CamelException Exception event info : " + event.getEvent_source());
       
        // KPI
        EventKPI kpi = new EventKPI(event, EventKPI.STATUS.EVENT_PROCESSING_FAILED);
        kpi.setEvent_topic_name((String)exchange.getProperty("kpi_event_topic_name"));
        kpi.setEvent_topic_offset(exchange.getProperty("kpi_event_topic_offset"));
        kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
        kpi.setComponent_route_name((String)exchange.getProperty("kpi_component_route_name"));
        kpi.setError(error);
        exchange.getMessage().setBody(kpi);
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
        Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        Error error = new Error();
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

  private void processCaseUserAccountCreated_avro_serdes() throws Exception {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    log.info("Defining '" + routeId + "' ...");

    // KafkaAvroDeserializer kafkaAvroDeserializer = new KafkaAvroDeserializer();
    // kafkaAvroDeserializer.configure(Collections.singletonMap("specific.avro.reader", "true"), false);

    from("kafka:{{pidp.kafka.topic.usercreation.name}}" + 
      "?groupId={{pidp.kafka.consumergroup.name}}" +
      "&autoOffsetReset=earliest"
    )
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG, "Received user creation event from PIDP ...  Headers = '${headers}'")
    .log("Received user creation event from PIDP 1...")
    //.log("Received user creation event from PIDP 2...")
    //.log("Received user creation event from PIDP 3... body = '${body}'.")
    //.log("(DEBUG) PIDP payload: ${body}")
    //.log(LoggingLevel.DEBUG,"PIDP payload: ${body}")
    .setProperty("event_topic", simple("{{kafka.topic.caseusers.name}}"))
    .unmarshal().json(JsonLibrary.Jackson, PidpUserModificationEvent.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        PidpUserModificationEvent pidpUserEvent = exchange.getIn().getBody(PidpUserModificationEvent.class);
        CaseUserEvent event = new CaseUserEvent(pidpUserEvent);

        exchange.getMessage().setHeader("kafka.KEY", event.getEvent_key());
        exchange.setProperty("event_object", event);
        exchange.getMessage().setBody(event);
      }
    })
    .marshal().json(JsonLibrary.Jackson, CaseUserEvent.class)
    .log(LoggingLevel.DEBUG,"Converted to CaseUserEvent: ${body}")
    .log("Publishing user creation event (key = ${header[kafka.KEY]}) ...")
    .to("kafka:{{kafka.topic.caseusers.name}}?brokers=events-kafka-bootstrap:9092&securityProtocol=PLAINTEXT")
    // + "&keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer"
    // + "&valueDeserializer=org.apache.kafka.common.serialization.StringDeserializer"
    .log("User creation event published.")

    // generate event KPI
    .setProperty("event_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        CaseUserEvent event = exchange.getProperty("event_object", CaseUserEvent.class);
        EventKPI kpi = new EventKPI(event, EventKPI.STATUS.EVENT_CREATED);
        String event_topic = exchange.getProperty("event_topic", String.class);
        String event_offset = KafkaComponentUtils.extractOffsetFromRecordMetadata(
          exchange.getProperty("event_recordmetadata")
        );

        kpi.setComponent_route_name(routeId);
        kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
        kpi.setEvent_topic_name(event_topic);
        kpi.setEvent_topic_offset(event_offset);
        exchange.getMessage().setBody(kpi);
      }
    })
    .marshal().json(JsonLibrary.Jackson, EventKPI.class)
    .log("Publishing event KPI ...")
    .to("direct:publishBodyAsEventKPI")
    .log("Event KPI published.")
    ;
  }

  private void processCaseUserAccountCreated() throws Exception {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    log.info("Defining '" + routeId + "' ...");

    // KafkaAvroDeserializer kafkaAvroDeserializer = new KafkaAvroDeserializer();
    // kafkaAvroDeserializer.configure(Collections.singletonMap("specific.avro.reader", "true"), false);

    from("kafka:{{pidp.kafka.topic.usercreation.name}}" + 
      "?groupId={{pidp.kafka.consumergroup.name}}" // +
      //"&autoOffsetReset=earliest"
    )
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG, "Received user creation event from PIDP ...  Headers = '${headers}'")
    .log("Received user creation event from PIDP 1...")
    //.log("Received user creation event from PIDP 2...")
    //.log("Received user creation event from PIDP 3... body = '${body}'.")
    //.log("(DEBUG) PIDP payload: ${body}")
    //.log(LoggingLevel.DEBUG,"PIDP payload: ${body}")
    .setProperty("event_topic", simple("{{kafka.topic.caseusers.name}}"))
    .doTry()
    .unmarshal().json(JsonLibrary.Jackson, PidpUserModificationEvent.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        PidpUserModificationEvent pidpUserEvent = exchange.getIn().getBody(PidpUserModificationEvent.class);
        CaseUserEvent event = new CaseUserEvent(pidpUserEvent);

        exchange.getMessage().setHeader("kafka.KEY", event.getEvent_key());
        exchange.setProperty("event_object", event);
        exchange.getMessage().setBody(event);
      }
    })
    .marshal().json(JsonLibrary.Jackson, CaseUserEvent.class)
    .log(LoggingLevel.DEBUG,"Converted to CaseUserEvent: ${body}")
    .log("Publishing user creation event (key = ${header[kafka.KEY]}) ...")
    .to("kafka:{{kafka.topic.caseusers.name}}?brokers={{ccm.kafka.brokers}}&securityProtocol=PLAINTEXT")
    // + "&keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer"
    // + "&valueDeserializer=org.apache.kafka.common.serialization.StringDeserializer"
    .log("User creation event published.")

    // generate event KPI
    .setProperty("event_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        CaseUserEvent event = exchange.getProperty("event_object", CaseUserEvent.class);
        EventKPI kpi = new EventKPI(event, EventKPI.STATUS.EVENT_CREATED);
        String event_topic = exchange.getProperty("event_topic", String.class);
        String event_offset = KafkaComponentUtils.extractOffsetFromRecordMetadata(
          exchange.getProperty("event_recordmetadata")
        );

        kpi.setComponent_route_name(routeId);
        kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
        kpi.setEvent_topic_name(event_topic);
        kpi.setEvent_topic_offset(event_offset);
        exchange.getMessage().setBody(kpi);
      }
    })
    .marshal().json(JsonLibrary.Jackson, EventKPI.class)
    .log("Publishing event KPI ...")
    .to("direct:publishBodyAsEventKPI")
    .log("Event KPI published.")
    .doCatch(Exception.class)
      .log(LoggingLevel.DEBUG,"Ignoring unknown event: ${body}")
      .setProperty("justin_event", body())
      .setProperty("kpi_component_route_name", simple(routeId))
      .setProperty("kpi_event_topic_name",simple("{{kafka.topic.general-errors.name}}"))
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) {
          BaseEvent event = (BaseEvent)exchange.getProperty("kpi_event_object");

          Error error = new Error();
          error.setError_dtm(DateTimeUtils.generateCurrentDtm());
          error.setError_summary("Unable to process unknown PIDP event.");
          error.setError_details(event);
          // KPI
          EventKPI kpi = new EventKPI(EventKPI.STATUS.EVENT_UNKNOWN);
          kpi.setError(error);
          kpi.setEvent_topic_name((String)exchange.getProperty("kpi_event_topic_name"));
          kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
          kpi.setComponent_route_name(routeId);
          exchange.getMessage().setBody(kpi, EventKPI.class);
        }
      })
      .setProperty("kpi_object", body())
      .marshal().json(JsonLibrary.Jackson, EventKPI.class)
      .to("direct:publishBodyAsEventKPI")
    .end()
    ;
  }

  private void getCourtCaseAuthList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId + "?httpMethodRestrict=GET")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"getCaseAuthList request received. key = ${header.number}")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .to("direct:getKafkaToken")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .log(LoggingLevel.DEBUG, "bearer set : ${header.pidp_access.token}")
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "${header.pidp_access.token}") //https://dev.jpidp.justice.gov.bc.ca/api/v1/evidence-case-management/getCaseUserKeys?RCCNumber=
    //.log(LoggingLevel.INFO,"trying to call evidence url : {{pidp-api-host}}evidence-case-management/getCaseUserKeys?RCCNumber=${header.number}")
    .toD("{{pidp-api-host}}evidence-case-management/getCaseUserKeys?RCCNumber=${header.number}")

    .log(LoggingLevel.DEBUG,"Received response from Case Mgt API: '${body}'")
    .choice()
      .when().simple("${header.CamelHttpResponseCode} == 200")
      //.log(LoggingLevel.INFO, "Success! processing results")
      .unmarshal().json(JsonLibrary.Jackson, ArrayList.class)
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) {
          ArrayList<String> j = exchange.getIn().getBody(ArrayList.class);
        
          AuthUserList authList = new AuthUserList();
          authList.setRcc_id((String)exchange.getIn().getHeader("number"));
          for(String userName : j) {
            authList.getAuth_user_list().add(new AuthUser(userName, AuthUser.RoleTypes.PIDP_SUBMITTING_AGENCY.toString()));
          }
          log.info("Returned PIDP Auth List size of:"+authList.getAuth_user_list().size());
          exchange.getMessage().setBody(authList, AuthUserList.class);
        }
      })
      .marshal().json(JsonLibrary.Jackson, AuthUserList.class)
      .log(LoggingLevel.DEBUG,"Converted response (from PIDP to Business model): '${body}'")
      .endChoice()
    .otherwise()
      .log(LoggingLevel.INFO, "Failed to retrieve PIDP auth users")
    .endChoice()
  .end()
  ;
  }
  
  private void getKafkaToken() {
    // use method name as route id
    // use method name as route id
    String routeId = new Object() {
    }.getClass().getEnclosingMethod().getName();
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching()
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader("CamelHttpMethod").simple("POST") 
    .setHeader("Content-Type").simple("application/x-www-form-urlencoded")
    .setHeader("Accept").simple("application/json")
    .setBody(simple("grant_type=client_credentials&client_id={{pidp-api-oauth-client-id}}&client_secret={{pidp-api-oauth-client-secret}}"))
     //.to("{{pidp-api-oauth-token-endpoint-url}}")
    .to("https://{{pidp-api-oauth-token-endpoint-url}}")
    .convertBodyTo(String.class)
    .log(LoggingLevel.DEBUG,"response from API: " + body())
    .choice()
      .when().simple("${header.CamelHttpResponseCode} == 200")
        //.unmarshal().json(JsonLibrary.Jackson, OAuthBearerToken.class)
        .log(LoggingLevel.DEBUG,"token : $.access_token")
        .setHeader("pidp_access.token").jsonpath("$.access_token")
        .endChoice()
        //.to("direct:<some direct route>")
      .otherwise()
        .log("Not Authenticated!!!")
        .endChoice()
    .end()
    ;
  }
  
/*   
private void processEvent() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // https://access.redhat.com/documentation/en-us/red_hat_integration/2021.q3/html/developing_and_managing_integrations_using_camel_k/authenticate-camel-k-against-kafka#creating-secret-oauthbearer-camel-k-kafka
  
    from("kafka:{{consumer.topic}}" + routeId)
    .routeId(routeId)
    .log(LoggingLevel.DEBUG,"body = ${body}")
    .process(exchange -> {
      exchange.getIn().setBody("Hello World");
    });
  } 
*/

/* 
  private void processCaseUserAccountCreated_old() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // Configure SSL context parameters for the P12 client certificate
    KeyStoreParameters keystore = new KeyStoreParameters();
    keystore.setResource("file:path/to/client.p12");
    keystore.setPassword("clientpassword");
    keystore.setType("PKCS12");

    KeyManagersParameters keyManagers = new KeyManagersParameters();
    keyManagers.setKeyStore(keystore);

    SSLContextParameters sslContext = new SSLContextParameters();
    sslContext.setKeyManagers(keyManagers);

    // Configure Kafka component and set the SSL context parameters
    KafkaComponent kafka = new KafkaComponent();
    //// kafka.setSslContextParameters(sslContext);
    getContext().addComponent("kafka", kafka);

    // Configure OIDC authentication parameters
    KafkaConfiguration kafkaConfig = new KafkaConfiguration();
    kafkaConfig.setSaslMechanism("OAUTHBEARER");
    kafkaConfig.setSecurityProtocol("SASL_SSL");
    kafkaConfig.setSaslJaasConfig("org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required "
        + "oidc.provider.url='https://oidc-provider.com' "
        + "oidc.client.id='client-id' "
        + "oidc.client.secret='client-secret' "
        + "oidc.token.endpoint='https://token-endpoint.com' "
        + "oidc.username.claim='sub' "
        + "oidc.groups.claim='groups';");

    //from("kafka:{{pidp.kafka.topic.usercreation.name}}?brokers={{pidp.kafka.bootstrapservers.url}}&groupId=jade-ccm&configuration=#kafkaConfig")
    from("direct:hey")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG,"body = ${body}")
    .unmarshal().json(JsonLibrary.Jackson, PidpUserModificationEvent.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        PidpUserModificationEvent pidpEvent = (PidpUserModificationEvent)exchange.getIn().getBody();
        CaseUserEvent event = new CaseUserEvent(pidpEvent);
        
        exchange.getMessage().setBody(event);
        exchange.getMessage().setHeader("kafka.KEY", event.getEvent_key());
      }
    })
    .setProperty("event_object", simple("${body}"))
    .marshal().json(JsonLibrary.Jackson, CaseUserEvent.class)
    .to("kafka:{{kafka.topic.caseusers.name}}")
    .setProperty("kpi_event_topic_recordmetadata", simple("${headers[org.apache.kafka.clients.producer.RecordMetadata]}"))
    .setProperty("event_topic_name",simple("{{kafka.topic.caseusers.name}}"))
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        CaseUserEvent event = (CaseUserEvent)exchange.getProperty("event_object");
        String event_offset = KafkaComponentUtils.extractOffsetFromRecordMetadata(exchange.getProperty("kpi_event_topic_recordmetadata"));

        EventKPI kpi = new EventKPI(event);
        kpi.setEvent_topic_offset(event_offset);
        kpi.setComponent_route_name(routeId);
        kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
        kpi.setEvent_topic_name((String)exchange.getProperty("event_topic_name"));

        exchange.getMessage().setBody(kpi);
      }
    })
    .to("direct:publishBodyAsEventKPI")
    ;
  } */
  
  private void publishBodyAsEventKPI() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    //IN: body = EventKPI json
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.DEBUG,"Publishing Event KPI to Kafka ...")
    .log(LoggingLevel.DEBUG,"body: ${body}")
    .to("kafka:{{kafka.topic.kpis.name}}?brokers={{ccm.kafka.brokers}}&securityProtocol=PLAINTEXT")
    // + "&keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer"
    // + "&valueDeserializer=org.apache.kafka.common.serialization.StringDeserializer"
    .log(LoggingLevel.DEBUG,"Event KPI published.")
    ;
  }
}