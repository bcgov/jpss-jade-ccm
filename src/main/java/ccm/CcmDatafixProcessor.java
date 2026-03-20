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


import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import ccm.models.system.dems.DemsCaseList;
import ccm.models.system.dems.DemsCaseStatus;
import ccm.models.system.dems.DemsFieldData;
import ccm.models.system.dems.DemsPersonData;
import ccm.utils.JsonParseUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;


public class CcmDatafixProcessor extends RouteBuilder {
  @Override
  public void configure() throws Exception {

    getCourtCaseDataByKey();
    getCourtCaseDataById();
    getCourtCaseStatusById();
    updateCaseCourtFileList();
    updateCaseCourtFile();
    updateExistingParticipantwithOTCV2();
    processParticipantsList();
    updateOtcParticipants();
  }

  private void getCourtCaseStatusById() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    //IN: exchangeProperty.id
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html

    .process(exchange -> {
      DemsCaseStatus emptyCase = new DemsCaseStatus();
      exchange.getMessage().setBody(emptyCase);
    })
    .marshal().json(JsonLibrary.Jackson, DemsCaseStatus.class)
    .setProperty("caseNotFound").simple("${bodyAs(String)}")

    .log(LoggingLevel.INFO, "caseId: '${exchangeProperty.id}'")
    .choice()
      .when(simple("${exchangeProperty.id} != ''"))
        .doTry()
          .to("direct:getCourtCaseDataById")
          .choice()
            .when(simple("${header.CamelHttpResponseCode} == 200"))
              .setProperty("DemsCourtCase", simple("${bodyAs(String)}"))
              .process(new Processor() {
                @Override
                public void process(Exchange exchange) {
                  DemsCaseStatus caseStatus = new DemsCaseStatus();

                  String courtCaseJson = exchange.getProperty("DemsCourtCase", String.class);
                  String caseId = JsonParseUtils.getJsonElementValue(courtCaseJson, "id");
                  String caseKey = JsonParseUtils.getJsonElementValue(courtCaseJson, "key");
                  String caseName = JsonParseUtils.getJsonElementValue(courtCaseJson, "name");
                  String courtFileUniqueId = JsonParseUtils.getJsonArrayElementValue(courtCaseJson, "/fields", "/name", DemsFieldData.FIELD_MAPPINGS.MDOC_JUSTIN_NO.getLabel(), "/value");
                  String courtFile = JsonParseUtils.getJsonArrayElementValue(courtCaseJson, "/fields", "/name", DemsFieldData.FIELD_MAPPINGS.COURT_FILE.getLabel(), "/value");
                  String courtFileNo = JsonParseUtils.getJsonArrayElementValue(courtCaseJson, "/fields", "/name", DemsFieldData.FIELD_MAPPINGS.COURT_FILE_NO.getLabel(), "/value");
                  String caseState = JsonParseUtils.getJsonArrayElementValue(courtCaseJson, "/fields", "/name", DemsFieldData.FIELD_MAPPINGS.CASE_STATE.getLabel(),"/value");
                  String primaryAgencyFileId = JsonParseUtils.getJsonArrayElementValue(courtCaseJson, "/fields", "/name", DemsFieldData.FIELD_MAPPINGS.PRIMARY_AGENCY_FILE_ID.getLabel(),"/value");
                  String primaryAgencyFileNo = JsonParseUtils.getJsonArrayElementValue(courtCaseJson, "/fields", "/name", DemsFieldData.FIELD_MAPPINGS.PRIMARY_AGENCY_FILE_NO.getLabel(),"/value");
                  String agencyFileId = JsonParseUtils.getJsonArrayElementValue(courtCaseJson, "/fields", "/name", DemsFieldData.FIELD_MAPPINGS.AGENCY_FILE_ID.getLabel(),"/value");
                  String agencyFileNo = JsonParseUtils.getJsonArrayElementValue(courtCaseJson, "/fields", "/name", DemsFieldData.FIELD_MAPPINGS.AGENCY_FILE_NO.getLabel(),"/value");
                  String status = JsonParseUtils.getJsonElementValue(courtCaseJson, "status");
                  String rccStatus = JsonParseUtils.getJsonArrayElementValue(courtCaseJson, "/fields", "/name", DemsFieldData.FIELD_MAPPINGS.RCC_STATUS.getLabel(),"/value");

                  caseStatus.setId(caseId);
                  caseStatus.setKey(caseKey);
                  caseStatus.setName(caseName);
                  caseStatus.setCaseState(caseState);
                  caseStatus.setPrimaryAgencyFileId(primaryAgencyFileId);
                  caseStatus.setPrimaryAgencyFileNo(primaryAgencyFileNo);
                  caseStatus.setAgencyFileId(agencyFileId);
                  caseStatus.setAgencyFileNo(agencyFileNo);
                  caseStatus.setCourtFileId(courtFileUniqueId);
                  caseStatus.setCourtFile(courtFile);
                  caseStatus.setCourtFileNo(courtFileNo);
                  caseStatus.setStatus(status);
                  caseStatus.setRccStatus(rccStatus);

                  exchange.getMessage().setBody(caseStatus);
                }
              })
              .setProperty("caseStatusObj", body())

              .marshal().json(JsonLibrary.Jackson, DemsCaseStatus.class)
              .setProperty("caseStatus").simple("${bodyAs(String)}")
            .endChoice()
            .otherwise()
              .setBody(simple("${exchangeProperty.caseNotFound}"))
              .setHeader("CamelHttpResponseCode", simple("200"))
              .log(LoggingLevel.INFO,"Case not found.")
            .endChoice()
          .end() // choice end
        .endDoTry()
        .doCatch(Exception.class)
          .log(LoggingLevel.ERROR,"Exception: ${exception}")
          .log(LoggingLevel.INFO,"Exchange Context: ${exchange.context}")
          .setBody(simple("${exchangeProperty.caseNotFound}"))
          .setHeader("CamelHttpResponseCode", simple("200"))
        .end()
      .endChoice()
      .otherwise()
        .setBody(simple("${exchangeProperty.caseNotFound}"))
        .setHeader("CamelHttpResponseCode", simple("200"))
        .log(LoggingLevel.INFO,"Case not found.")
      .endChoice()
    .end()
    .log(LoggingLevel.INFO, "DEMS Case Status: ${body}")
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
    .log(LoggingLevel.INFO,"Processing request ${exchangeProperty.key}")
    .to("direct:getCourtCaseIdByKey")
    .setProperty("id", jsonpath("$.id"))
    .to("direct:getCourtCaseDataById")
    ;
  }

  private void getCourtCaseDataById() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: exchangeProperty.id
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"Processing request (id=${exchangeProperty.id})...")
    .doTry()

      .setProperty("maxRecordIncrements").simple("2")
      .setProperty("incrementCount").simple("0")
      .setProperty("continueLoop").simple("true")
      // limit the number of times incremented to 25.
      .loopDoWhile(simple("${exchangeProperty.continueLoop} == 'true' && ${exchangeProperty.id} != '' && ${exchangeProperty.id} != null && ${exchangeProperty.incrementCount} < ${exchangeProperty.maxRecordIncrements}"))

        .removeHeader("CamelHttpUri")
        .removeHeader("CamelHttpBaseUri")
        .removeHeaders("CamelHttp*")
        .removeHeader(Exchange.CONTENT_ENCODING) // In certain cases, the encoding was gzip, which DEMS does not support
        .setHeader(Exchange.HTTP_METHOD, simple("GET"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
        .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/cases/info/${exchangeProperty.id}")
        .log(LoggingLevel.DEBUG,"Retrieved court case data by id.")

        .setProperty("edtCaseStatus",jsonpath("$.status"))
        .log(LoggingLevel.INFO, "${exchangeProperty.id} Case Status: ${exchangeProperty.edtCaseStatus}")
        .choice()
          .when(simple("${exchangeProperty.edtCaseStatus} == 'Removed'"))
            .log(LoggingLevel.WARN, "The case is removed in EDT, clear-out the returned body.")
            .setBody(simple(""))
            .setProperty("continueLoop").simple("false")

            .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("404"))
            .setHeader("CCMException", simple("{\"error\": \"Record is deleted in EDT.\"}"))
            .stop()
          .endChoice()
          .when(simple("${exchangeProperty.edtCaseStatus} == 'Offline'"))
            .log(LoggingLevel.WARN, "The case is offline in EDT, clear-out the returned body.")
            .setBody(simple(""))
            .setProperty("continueLoop").simple("false")

            .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("500"))
            .setHeader("CCMException", simple("{\"error\": \"Record is offline in EDT.\"}"))
            .stop()
          .endChoice()
          .when(simple("${exchangeProperty.edtCaseStatus} != 'Active' && ${exchangeProperty.edtCaseStatus} != 'Inactive'"))
            .log(LoggingLevel.DEBUG,"${body}")
            .log(LoggingLevel.INFO, "Case ${exchangeProperty.id} not active yet, wait 10 seconds... iteration: ${exchangeProperty.incrementCount}")
            .delay(10000)
            .log(LoggingLevel.INFO, "Retry case data retrieval... ${exchangeProperty.id}")
          .endChoice()
          .otherwise()
            .setProperty("continueLoop").simple("false")
          .endChoice()
        .end()
        // increment the loop count.
        .process(new Processor() {
          @Override
          public void process(Exchange ex) {
            Integer incrementCount = (Integer)ex.getProperty("incrementCount", Integer.class);
            incrementCount++;
            ex.setProperty("incrementCount", incrementCount);
          }
        })

      .end() // end loop

      .choice()
        .when(simple("${exchangeProperty.edtCaseStatus} == 'Queued'"))
          .log(LoggingLevel.ERROR, "Court case... ${exchangeProperty.id} possibly stuck in queue.")
          .setBody(simple(""))
          .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("500"))
          .setHeader("CCMException", simple("{\"error\": \"Case id ${exchangeProperty.id} possibly stuck in queued state.\"}"))
          .stop()
        .endChoice()
      .end()

    .endDoTry()
    .doCatch(HttpOperationFailedException.class)
      // sometimes, if events come in a little too fast for the same case, it may cause an error
      // wait 25 seconds then try again.
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
          HttpOperationFailedException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);

          log.error("Returned status code : " + cause.getStatusCode());
          log.error("Response body : " + cause.getResponseBody());
        }
      })
      .delay(25000)
      .log(LoggingLevel.WARN,"Re-attempting to retrieve case data (id=${exchangeProperty.id})...")
      .removeHeader("CamelHttpUri")
      .removeHeader("CamelHttpBaseUri")
      .removeHeaders("CamelHttp*")
      .removeHeader(Exchange.CONTENT_ENCODING) // In certain cases, the encoding was gzip, which DEMS does not support
      .setHeader(Exchange.HTTP_METHOD, simple("GET"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
      .toD("https://{{dems.host}}/cases/${exchangeProperty.id}")
      .log(LoggingLevel.INFO,"Retrieved court case data by id.")
    .end()
    ;
  }

  private void updateCaseCourtFileList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: header = id
    from("platform-http:/" + routeId + "?httpMethodRestrict=POST")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"Searching for case list: ${body}")
    .split()
      .jsonpathWriteAsString("$.case_ids")
      .setProperty("caseId",simple("${body}"))
      .log(LoggingLevel.INFO,"Searching for case id: ${exchangeProperty.caseId}")
      .to("direct:updateCaseCourtFile")
    .end() // end loop
    ;
  }

  private void updateCaseCourtFile() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    // IN: exchangeProperty.caseId
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"******** Searching for case with: caseId = ${exchangeProperty.caseId} ...")
    .choice()
      .when(simple("${exchangeProperty.caseId} != null"))
        .log(LoggingLevel.INFO, "Look-up caseId.")
        .setProperty("id",simple("${exchangeProperty.caseId}"))

        .to("direct:getCourtCaseStatusById")
        .unmarshal().json(JsonLibrary.Jackson, DemsCaseStatus.class)

        .process(new Processor() {
          @Override
          public void process(Exchange exchange) {
            DemsCaseStatus demsCaseStatus = (DemsCaseStatus)exchange.getIn().getBody(DemsCaseStatus.class);
            exchange.setProperty("demsCaseId", demsCaseStatus.getId());
            exchange.setProperty("caseStatus", demsCaseStatus.getStatus());
            exchange.setProperty("courtFileNo", demsCaseStatus.getCourtFileNo());

            String courtFileNos = demsCaseStatus.getCourtFileNo();

            if(courtFileNos != null) {
              String[] courtFiles = courtFileNos.split("; ");
              List<String> uniqueCourtFiles = new ArrayList<String>();
              for (int i=0; i < courtFiles.length; i++) {
                System.out.println(courtFiles[i]);
                String[] courtFileSplit = courtFiles[i].split("-");
                String courtFile = courtFileSplit[0];
                if(!uniqueCourtFiles.contains(courtFile)) {
                  uniqueCourtFiles.add(courtFile);
                }
              }

              StringBuilder courtFileBuilder = new StringBuilder();
              for(String courtFile : uniqueCourtFiles) {
                if(courtFileBuilder.length() > 0) {
                    courtFileBuilder.append("; ");
                }
                courtFileBuilder.append(courtFile);
              }

              StringBuffer outputStringBuffer = new StringBuffer();

              outputStringBuffer.append("{\"id\": \"");
              outputStringBuffer.append(demsCaseStatus.getId());

              outputStringBuffer.append("\", \"name\": \"");
              outputStringBuffer.append(demsCaseStatus.getName());

              outputStringBuffer.append("\", \"key\": \"");
              outputStringBuffer.append(demsCaseStatus.getKey());

              outputStringBuffer.append("\", \"status\": \"");
              outputStringBuffer.append(demsCaseStatus.getStatus());

              outputStringBuffer.append("\", \"fields\": [");

              outputStringBuffer.append("{ \"name\": \"Court File\", \"value\": \"");
              // Extract court file no and set the court file attribute.
              outputStringBuffer.append(courtFileBuilder.toString());

              outputStringBuffer.append("\" }");
              outputStringBuffer.append("]}");

              exchange.getMessage().setBody(outputStringBuffer.toString());

            }
          }
        })

        .log(LoggingLevel.INFO,"Updating case: ${body}")
        .doTry()
          .choice()
            .when(simple("${exchangeProperty.demsCaseId} != '' && ${exchangeProperty.caseStatus} == 'Active' && ${exchangeProperty.courtFileNo} != ''"))
              // update case
              .removeHeader("CamelHttpUri")
              .removeHeader("CamelHttpBaseUri")
              .removeHeaders("CamelHttp*")
              .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
              .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
              .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
              .log(LoggingLevel.INFO,"Updating DEMS case (caseId = ${exchangeProperty.demsCaseId})... ${body}")
              .toD("https://{{dems.host}}/cases/${exchangeProperty.demsCaseId}")
              .log(LoggingLevel.INFO,"################################# DEMS case court file atribute updated.")

            .endChoice()
            .otherwise()
              .setBody(simple("${exchangeProperty.caseNotFound}"))
              .setHeader("CamelHttpResponseCode", simple("200"))
              .log(LoggingLevel.ERROR,"Case not found.")
            .endChoice()
          .end() // choice end
        .endDoTry()
        .doCatch(Exception.class)
          .log(LoggingLevel.ERROR,"Exception: ${exception}")
          .log(LoggingLevel.INFO,"Exchange Context: ${exchange.context}")
          .setBody(simple("${exchangeProperty.caseNotFound}"))
          .setHeader("CamelHttpResponseCode", simple("200"))
        .end()

      .endChoice()
      .otherwise()
        .log(LoggingLevel.ERROR, "No caseId provided.")
      .endChoice()
    .end()
    ;
  }

  private void updateExistingParticipantwithOTCV2() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    // IN: header = id
    from("platform-http:/" + routeId )
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"updateExistingParticipantwithOTC... pages: ${header.pageFrom} -> ${header.pageTo}")
    //.setProperty("pageFrom", header("pageFrom"))
    //.setProperty("pageTo", header("pageTo"))

    .setProperty("v2DemsHost", simple("{{dems.host}}"))
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        String v2DemsHost = (String)exchange.getProperty("v2DemsHost");
        v2DemsHost = v2DemsHost.replace("/v1", "/v2");
        exchange.setProperty("v2DemsHost", v2DemsHost);
      }
    })
    .log(LoggingLevel.DEBUG, "New URL: ${exchangeProperty.v2DemsHost}")

    .setProperty("pageSize").simple("5000")
    .setProperty("maxRecordIncrements").simple("250")
    .setProperty("incrementCount").simple("1")
    .setProperty("continueLoop").simple("true")

    .choice()
      .when(simple("${header.pageFrom} != null && ${header.pageFrom} != ''"))
        .setProperty("incrementCount").simple("${header.pageFrom}")
      .endChoice()
    .end()

    .choice()
      .when(simple("${header.pageTo} != null && ${header.pageTo} != ''"))
        .setProperty("maxRecordIncrements").simple("${header.pageTo}")
      .endChoice()
    .end()

    // limit the number of times incremented to 250.
    .loopDoWhile(simple("${exchangeProperty.continueLoop} == 'true' && ${exchangeProperty.incrementCount} <= ${exchangeProperty.maxRecordIncrements}"))
      .log(LoggingLevel.INFO, "\n\nViewing page: ${exchangeProperty.incrementCount}")

      .removeHeader("CamelHttpUri")
      .removeHeader("CamelHttpBaseUri")
      .removeHeaders("CamelHttp*")
      .setHeader(Exchange.HTTP_METHOD, simple("GET"))
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
      //traverse through all persons in DEMS
      .toD("https://${exchangeProperty.v2DemsHost}/org-units/{{dems.org-unit.id}}/persons?filter=category:Other&page=${exchangeProperty.incrementCount}&pagesize=${exchangeProperty.pageSize}&sort=id")
      //.log(LoggingLevel.DEBUG,"Person list: '${body}'")
      .setProperty("length",jsonpath("$.items.length()"))
      .log(LoggingLevel.INFO,"Person count: ${exchangeProperty.length}")

      .choice()
        .when(simple("${exchangeProperty.length} < 1"))
          .log(LoggingLevel.INFO, "End of pages.")
          .setProperty("continueLoop").simple("false")
        .endChoice()
        .when(simple("${header.CamelHttpResponseCode} == 200"))
          .to("direct:processParticipantsList")
        .endChoice()
      .end()

      .log(LoggingLevel.INFO, "\n\nEnd of page: ${exchangeProperty.incrementCount}")

      // increment the loop count.
      .process(new Processor() {
        @Override
        public void process(Exchange ex) {
          Integer incrementCount = (Integer)ex.getProperty("incrementCount", Integer.class);
          incrementCount++;
          ex.setProperty("incrementCount", incrementCount);
        }
      })
    .end() // end loop
    .log(LoggingLevel.INFO,"end of updateExistingParticipantwithOTCV2.")
    ;
  }

  private void processParticipantsList() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    //.log(LoggingLevel.DEBUG,"Person list: '${body}'")
    .setProperty("length",jsonpath("$.items.length()"))
    .log(LoggingLevel.INFO,"Person count: ${exchangeProperty.length}")
    .split()
      .jsonpathWriteAsString("$.items.*").stopOnException()
      .setProperty("personId",jsonpath("$.id"))
      .setProperty("personKey",jsonpath("$.key"))
      .setProperty("status",jsonpath("$.status"))
      .setProperty("lastname",jsonpath("$.lastName"))
      .setProperty("existingOtc",jsonpath("$.fields[?(@.name == '12')]"))
      .choice()
        .when().simple("${exchangeProperty.status} == 'Active' && ${exchangeProperty.personKey} != null && ${exchangeProperty.lastname} != '' && ${exchangeProperty.existingOtc} == '[]'")
          .to("direct:updateOtcParticipants")
        .endChoice()
        .otherwise()
          .log(LoggingLevel.INFO,"Person Id: ${exchangeProperty.personId}, status: ${exchangeProperty.status}, OTC: ${exchangeProperty.existingOtc}")
        .endChoice()
      .end()
      .log(LoggingLevel.DEBUG, "End of loop for person.")
    .end() // end loop
    .log(LoggingLevel.INFO,"end of processParticipantsList.")
    ;
  }

  private void updateOtcParticipants() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
    from("direct:" + routeId)
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html

    //look-up list of accused participants of each case
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
    //traverse through all cases in DEMS
    .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/persons/${exchangeProperty.personId}")
    //.log(LoggingLevel.DEBUG,"Person in system: '${body}'")
    .setProperty("otcfieldexist").simple("false")
    .setProperty("demspersondata", simple("${body}"))
    .setProperty("caselength",jsonpath("$.cases.length()"))
    .setProperty("existingOtc",jsonpath("$.fields[?(@.name == 'OTC')]"))
    .unmarshal().json()
    .log(LoggingLevel.DEBUG, "Participant ${exchangeProperty.personId} case length: ${exchangeProperty.caselength} existingOtc: ${exchangeProperty.existingOtc}")
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        Object d =(Object)exchange.getIn().getBody();
        exchange.getMessage().setBody(d);
        LinkedHashMap<String, Object> dataMap = (LinkedHashMap<String, Object>) d;

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(dataMap);

        Boolean present =false;
        ObjectMapper personDataMapper = new ObjectMapper();
        personDataMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        DemsPersonData personData = personDataMapper.readValue(json, DemsPersonData.class);
        for(DemsFieldData fieldData : personData.getFields()) {
          //log.info("Field Name: " + fieldData.getName());
          //log.info("Field Value: " + fieldData.getValue());
          if(fieldData.getName().equalsIgnoreCase("OTC")) {
            present = true;
            exchange.setProperty("otcfieldexist", "true");
            break;
          }
        }
        if(!present) {
          log.info("Generating OTC for person id: "+personData.getId() + " generated: " +personData.generateOTC());
        }

        exchange.getMessage().setBody(personData, DemsPersonData.class);
      }
    })
    .marshal().json(JsonLibrary.Jackson, DemsPersonData.class)
    .setProperty("update_data", simple("${body}"))
    .log(LoggingLevel.DEBUG, "Check otc exist: ${exchangeProperty.otcfieldexist}")
    .choice()
      .when(simple("${exchangeProperty.otcfieldexist} == 'false'"))
        .log(LoggingLevel.DEBUG,"DEMS-bound person data: '${body}'")
        // update case
        .setBody(simple("${exchangeProperty.update_data}"))
        .log(LoggingLevel.DEBUG,"DEMS-bound person data: '${body}'")
        .setHeader("key", jsonpath("$.key"))
        .setHeader("id", jsonpath("$.id"))
        .log(LoggingLevel.INFO,"DEMS-bound person id: '${header[id]}' key: '${header[key]}' case count: '${exchangeProperty.caselength}'")
        .setHeader("key").simple("${header.key}")
        .setHeader("id").simple("${header.id}")
        .removeHeader("CamelHttpUri")
        .removeHeader("CamelHttpBaseUri")
        .removeHeaders("CamelHttp*")
        .setHeader(Exchange.HTTP_METHOD, simple("PUT"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .setHeader("Authorization").simple("Bearer " + "{{dems.token}}")
        .toD("https://{{dems.host}}/org-units/{{dems.org-unit.id}}/persons/${header[id]}")
        .log(LoggingLevel.INFO,"Person updated.")
      .endChoice()
      .otherwise()
        .log(LoggingLevel.INFO,"OTC data already exists, skip updating person id: ${exchangeProperty.personId}.")
      .endChoice()
    .end()
    ;
  }

}