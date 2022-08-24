package ccm;

// To run this integration use:
// kamel run CcmDemsEdgeAdapter.java --property file:application.properties --profile openshift
// 
// recover the service location. If you're running on minikube, minikube service platform-http-server --url=true
// curl -H "name:World" http://<service-location>/hello
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
import org.apache.camel.model.dataformat.JsonLibrary;

import ccm.models.business.BusinessCourtCaseData;
import ccm.models.system.dems.DemsCreateCourtCaseData;

import org.apache.camel.CamelException;
//import org.apache.camel.http.common.HttpOperationFailedException;

public class CcmDemsAdapter extends RouteBuilder {
  @Override
  public void configure() throws Exception {
    /* from("kafka:{{kafka.topic.name}}")
    .routeId("courtCases")
    .log("Message received from Kafka : ${body}")
    .log("    on the topic ${headers[kafka.TOPIC]}")
    .log("    on the partition ${headers[kafka.PARTITION]}")
    .log("    with the offset ${headers[kafka.OFFSET]}")
    .log("    with the key ${headers[kafka.KEY]}")
    .unmarshal().json()
    //.setBody().simple("{\"number\": \"${exchangeProperty.number}\", \"sensitive_content\": \"Shh... this is the secret.\", \"public_content\": \"This is a mock event object.\", \"created_datetime\": \"${date:header.created_datetime:yyyy-MM-dd'T'HH:mm:ssX}\"}")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{token.dems}}")
    //.to("http://ccm-dems-mock-app/createCourtCase")
    .setBody().simple("{\"name\": \"JUSTIN Mock Case ${body[number]}\", \"description\": \"${body[public_content]}\", \"timeZone\": \"Pacific Standard Time\"}")
    .to("{{dems.host}}/cases")
    //.setBody().simple("Successfully created new court case in DEMS mock app: number=${exchangeProperty.number}.")
    .log("Response from DEMS: ${body}"); */

    // from("platform-http:/v1/version?httpMethodRestrict=GET")
    // .routeId("version")
    // .log("version query request received")
    // .setBody().simple("{\n  \"component\": \"CCM DEMS Edge Adapter\",\n  \"version\": \"0.0.1\"\n}")
    // .log("${body}");

    from("platform-http:/v1/version?httpMethodRestrict=GET")
    .routeId("version")
    .log("version query request received")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .choice()
      .when(simple("${header.authorization} == 'Bearer {{token.adapter}}'"))
        .to("http://ccm-justin-mock-app/v1/version")
        .setProperty("version").simple("${body}")
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .setBody().simple("${exchangeProperty.version}${exchangeProperty.version}")
        .log("Response: ${exchangeProperty.version}")
      .otherwise()
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(401))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .setBody().simple("{ \"message\": \"Authentication error.\" }")
        .log("Response: ${body}")
      .end();
    // .to("http://ccm-justin-mock-app/v1/version").setBody().simple("${body}").log("${body}")

    from("platform-http:/dems/v1/version?httpMethodRestrict=GET")
    .routeId("DEMS version")
    .log("DEMS version query request received")
    .choice()
      .when(simple("${header.authorization} == 'Bearer {{token.adapter}}'"))
        .removeHeader("CamelHttpUri")
        .removeHeader("CamelHttpBaseUri")
        .removeHeaders("CamelHttp*")
        .setHeader(Exchange.HTTP_METHOD, simple("GET"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .setHeader("Authorization").simple("Bearer " + "{{token.dems}}")
        .to("{{dems.host}}/version")
        .log("Response: ${body}")
      .otherwise()
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(401))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .setBody().simple("{ \"message\": \"Authentication error.\" }")
        .log("Response: ${body}")
      .end();
      
    from("platform-http:/getCourtCaseExists")
    .routeId("getCourtCaseExists")
    .log("Processing getCourtCaseExists request (event_object_id=${header.event_object_id})...")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{token.dems}}")
    .setHeader("rcc_id").simple("${header.event_object_id}")
    .doTry()
      .toD("{{dems.host}}/org-units/1/cases/12:${header.rcc_id}/id")
    //.doCatch(HttpOperationFailedException.class)
    .doCatch(Exception.class)
    ////.onException(Exception.class)
    ////  .handled(true)
    ////.to("direct:testPathVar")
      .log("Exception: ${exception}")
      .log("Exchange: ${exchange}")
      .choice()
        //.when(header(Exchange.HTTP_RESPONSE_CODE).isEqualTo("404"))
        .when().simple("${exception.statusCode} == 404")
          .log(LoggingLevel.INFO,"Record not found.  HTTP response code = ${exception.statusCode}")
          ////.toD("splunk-hec://hec.monitoring.ag.gov.bc.ca:8088/services/collector")
          .setBody(simple("{\"case_id\": \"123.1\"}"))
          .unmarshal().json()
          .setBody(simple("{\"id\": \"\"}"))
          //.setBody(simple("{\"id\": \"${body[case_id]}\"}"))
          //.log("Response body: '${body}'")
        .endChoice()
        .otherwise()
          .log(LoggingLevel.ERROR,"Unknown error.  HTTP response code = ${exception.statusCode}")
          .setBody(simple("{\"id\": \"123\"}"))
        .endChoice()
      .end()
    .end()
    ;

    from("direct:testPathVar")
    .routeId("testPathVar")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing testPathVar request (rcc_id=${header.rcc_id})...")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{token.dems}}")
    //.toD("{{dems.host}}/cases/rcc_id:${header.rcc_id}/id")
    ;
      
    from("platform-http:/createCourtCase")
    .routeId("createCourtCase")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing createCourtCase request: ${body}")
    .unmarshal().json(JsonLibrary.Jackson, BusinessCourtCaseData.class)
    .process(new Processor() {
      public void process(Exchange exchange) {
        BusinessCourtCaseData b = exchange.getIn().getBody(BusinessCourtCaseData.class);
        DemsCreateCourtCaseData d = new DemsCreateCourtCaseData(b);
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsCreateCourtCaseData.class)
    .log("DEMS-bound request data: '${body}'")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{token.dems}}")
    //.toD("{{dems.host}}/org-units/1/cases")
    ;
      
    from("platform-http:/updateCourtCase")
    .routeId("updateCourtCase")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing updateCourtCase request: ${body}")
    .unmarshal().json(JsonLibrary.Jackson, BusinessCourtCaseData.class)
    .process(new Processor() {
      public void process(Exchange exchange) {
        BusinessCourtCaseData b = exchange.getIn().getBody(BusinessCourtCaseData.class);
        DemsCreateCourtCaseData d = new DemsCreateCourtCaseData(b);
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsCreateCourtCaseData.class)
    .log("DEMS-bound request data: '${body}'")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{token.dems}}")
    //.toD("{{dems.host}}/org-units/1/cases")
    .log("DEBUG: do nothing for now.")
    ;
  }
}