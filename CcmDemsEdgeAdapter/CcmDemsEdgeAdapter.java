// To run this integration use:
// kamel run CcmDemsEdgeAdapter.java --property file:application.properties --profile openshift
// 
// recover the service location. If you're running on minikube, minikube service platform-http-server --url=true
// curl -H "name:World" http://<service-location>/hello
//

// camel-k: language=java
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-kafka

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public class CcmDemsEdgeAdapter extends RouteBuilder {
  @Override
  public void configure() throws Exception {
    log.info("About to start DEMS edge adapter route: kafka -> ccm-dems-mock-app");

    // from("kafka:{{kafka.topic.name}}")
    // .routeId("courtCases")
    // .log("Message received from Kafka : ${body}")
    // .log("    on the topic ${headers[kafka.TOPIC]}")
    // .log("    on the partition ${headers[kafka.PARTITION]}")
    // .log("    with the offset ${headers[kafka.OFFSET]}")
    // .log("    with the key ${headers[kafka.KEY]}")
    // .unmarshal().json()
    // .setBody().simple("{\"number\": \"${exchangeProperty.number}\", \"sensitive_content\": \"Shh... this is the secret.\", \"public_content\": \"This is a mock event object.\", \"created_datetime\": \"${date:header.created_datetime:yyyy-MM-dd'T'HH:mm:ssX}\"}")
    // .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    // .to("http://ccm-dems-mock-app/createCourtCase")
    // .setBody().simple("Successfully created new court case in DEMS mock app: number=${exchangeProperty.number}.")
    // .log("${body}");

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
        .setBody().simple("Authentication error.")
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
        .to("https://346n1v5oeh.execute-api.ca-central-1.amazonaws.com/v1/version")
        .log("Response: ${body}")
      .otherwise()
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(401))
        .setBody().simple("Authentication error.")
        .log("Response: ${body}")
      .end();
  }
}