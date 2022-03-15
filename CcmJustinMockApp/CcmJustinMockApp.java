// To run this integration in OpenShift in server mode, use:
// kamel run CcmJustinMockApp.java --profile openshift
//
// Add '--dev' to run in dev mode
// 
// curl http://ccm-justin-mock-app/courtFileCreated?number=1
//

import java.util.Calendar;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

public class CcmJustinMockApp extends RouteBuilder {
  @Override
  public void configure() throws Exception {
    from("platform-http:/courtFileCreated?httpMethodRestrict=GET")
    .routeId("courtFileCreated")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setProperty("number").simple("${header.number}")
    .process(new Processor() {
      public void process(Exchange ex) {
        // https://stackoverflow.com/questions/12008472/get-and-format-yesterdays-date-in-camels-expression-language
        Calendar createdCal = Calendar.getInstance();
        createdCal.add(Calendar.DATE, 0);
        ex.getIn().setHeader("created_datetime", createdCal.getTime());
      }
    })
    .setBody().simple("{\"number\": \"${exchangeProperty.number}\", \"sensitive_content\": \"Shh... this is the secret.\", \"public_content\": \"This is a mock event object.\", \"created_datetime\": \"${date:header.created_datetime:yyyy-MM-dd'T'HH:mm:ssX}\"}")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .to("http://ccm-justin-utility-adapter/courtFileCreated")
    .setBody().simple("Successfully generated new court file: number=${exchangeProperty.number}.")
    //.log("Newly generated court file: ${body}");
    .log("${body}");

    from("platform-http:/greeting?httpMethodRestrict=GET")
    .routeId("greeting")
    .setProperty("name").simple("${header.name}")
    .setBody().simple("<html><body><h2>Hello ${exchangeProperty.name}</body></html>")
    .log("Greeting: Hello ${exchangeProperty.name}.");
  }
}