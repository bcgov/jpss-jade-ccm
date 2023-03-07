package mock;

import org.apache.camel.CamelException;

// To run this integration use:
// kamel run MockDemsApp.java --property file:MockDemsApp.properties --profile openshift
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

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;

import ccm.models.common.data.CourtCaseData;
import ccm.models.common.event.BaseEvent;
import ccm.models.common.event.EventKPI;
import ccm.models.common.data.AuthUser;
import ccm.models.common.data.AuthUserList;
import ccm.models.common.data.CaseAccused;
import ccm.models.common.data.CaseAppearanceSummaryList;
import ccm.models.common.data.CaseCrownAssignmentList;
import ccm.models.common.data.ChargeAssessmentData;
import ccm.models.common.data.ChargeAssessmentDataRefList;
import ccm.models.system.dems.*;
import ccm.utils.DateTimeUtils;
import ccm.utils.JsonParseUtils;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MockDemsApp extends RouteBuilder {

  @Override
  public void configure() throws Exception {
    version();
  }

  private void version() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    from("platform-http:/v1/version?httpMethodRestrict=GET")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .log(LoggingLevel.INFO,"version query request received")
    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setBody().simple("{ \"version\": \"0.1\" }")
    .log(LoggingLevel.DEBUG,"Response: ${body}")
    ;
  }
}