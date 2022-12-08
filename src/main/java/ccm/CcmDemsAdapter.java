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

import ccm.models.common.data.ApprovedCourtCaseData;
import ccm.models.common.data.AuthUser;
import ccm.models.common.data.AuthUserList;
import ccm.models.common.data.CaseAccused;
import ccm.models.common.data.CaseAppearanceSummaryList;
import ccm.models.common.data.CaseCrownAssignmentList;
import ccm.models.common.data.ChargeAssessmentCaseData;
import ccm.models.common.data.ChargeAssessmentCaseDataRefList;
import ccm.models.system.dems.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.camel.CamelException;
//import org.apache.camel.http.common.HttpOperationFailedException;

public class CcmDemsAdapter extends RouteBuilder {
  @Override
  public void configure() throws Exception {
    
    version();
    dems_version();
    getCourtCaseExists();
    getCourtCaseIdByKey(); 
    getCourtCaseDataById();
    getCourtCaseDataByKey();
    getCourtCaseNameByKey();
    createCourtCase();
    updateCourtCase();
    updateCourtCaseWithMetadata();
    updateCourtCaseWithAppearanceSummary();
    updateCourtCaseWithCrownAssignmentData();
    http_syncCaseUserList();
    syncCaseUserList();
    processAccusedPerson();
    getPersonExists();
    getPersonByKey();
    createPerson();
    updatePerson();
    addParticipantToCase();
    getGroupMapByCaseId();
    prepareDemsCaseGroupMembersSyncHelperList();
    syncCaseGroupMembers();
    getCaseListByUserKey();
  }

  private void version() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/v1/version?httpMethodRestrict=GET")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("version query request received")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .choice()
      .when(simple("${header.authorization} == 'Bearer {{adapter.token}}'"))
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
  }

  private void dems_version() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/dems/v1/version?httpMethodRestrict=GET")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("DEMS version query request received")
    .choice()
      .when(simple("${header.authorization} == 'Bearer {{adapter.token}}'"))
        .removeHeader("CamelHttpUri")
        .removeHeader("CamelHttpBaseUri")
        .removeHeaders("CamelHttp*")
        .setHeader(Exchange.HTTP_METHOD, simple("GET"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
        .to("https://{{dems.host}}/version")
        .log("Response: ${body}")
      .otherwise()
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(401))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .setBody().simple("{ \"message\": \"Authentication error.\" }")
        .log("Response: ${body}")
      .end();
  }

  private void getCourtCaseExists() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
      .routeId(routeId)
      .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
      // .log("Before delay call...")
      // .delay(10000)
      // .log("After delay call.")
      .setProperty("key", simple("${header.event_key}"))
      .log("Key = ${exchangeProperty.key}")
      .to("direct:getCourtCaseIdByKey")
    ;
  }

  private void getCourtCaseIdByKey() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: exchangeProeprty.key
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("key = ${exchangeProperty.key}...")
    .setProperty("dems_org_unit_id").simple("{{dems.org-unit.id}}")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/org-units/${exchangeProperty.dems_org_unit_id}/cases/${exchangeProperty.key}/id?throwExceptionOnFailure=false")
    //.toD("rest:get:org-units/${exchangeProperty.dems_org_unit_id}/cases/${exchangeProperty.key}/id?throwExceptionOnFailure=false&host={{dems.host}}&bindingMode=json&ssl=true")
    //.toD("netty-http:https://{{dems.host}}/org-units/${exchangeProperty.dems_org_unit_id}/cases/${exchangeProperty.key}/id?throwExceptionOnFailure=false")
    .setProperty("length",jsonpath("$.length()"))
      .choice()
        .when(simple("${header.CamelHttpResponseCode} == 200 && ${exchangeProperty.length} > 0"))
          .setProperty("id", jsonpath("$[0].id"))
          .setBody(simple("{\"id\": \"${exchangeProperty.id}\"}"))
        .endChoice()
        .when(simple("${header.CamelHttpResponseCode} == 200"))
          .log("body = '${body}'.")
          .setProperty("id", simple(""))
          .setBody(simple("{\"id\": \"\"}"))
          .setHeader("CamelHttpResponseCode", simple("200"))
          .log("Case not found.")
        .endChoice()
      .end()
    ;
  }

  private void getCourtCaseDataById() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: exchangeProperty.id
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing request (id=${exchangeProperty.id})...")
    .setProperty("dems_org_unit_id").simple("{{dems.org-unit.id}}")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.id}")
    .log("Retrieved court case data by id.")
    ;
  }

  private void getCourtCaseDataByKey() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: exchangeProperty.key
    // OUT: JSON
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing request")
    .to("direct:getCourtCaseIdByKey")
    .setProperty("id", jsonpath("$.id"))
    .to("direct:getCourtCaseDataById")
    ;
  }

  private void getCourtCaseNameByKey() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: exchangeProperty.key
    // OUT: String
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing request")
    .to("direct:getCourtCaseDataByKey")
    .setProperty("courtCaseName",jsonpath("$.name"))
    .log("DEMS court case name (key = ${exchangeProperty.key}): ${exchangeProperty.courtCaseName}")
    .setBody(simple("${exchangeProperty.courtCaseName}"))
    ;
  }

  private void createCourtCase() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing request: ${body}")
    .setProperty("dems_org_unit_id").simple("{{dems.org-unit.id}}")
    .setProperty("CourtCaseMetadata", simple("${bodyAs(String)}"))
    .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentCaseData.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        String caseTemplateId = exchange.getContext().resolvePropertyPlaceholders("{{dems.casetemplate.id}}");
        ChargeAssessmentCaseData b = exchange.getIn().getBody(ChargeAssessmentCaseData.class);
        DemsChargeAssessmentCaseData d = new DemsChargeAssessmentCaseData(caseTemplateId,b);
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsChargeAssessmentCaseData.class)
    .log("DEMS-bound request data: '${body}'")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/org-units/${exchangeProperty.dems_org_unit_id}/cases")
    .log("Court case created.")
    .setProperty("courtCaseId", jsonpath("$.id"))
    .setBody(simple("${exchangeProperty.CourtCaseMetadata}"))
    .split()
      .jsonpathWriteAsString("$.accused_persons")
      .setHeader("key", jsonpath("$.identifier"))
      .setHeader("courtCaseId").simple("${exchangeProperty.courtCaseId}")
      .log("Found accused participant. Key: ${header.number}")
      .to("direct:processAccusedPerson")
    .end()
    ;
  }

  private void updateCourtCase() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing request: ${body}")
    .setProperty("dems_org_unit_id").simple("{{dems.org-unit.id}}")
    .setProperty("CourtCaseMetadata", simple("${bodyAs(String)}"))
    .unmarshal().json(JsonLibrary.Jackson, ChargeAssessmentCaseData.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        String caseTemplateId = exchange.getContext().resolvePropertyPlaceholders("{{dems.casetemplate.id}}");
        ChargeAssessmentCaseData b = exchange.getIn().getBody(ChargeAssessmentCaseData.class);
        DemsChargeAssessmentCaseData d = new DemsChargeAssessmentCaseData(caseTemplateId,b);
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsChargeAssessmentCaseData.class)
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
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}")
    .log("Court case updated.")
    .setProperty("courtCaseId", jsonpath("$.id"))
    .setBody(simple("${exchangeProperty.CourtCaseMetadata}"))
    .split()
      .jsonpathWriteAsString("$.accused_persons")
      .setHeader("key", jsonpath("$.identifier"))
      .setHeader("courtCaseId").simple("${exchangeProperty.dems_case_id}")
      .log("Found accused participant. Key: ${header.key}")
      .to("direct:processAccusedPerson")
    .end()
    ;
  }

  private void updateCourtCaseWithMetadata() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.rcc_id
    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing request: ${body}")
    .setProperty("metadata_data", simple("${bodyAs(String)}"))
    .setProperty("dems_org_unit_id").simple("{{dems.org-unit.id}}")
    .setProperty("key", simple("${header.rcc_id}"))
    .unmarshal().json(JsonLibrary.Jackson, ApprovedCourtCaseData.class)
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
        ApprovedCourtCaseData bcm = exchange.getProperty("CourtCaseMetadata", ApprovedCourtCaseData.class);
        DemsApprovedCourtCaseData d = new DemsApprovedCourtCaseData(key, courtCaseName, bcm);
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsApprovedCourtCaseData.class)
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
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}")
    .log("Court case updated.")
    .log("Create participants")
    .setBody(simple("${exchangeProperty.metadata_data}"))
    .split()
      .jsonpathWriteAsString("$.accused_persons")
      .setHeader("key", jsonpath("$.identifier"))
      .setHeader("courtCaseId").simple("${exchangeProperty.dems_case_id}")
      .log("Found accused participant. Key: ${header.key} Case Id: ${header.courtCaseId}")
      .to("direct:processAccusedPerson")
    .end()
    ;
  }

  private void updateCourtCaseWithAppearanceSummary() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.rcc_id
    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing request: ${body}")
    .setProperty("dems_org_unit_id").simple("{{dems.org-unit.id}}}")
    .setProperty("key", simple("${header.rcc_id}"))
    .unmarshal().json(JsonLibrary.Jackson, CaseAppearanceSummaryList.class)
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
        CaseAppearanceSummaryList b = exchange.getProperty("business_data", CaseAppearanceSummaryList.class);
        DemsCaseAppearanceSummaryData d = new DemsCaseAppearanceSummaryData(key, courtCaseName, b);
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsCaseAppearanceSummaryData.class)
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
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}")
    .log("Court case updated.")
    ;
  }

  private void updateCourtCaseWithCrownAssignmentData() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.rcc_id
    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing request: ${body}")
    .setProperty("dems_org_unit_id").simple("{{dems.org-unit.id}}")
    .setProperty("key", simple("${header.rcc_id}"))
    .unmarshal().json(JsonLibrary.Jackson, CaseCrownAssignmentList.class)
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
        CaseCrownAssignmentList b = exchange.getProperty("business_data", CaseCrownAssignmentList.class);
        DemsCaseCrownAssignmentData d = new DemsCaseCrownAssignmentData(key, courtCaseName, b);
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsCaseCrownAssignmentData.class)
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
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}")
    .log("Court case updated.")
    ;
  }

  private void http_syncCaseUserList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/syncCaseUserList")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .setProperty("key", simple("${header.event_key}"))
    //.setBody(simple("{\"rcc_id\":\"50433.0734\",\"auth_users_list\":[{\"part_id\":\"11429.0026\",\"crown_agency\":null,\"user_name\":null},{\"part_id\":\"85056.0734\",\"crown_agency\":null,\"user_name\":null},{\"part_id\":\"85062.0734\",\"crown_agency\":null,\"user_name\":null},{\"part_id\":\"85170.0734\",\"crown_agency\":null,\"user_name\":null}]}"))
    .setBody(simple("${header.temp-body}"))
    .removeHeader("temp-body")
    .log("Processing request (event_key = ${exchangeProperty.key}): ${body}")
    .to("direct:syncCaseUserList");
  }

  private void syncCaseUserList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing request: ${body}")
    .setProperty("dems_org_unit_id").simple("{{dems.org-unit.id}}")
    .unmarshal().json(JsonLibrary.Jackson, AuthUserList.class)
    .process(new Processor() {
      public void process(Exchange exchange) {
        AuthUserList b = exchange.getIn().getBody(AuthUserList.class);
        DemsAuthUsersList da = new DemsAuthUsersList(b);
        exchange.getMessage().setBody(da);
        exchange.setProperty("auth_user_list_object", b);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsAuthUsersList.class)
    .setProperty("dems_auth_user_list").simple("${body}")
    .log("DEMS-bound case users sync request data: '${body}'.")
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
    .setHeader("Authorization", simple("Bearer " + "{{dems.token}}"))
    .setBody(simple("${exchangeProperty.dems_auth_user_list}"))
    .log("Synchronizing case users ...")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/case-users/sync")
    .log("Case users synchronized.")
    // retrieve DEMS case group map
    .to("direct:getGroupMapByCaseId")
    // create DEMS case group members sync helper list
    .to("direct:prepareDemsCaseGroupMembersSyncHelperList")
    // sync case group members
    .to("direct:syncCaseGroupMembers")
    ;
  }

  private void prepareDemsCaseGroupMembersSyncHelperList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: exchangeProperty.auth_user_list_object
    // IN: exchangeProperty.dems_case_group_map
    // ---
    // OUT: exchangeProperty.dems_case_group_members_sync_helper_list
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .process(new Processor() {
      public void process(Exchange exchange) {
        AuthUserList userList = (AuthUserList)exchange.getProperty("auth_user_list_object");
        DemsCaseGroupMap demsCaseGroupMapForCase = (DemsCaseGroupMap)exchange.getProperty("dems_case_group_map");
        
        List<DemsCaseGroupMembersSyncHelper> demsGroupMembersSyncHelperList = new ArrayList<DemsCaseGroupMembersSyncHelper>();

        // create an empty DEMS case group members map with all case groups.
        HashMap<String,DemsCaseGroupMembersSyncData> demsGroupMembersMapByName = new HashMap<String,DemsCaseGroupMembersSyncData>();
        for (DemsListItemFieldData.CASE_GROUP_FIELD_MAPPINGS demsCaseGroup : DemsListItemFieldData.CASE_GROUP_FIELD_MAPPINGS.values()) {
          DemsCaseGroupMembersSyncData emptySyncData = new DemsCaseGroupMembersSyncData();
          demsGroupMembersMapByName.put(demsCaseGroup.getDems_name(),emptySyncData);
        }

        // iterate through auth user list
        for (AuthUser user : userList.getAuth_user_list()) {
          DemsListItemFieldData.CASE_GROUP_FIELD_MAPPINGS demsCaseGroupListMapping = DemsListItemFieldData.CASE_GROUP_FIELD_MAPPINGS.findCaseGroupByJustinName(user.getJrs_role());

          String demsCaseGroupName = (demsCaseGroupListMapping == null) ? null : demsCaseGroupListMapping.getDems_name();
          Long demsCaseGroupId = (demsCaseGroupListMapping == null) ? null : demsCaseGroupMapForCase.getIdByName(demsCaseGroupName);

          if (demsCaseGroupId != null) {
            DemsCaseGroupMembersSyncData syncData = demsGroupMembersMapByName.get(demsCaseGroupName);

            // check if sync data is found
            if (syncData != null) {
              // add user to sync data
              syncData.getValues().add(user.getPart_id());
              System.out.println("DEBUG: User added to sync data for DEMS group '" + demsCaseGroupName + "' (id=" + demsCaseGroupId + "), user id = " + user.getPart_id());
            }
          } else {
            System.out.println("ERROR: Cannot add user sync data for DEMS group '" + demsCaseGroupName + "' (id=" + demsCaseGroupId + "), user id = " + user.getPart_id() + ", user JRS role = " + user.getJrs_role());
          }
        }

        for (String actualDemsCaseGroupName : demsCaseGroupMapForCase.getMap().keySet()) {
          Long actualDemsCaseGroupId = demsCaseGroupMapForCase.getIdByName(actualDemsCaseGroupName);
          DemsCaseGroupMembersSyncData syncData = demsGroupMembersMapByName.get(actualDemsCaseGroupName);
          DemsCaseGroupMembersSyncHelper helper = null;

          if (syncData != null) {
            // add sync data to helper list
            helper = new DemsCaseGroupMembersSyncHelper(actualDemsCaseGroupId, actualDemsCaseGroupName, syncData);
            System.out.println("DEBUG: found case group: " + actualDemsCaseGroupName);
          } else {
            // add empty sync data to helper list
            DemsCaseGroupMembersSyncData emptySyncData = new DemsCaseGroupMembersSyncData();
            helper = new DemsCaseGroupMembersSyncHelper(actualDemsCaseGroupId, actualDemsCaseGroupName, emptySyncData);
            System.out.println("ERROR: did not find case group: " + actualDemsCaseGroupName);
          }

          demsGroupMembersSyncHelperList.add(helper);
        }
        exchange.setProperty("dems_case_group_members_sync_helper_list", demsGroupMembersSyncHelperList);
      }
    })
    ;
  }

  private void syncCaseGroupMembers() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: exchangeProperty.dems_case_id
    // IN: exchangeProperty.dems_case_group_members_sync_helper_list
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Case group sync processing started.")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization", simple("Bearer " + "{{dems.token}}"))
    .setBody(simple("${exchangeProperty.dems_case_group_members_sync_helper_list}"))
    .marshal().json()
    .log("body = '${body}'")
    .split().jsonpathWriteAsString("$")
      .setProperty("dems_case_group_name", jsonpath("$.caseGroupName"))
      .setProperty("dems_case_group_id", jsonpath("$.caseGroupId"))
      .unmarshal().json(JsonLibrary.Jackson, DemsCaseGroupMembersSyncHelper.class)
      .process(new Processor() {
        public void process(Exchange exchange) {
          DemsCaseGroupMembersSyncHelper helper = (DemsCaseGroupMembersSyncHelper)exchange.getIn().getBody();
          exchange.getMessage().setBody(helper.getSyncData());
        }
      })
      .marshal().json(JsonLibrary.Jackson, DemsCaseGroupMembersSyncData.class)
      .log("Syncing case group (name='${exchangeProperty.dems_case_group_name}', id='${exchangeProperty.dems_case_group_id}'). sync data = '${body}' ...")
      .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/groups/${exchangeProperty.dems_case_group_id}/sync")
      .log("Case group (name='${exchangeProperty.dems_case_group_name}', id='${exchangeProperty.dems_case_group_id}') members synchronized.")
      .end()
    .log("Case group sync processing completed.")
    ;
  }

  private void processAccusedPerson() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.key
    // IN: header.courtCaseId
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("processAccusedPerson.  key = ${header[key]}")
    .setProperty("person_data", simple("${bodyAs(String)}"))
    .log("Accused Person data = ${body}.")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("key").simple("${header.key}")
    .log("Check whether person exists in DEMS")
    .to("direct:getPersonExists")
    .log("${body}")
    .unmarshal().json()
    .setProperty("personFound").simple("${body[id]}")
    .setHeader("organizationId").jsonpath("$.orgs[0].organisationId", true)
    .setHeader("key").simple("${header.key}")
    .setHeader("courtCaseId").simple("${header.courtCaseId}")
    .setBody(simple("${exchangeProperty.person_data}"))
    .choice()
      .when(simple("${exchangeProperty.personFound} == ''"))
        .to("direct:createPerson")
      .endChoice()
      .otherwise()
        .log("PersonId: ${exchangeProperty.personFound}")
        .setHeader("personId").simple("${exchangeProperty.personFound}")
        .log("OrganizationId: ${header.organizationId}")
        .to("direct:updatePerson")
      .endChoice()
      .end()
    .setHeader("key").simple("${header.key}")
    .setHeader("courtCaseId").simple("${header.courtCaseId}")
    .to("direct:addParticipantToCase")
    ;
  }

  private void getPersonExists() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.key
    from("direct:" + routeId)
      .routeId(routeId)
      .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
      .setProperty("key", simple("${header.key}"))
      .to("direct:getPersonByKey")
    ;
  }

  private void getPersonByKey() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.key
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing request (key=${header[key]})...")
    .setProperty("dems_org_unit_id").simple("{{dems.org-unit.id}}")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/org-units/${exchangeProperty.dems_org_unit_id}/persons/${header[key]}?throwExceptionOnFailure=false")
    .choice()
      .when().simple("${header.CamelHttpResponseCode} == 200")
        // person found
        .setProperty("id", jsonpath("$.id"))
        .log("Participant found. Id = ${exchangeProperty.id}")
        .endChoice()
      .when().simple("${header.CamelHttpResponseCode} == 404")
        // person not found
        .log("Participant not found.")
        .setBody(simple("{\"id\": \"\"}"))
        .setHeader("CamelHttpResponseCode", simple("200"))
        .endChoice()
    .end()
    ;
  }

  private void createPerson() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing request: ${body}")
    .setProperty("dems_org_unit_id").simple("{{dems.org-unit.id}}")
    .setProperty("PersonData").body()
    .unmarshal().json(JsonLibrary.Jackson, CaseAccused.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        CaseAccused b = exchange.getIn().getBody(CaseAccused.class);
        DemsPersonData d = new DemsPersonData(b);
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsPersonData.class)
    .log("DEMS-bound request data: '${body}'")
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/org-units/${exchangeProperty.dems_org_unit_id}/persons")
    .log("Person created.")
    ;
  }

  private void updatePerson() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.key
    // IN: header.personId
    // IN: header.organizationId
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log("Processing request: ${body}")
    .setProperty("dems_org_unit_id").simple("{{dems.org-unit.id}}")
    .setProperty("PersonData").body()
    .setProperty("personId").simple("${header[personId]}")
    .setProperty("organizationId").simple("${header[organizationId]}")
    .unmarshal().json(JsonLibrary.Jackson, CaseAccused.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) {
        CaseAccused b = exchange.getIn().getBody(CaseAccused.class);
        DemsPersonData d = new DemsPersonData(b);
        String personId = exchange.getProperty("personId", String.class);
        String organizationId = exchange.getProperty("organizationId", String.class);
        d.setId(personId);
        DemsOrganisationData o = new DemsOrganisationData(organizationId);
        d.setOrgs(new ArrayList<DemsOrganisationData>());
        d.getOrgs().add(o);
        exchange.getMessage().setBody(d);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsPersonData.class)
    .log("DEMS-bound request data: '${body}'")
    .setProperty("update_data", simple("${body}"))
    // update case
    .setBody(simple("${exchangeProperty.update_data}"))
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .toD("https://{{dems.host}}/org-units/${exchangeProperty.dems_org_unit_id}/persons/${header[key]}")
    .log("Person updated.")
    ;
  }

  private void addParticipantToCase() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header.key
    // IN: header.courtCaseId
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html"{{dems.host}}
    .log("Processing request: ${body}")
    .setProperty("participantType").simple("Accused")
    .setProperty("key").simple("${header.key}")
    .setProperty("courtCaseId").simple("${header.courtCaseId}")
    .log("addParticipantToCase.  key = ${header[key]} case = ${header[courtCaseId]}")
    .choice()
      .when(simple("${header[courtCaseId]} != '' && ${header[courtCaseId]} != null"))
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) {
            String key = exchange.getProperty("key", String.class);
            String participantType = exchange.getProperty("participantType", String.class);
            DemsCaseParticipantData d = new DemsCaseParticipantData(key, participantType);
            exchange.getMessage().setBody(d);
          }
        })
        .marshal().json(JsonLibrary.Jackson, DemsCaseParticipantData.class)
        .log("DEMS-bound request data: '${body}'")
        .removeHeader("CamelHttpUri")
        .removeHeader("CamelHttpBaseUri")
        .removeHeaders("CamelHttp*")
        .setHeader(Exchange.HTTP_METHOD, simple("POST"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
        .toD("https://{{dems.host}}/cases/${exchangeProperty.courtCaseId}/participants")
        .log("Person added to case.")
      .endChoice()
    .otherwise()
      .log("Court case id was not defined. Skipped linking to a case.")
    .endChoice()
    ;
  }

  private void getGroupMapByCaseId() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
  
    // IN: exchangeProperty.dems_case_id
    // OUT: exchangeProperty.dems_case_group_map object
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .log("Looking up case groups (case id = ${exchangeProperty.dems_case_id}) ...")
    .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/groups?throwExceptionOnFailure=false")
    .choice()
      .when().simple("${header.CamelHttpResponseCode} == 200")
        // create initial case group map
        .convertBodyTo(String.class)
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) {
            DemsCaseGroupMap caseGroupMap = new DemsCaseGroupMap((String)exchange.getIn().getBody());
            exchange.setProperty("dems_case_group_map", caseGroupMap);
          }
        })
        .endChoice()
      .otherwise()
        .log(LoggingLevel.ERROR,"Case groups lookup error.  " + 
            "Response status code = ${header.CamelHttpResponseCode}.  " + 
            "Response body = '${body}'.  " + 
            "Assume no case groups.")
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) {
            DemsCaseGroupMap caseGroupMap = new DemsCaseGroupMap();
            exchange.setProperty("dems_case_group_map", caseGroupMap);
          }
        })
        .setHeader("CamelHttpResponseCode", simple("200"))
        .endChoice()
    .end()
    ;
  }

  private void getCaseListByUserKey() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
  
    // IN: header.key
    // OUT: body as ChargeAssessmentCaseDataRefList
    from("platform-http:/" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    .log("Looking up case list by user key (${header.key}) ...")
    .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/users/key:${header.key}/cases?throwExceptionOnFailure=false")
    .choice()
      .when().simple("${header.CamelHttpResponseCode} == 200")
        // create initial case list
        .convertBodyTo(String.class)
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) {
            DemsCaseRefList demsCaseList = new DemsCaseRefList((String)exchange.getIn().getBody());
            ChargeAssessmentCaseDataRefList caseList = new ChargeAssessmentCaseDataRefList(demsCaseList);
            exchange.getMessage().setBody(caseList);
            exchange.setProperty("case_list_size", caseList.getCase_list().size());
          }
        })
        .marshal().json(JsonLibrary.Jackson, ChargeAssessmentCaseDataRefList.class)
        .log("User found; case list size = ${exchangeProperty.case_list_size}.")
        .endChoice()
      .when().simple("${header.CamelHttpResponseCode} == 404")
        .log("User not found.  Message from DEMS: ${body}")
        .endChoice()
    .end()
    ;
  }
}