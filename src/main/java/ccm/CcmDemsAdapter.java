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

import ccm.models.business.*;
import ccm.models.system.dems.*;

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
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
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
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
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
      .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
      .setProperty("key", simple("${header.event_object_id}"))
      .to("direct:getCourtCaseIdByKey")
    ;
      
    // IN: exchangeProperty.key
    from("direct:getCourtCaseIdByKey")
    .routeId("direct:getCourtCaseIdByKey")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing getCourtCaseIdByKey request (key=${exchangeProperty.key})...")
    .setProperty("dems_org_unit_id").simple("1")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{token.dems}}")
    .doTry()
      // JADE-1453 - workaround: introduced httpClient.soTimeout parameter.
      //   Observation: the timeout issue has not re-occurred since the introduction of the following logging, as well as the timeout setting.
      .toD("{{dems.host}}/org-units/${exchangeProperty.dems_org_unit_id}/cases/${exchangeProperty.key}/id?httpClient.soTimeout=30")
      .setProperty("id", jsonpath("$.id"))
      .log("Case found. Id = ${exchangeProperty.id}")
    //.doCatch(HttpOperationFailedException.class)
    .doCatch(CamelException.class)
      .log("Exception: ${exception}")
      .log("Exchange: ${exchange}")
      .choice()
        .when().simple("${exception.statusCode} == 404")
          .log(LoggingLevel.INFO,"Record not found (404).")
          ////.toD("splunk-hec://hec.monitoring.ag.gov.bc.ca:8088/services/collector")
          .setBody(simple("{\"id\": \"\"}"))
        .endChoice()
        .otherwise()
          .log(LoggingLevel.ERROR,"Unknown error.  Re-throw ${exception}")
          .process(new Processor() {
            public void process(Exchange exchange) throws Exception {
              throw exchange.getException();
            }
          })
        .endChoice()
      .end()
    .endDoTry()
    ;
      
    // IN: exchangeProperty.id
    from("direct:getCourtCaseDataById")
    .routeId("direct:getCourtCaseDataById")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing getCourtCaseDataById request (id=${exchangeProperty.id})...")
    .setProperty("dems_org_unit_id").simple("1")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{token.dems}}")
    .toD("{{dems.host}}/cases/${exchangeProperty.id}")
    .log("Retrieved court case data by id: ${body}")
    ;

    // IN: exchangeProperty.key
    // OUT: JSON
    from("direct:getCourtCaseDataByKey")
    .routeId("direct:getCourtCaseDataByKey")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing getCourtCaseDataByKey")
    .to("direct:getCourtCaseIdByKey")
    .setProperty("id", jsonpath("$.id"))
    .to("direct:getCourtCaseDataById")
    ;

    // IN: exchangeProperty.key
    // OUT: String
    from("direct:getCourtCaseNameByKey")
    .routeId("direct:getCourtCaseNameByKey")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing getCourtCaseNameByKey")
    .to("direct:getCourtCaseDataByKey")
    .setProperty("courtCaseName",jsonpath("$.name"))
    .log("DEMS court case name (key = ${exchangeProperty.key}): ${exchangeProperty.courtCaseName}")
    .setBody(simple("${exchangeProperty.courtCaseName}"))
    ;
      
    from("platform-http:/createCourtCase")
    .routeId("createCourtCase")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing createCourtCase request: ${body}")
    .setProperty("dems_org_unit_id").simple("1")
    .unmarshal().json(JsonLibrary.Jackson, BusinessCourtCaseData.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        BusinessCourtCaseData b = exchange.getIn().getBody(BusinessCourtCaseData.class);
        DemsCourtCaseData d = new DemsCourtCaseData(b);
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsCourtCaseData.class)
    .log("DEMS-bound request data: '${body}'")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{token.dems}}")
    .doTry()
      .toD("{{dems.host}}/org-units/${exchangeProperty.dems_org_unit_id}/cases")
    .doCatch(Exception.class)
      .log(LoggingLevel.ERROR, "Exception: ${exception}")
      .process(new Processor() {
        public void process(Exchange exchange) throws Exception {
          throw exchange.getException();
        }
      })
    .endDoTry()
    .log("Court case created.")
    ;
      
    from("platform-http:/updateCourtCase")
    .routeId("updateCourtCase")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing updateCourtCase request: ${body}")
    .setProperty("dems_org_unit_id").simple("1")
    .unmarshal().json(JsonLibrary.Jackson, BusinessCourtCaseData.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        BusinessCourtCaseData b = exchange.getIn().getBody(BusinessCourtCaseData.class);
        DemsCourtCaseData d = new DemsCourtCaseData(b);
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsCourtCaseData.class)
    .log("DEMS-bound request data: '${body}'")
    .setProperty("update_data", simple("${body}"))
    // get case id
    .setProperty("key", jsonpath("$.key"))
    .to("direct:getCourtCaseIdByKey")
    .setProperty("dems_case_id", jsonpath("$.id"))
    // update case
    .setBody(simple("${exchangeProperty.update_data}"))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{token.dems}}")
    .toD("{{dems.host}}/cases/${exchangeProperty.dems_case_id}")
    .log("Court case updated.")
    ;
      
    // IN: header.rcc_id
    from("platform-http:/updateCourtCaseWithMetadata")
    .routeId("updateCourtCaseWithMetadata")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing updateCourtCaseWithMetadata request: ${body}")
    .setProperty("dems_org_unit_id").simple("1")
    .setProperty("key", simple("${header.rcc_id}"))
    .unmarshal().json(JsonLibrary.Jackson, BusinessCourtCaseMetadataData.class)
    .setProperty("CourtCaseMetadata").body()
    // retrieve court case name from DEMS
    .to("direct:getCourtCaseNameByKey")
    .setProperty("courtCaseName",simple("${bodyAs(String)}"))
    .log("getCourtCaseNameByKey: ${exchangeProperty.courtCaseName}")
    // generate DEMS court case metatdata
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        String key = exchange.getProperty("key", String.class);
        String courtCaseName = exchange.getProperty("courtCaseName", String.class);
        BusinessCourtCaseMetadataData bcm = exchange.getProperty("CourtCaseMetadata", BusinessCourtCaseMetadataData.class);
        DemsCourtCaseMetadataData d = new DemsCourtCaseMetadataData(key, courtCaseName, bcm);
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsCourtCaseMetadataData.class)
    .log("DEMS-bound request data: '${body}'")
    .setProperty("update_data", simple("${body}"))
    // get case id
    .setProperty("key", jsonpath("$.key"))
    .to("direct:getCourtCaseIdByKey")
    .setProperty("dems_case_id", jsonpath("$.id"))
    // update case
    .setBody(simple("${exchangeProperty.update_data}"))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{token.dems}}")
    .toD("{{dems.host}}/cases/${exchangeProperty.dems_case_id}")
    .log("Court case updated.")
    ;
      
    // IN: header.rcc_id
    from("platform-http:/updateCourtCaseWithAppearanceSummary")
    .routeId("updateCourtCaseWithAppearanceSummary")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing updateCourtCaseWithAppearanceSummary request: ${body}")
    .setProperty("dems_org_unit_id").simple("1")
    .setProperty("key", simple("${header.rcc_id}"))
    .unmarshal().json(JsonLibrary.Jackson, BusinessCourtCaseAppearanceSummaryList.class)
    .setProperty("business_data").body()
    // retrieve court case name from DEMS
    .to("direct:getCourtCaseNameByKey")
    .setProperty("courtCaseName",simple("${bodyAs(String)}"))
    .log("getCourtCaseNameByKey: ${exchangeProperty.courtCaseName}")
    // generate DEMS court case appearance summary
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        String key = exchange.getProperty("key", String.class);
        String courtCaseName = exchange.getProperty("courtCaseName", String.class);
        BusinessCourtCaseAppearanceSummaryList b = exchange.getProperty("business_data", BusinessCourtCaseAppearanceSummaryList.class);
        DemsCourtCaseAppearanceSummaryData d = new DemsCourtCaseAppearanceSummaryData(key, courtCaseName, b);
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsCourtCaseAppearanceSummaryData.class)
    .log("DEMS-bound request data: '${body}'")
    .setProperty("update_data", simple("${body}"))
    // get case id
    .setProperty("key", jsonpath("$.key"))
    .to("direct:getCourtCaseIdByKey")
    .setProperty("dems_case_id", jsonpath("$.id"))
    // update case
    .setBody(simple("${exchangeProperty.update_data}"))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{token.dems}}")
    .toD("{{dems.host}}/cases/${exchangeProperty.dems_case_id}")
    .log("Court case updated.")
    ;
      
    // IN: header.rcc_id
    from("platform-http:/updateCourtCaseWithCrownAssignmentData")
    .routeId("updateCourtCaseWithCrownAssignmentData")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing updateCourtCaseWithCrownAssignmentData request: ${body}")
    .setProperty("dems_org_unit_id").simple("1")
    .setProperty("key", simple("${header.rcc_id}"))
    .unmarshal().json(JsonLibrary.Jackson, BusinessCourtCaseCrownAssignmentList.class)
    .setProperty("business_data").body()
    // retrieve court case name from DEMS
    .to("direct:getCourtCaseNameByKey")
    .setProperty("courtCaseName",simple("${bodyAs(String)}"))
    .log("getCourtCaseNameByKey: ${exchangeProperty.courtCaseName}")
    // generate DEMS court case crown assignment data
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        String key = exchange.getProperty("key", String.class);
        String courtCaseName = exchange.getProperty("courtCaseName", String.class);
        BusinessCourtCaseCrownAssignmentList b = exchange.getProperty("business_data", BusinessCourtCaseCrownAssignmentList.class);
        DemsCourtCaseCrownAssignmentData d = new DemsCourtCaseCrownAssignmentData(key, courtCaseName, b);
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsCourtCaseCrownAssignmentData.class)
    .log("DEMS-bound request data: '${body}'")
    .setProperty("update_data", simple("${body}"))
    // get case id
    .setProperty("key", jsonpath("$.key"))
    .to("direct:getCourtCaseIdByKey")
    .setProperty("dems_case_id", jsonpath("$.id"))
    // update case
    .setBody(simple("${exchangeProperty.update_data}"))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{token.dems}}")
    .toD("{{dems.host}}/cases/${exchangeProperty.dems_case_id}")
    .log("Court case updated.")
    ;
      
    from("platform-http:/syncCaseUserList")
    .routeId("rest-syncCaseUserList")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("key", simple("${header.event_object_id}"))
    //.setBody(simple("{\"rcc_id\":\"50433.0734\",\"auth_users_list\":[{\"part_id\":\"11429.0026\",\"crown_agency\":null,\"user_name\":null},{\"part_id\":\"85056.0734\",\"crown_agency\":null,\"user_name\":null},{\"part_id\":\"85062.0734\",\"crown_agency\":null,\"user_name\":null},{\"part_id\":\"85170.0734\",\"crown_agency\":null,\"user_name\":null}]}"))
    .setBody(simple("${header.temp-body}"))
    .removeHeader("temp-body")
    .log("Processing rest-syncCaseUserList request (event_object_id = ${exchangeProperty.event_object_id}): ${body}")
    .to("direct:syncCaseUserList");

    from("direct:syncCaseUserList")
    .routeId("syncCaseUserList")
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing syncCaseUserList request: ${body}")
    .setProperty("dems_org_unit_id").simple("1")
    .unmarshal().json(JsonLibrary.Jackson, BusinessAuthUsersList.class)
    .process(new Processor() {
      public void process(Exchange exchange) {
        BusinessAuthUsersList b = exchange.getIn().getBody(BusinessAuthUsersList.class);
        DemsAuthUsersList da = new DemsAuthUsersList(b);
        exchange.getMessage().setBody(da);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsAuthUsersList.class)
    .setProperty("demsAuthUserList").simple("${body}")
    .log("DEMS-bound case users sync request data: '${body}'")
    .setProperty("sync_data", simple("${body}"))
    // get case id
    // exchangeProperty.key already set
    .to("direct:getCourtCaseIdByKey")
    .setProperty("dems_case_id", jsonpath("$.id"))
    // sync case users
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization", simple("Bearer " + "{{token.dems}}"))
    .setBody(simple("${exchangeProperty.demsAuthUserList}"))
    .toD("{{dems.host}}/cases/${exchangeProperty.dems_case_id}/case-users/sync")
    .log("Case users synchronized.")
    //
    // sync case group members
    .setBody(simple("${exchangeProperty.demsAuthUserList}"))
    .unmarshal().json(JsonLibrary.Jackson, DemsAuthUsersList.class)
    .process(new Processor() {
      public void process(Exchange exchange) {
        DemsAuthUsersList da = exchange.getIn().getBody(DemsAuthUsersList.class);
        DemsGroupMembersSyncData dg = new DemsGroupMembersSyncData(da);
        exchange.getMessage().setBody(dg);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsGroupMembersSyncData.class)
    .log("DEMS-bound case group members sync request data: '${body}'")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization", simple("Bearer " + "{{token.dems}}"))
    .toD("{{dems.host}}/cases/${exchangeProperty.dems_case_id}/groups/0/sync")
    .log("Case group members synchronized.")
    ;
  }
}