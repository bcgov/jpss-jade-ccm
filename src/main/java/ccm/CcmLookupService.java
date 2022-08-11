package ccm;

// To run this integration use:
// kamel run CcmLookupService.java --property file:application.properties --profile openshift
// 
// curl -H "user_id: 2" -H "court_case_number: 6" http://ccm-lookup-service/getCourtCaseDetails
//

// camel-k: language=java
// camel-k: dependency=mvn:org.apache.camel.quarkus:camel-quarkus-kafka:camel-quarkus-jsonpath:camel-jackson:camel-splunk-hec

import java.util.Calendar;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

public class CcmLookupService extends RouteBuilder {
  @Override
  public void configure() throws Exception {
    from("platform-http:/getCourtCaseDetails_old?httpMethodRestrict=GET")
    .routeId("getCourtCaseDetails_old")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .process(new Processor() {
      public void process(Exchange ex) {
        // https://stackoverflow.com/questions/12008472/get-and-format-yesterdays-date-in-camels-expression-language
        Calendar createdCal = Calendar.getInstance();
        createdCal.add(Calendar.DATE, 0);
        ex.getIn().setHeader("audit_datetime", createdCal.getTime());
      }
    })
    .transform(simple("{\"audit_type\": \"get_court_case_details\", \"user_id\": \"${header.user_id}\", \"court_case_number\": \"${header.court_case_number}\", \"audit_datetime\": \"${header.audit_datetime}\"}"))
    .log("body (after transform): '${body}'")
    .to("kafka:{{kafka.topic.name}}")
    ;

    from("platform-http:/getCourtCaseExists")
    .routeId("getCourtCaseExists")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    //.setProperty("name",simple("${header[number]}"))
    .log("Processing getCourtCaseExists request... number = ${header[number]}")
    .to("http://ccm-dems-adapter/getCourtCaseExists?number=${header[number]}")
    ;

    from("platform-http:/getCourtCaseDetails")
    .routeId("getCourtCaseDetails")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setProperty("name",simple("${header.number}"))
    .log("Processing getCourtCaseDetails request... number = ${header[number]}")
    .to("http://ccm-dems-adapter/getCourtCaseExists")
    ;
  }
}