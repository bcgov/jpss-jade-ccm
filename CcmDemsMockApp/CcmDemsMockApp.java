// To run this integrations use:
// kamel run CcmDemsMockApp.java --dev -t service.enabled=true
// 
// recover the service location. If you're running on minikube, minikube service platform-http-server --url=true
// curl -H "name:World" http://<service-location>/hello
//

// camel-k: language=java
import org.apache.camel.builder.RouteBuilder;

public class CcmDemsMockApp extends RouteBuilder {
  @Override
  public void configure() throws Exception {

    // // https://camel.apache.org/components/next/eips/delay-eip.html
    // from("platform-http:/createCourtCase?httpMethodRestrict=GET")
    // .routeId("createCourtFile2")
    // .delay(3000)
    // .setBody(simple("Court case '${header.number}' processed successfully after 3 seconds."))
    // .to("log:info");

    // https://camel.apache.org/components/next/eips/delay-eip.html
    from("platform-http:/createCourtCase?httpMethodRestrict=POST")
    .routeId("createCourtCase")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .log("body (before unmarshalling): '${body}'")
    .delay(3000)
    .unmarshal().json()
    .transform(simple("{\"number\": \"${body[number]}\", \"status\": \"created\", \"sensitive_content\": \"${body[sensitive_content]}\", \"public_content\": \"${body[public_content]}\", \"created_datetime\": \"${body[created_datetime]}\"}"))
    .setBody(simple("Court case '${header.number}' processed successfully after 3 seconds."))
    .to("log:info");
  }
}