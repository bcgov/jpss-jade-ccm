package ccm;

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

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import ccm.models.system.justin.*;
import ccm.models.common.data.CaseHyperlinkData;
import ccm.models.common.versioning.Version;

public class CcmJustinInAdapter extends RouteBuilder {
  @Override
  public void configure() throws Exception {
    version();
    getCaseHyperlink();
    getCaseHyperlinkNew();
  }

  private void version() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    String path = "justin/api/v1/version";

    from("platform-http:/" + path + "?httpMethodRestrict=GET")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        exchange.getMessage().setBody(Version.V1_0.toString());
      }
    })
    ;
  }

  private void getCaseHyperlink() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    String path = "justin/api/v1/" + routeId;

    // IN: header = rcc_id
    from("platform-http:/" + path + "?httpMethodRestrict=GET")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html

    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        exchange.setProperty("exchangeId",exchange.getExchangeId());
      }
    })
    .log(LoggingLevel.INFO, "Received request (exchange id: ${exchangeProperty.exchangeId}) for case hyperlink. RCC_ID: ${header.rcc_id}")

/*     .choice()
      .when(simple("${header.authorization} != 'Bearer {{justin.in.token}}'"))
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(401))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            JustinCaseHyperlinkData body = new JustinCaseHyperlinkData();
            body.setMessage("Unauthorized.");
            exchange.getMessage().setBody(body);
          }
        })
        .marshal().json(JsonLibrary.Jackson, JustinCaseHyperlinkData.class)
        .log(LoggingLevel.DEBUG,"HTTP response 401. Body: ${body}")
        .stop()
      .end() */

    .setProperty("rcc_id", simple("${header.rcc_id}"))

    .choice()
      .when(PredicateBuilder.or(
          header("rcc_id").isNull(),
          header("rcc_id").isEqualTo("")))
        .log(LoggingLevel.INFO, "RCC_ID is empty.")
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))

        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            JustinCaseHyperlinkData body = new JustinCaseHyperlinkData();
            body.setMessage("Required parameter (RCC_ID) is missing.");
            exchange.getMessage().setBody(body);
          }
        })
        .marshal().json(JsonLibrary.Jackson, JustinCaseHyperlinkData.class)
        .stop()
        .endChoice()
    .end()

    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))

    // attempt to retrieve case id using getCourtCaseExists lookup endpoint.
    .setHeader("number", simple("${exchangeProperty.rcc_id}"))
    .to("http://ccm-lookup-service/getCourtCaseExists")
    .unmarshal().json()
    .setProperty("caseId").simple("${body[id]}")
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .choice()
      .when(simple("${exchangeProperty.caseId} != ''"))
        .setProperty("hyperlinkPrefix", simple("{{dems.case.hyperlink.prefix}}"))
        .setProperty("hyperlinkSuffix", simple("{{dems.case.hyperlink.suffix}}"))
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            String prefix = exchange.getProperty("hyperlinkPrefix", String.class);
            String suffix = exchange.getProperty("hyperlinkSuffix", String.class);
            String caseId = exchange.getProperty("caseId", String.class);
            JustinCaseHyperlinkData body = new JustinCaseHyperlinkData();

            body.setMessage("Case found.");
            body.setHyperlink(prefix + caseId + suffix);
            exchange.getMessage().setBody(body);
          }
        })
        .log(LoggingLevel.INFO, "Case (RCC_ID: ${exchangeProperty.rcc_id}) found; caseId: '${exchangeProperty.caseId}'")
        .endChoice()
      .otherwise()
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            JustinCaseHyperlinkData body = new JustinCaseHyperlinkData();
            body.setMessage("Case not found.");
            exchange.getMessage().setBody(body);
          }
        })
        .log(LoggingLevel.INFO, "Case not found.")
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
        .endChoice()
    .end()
    .marshal().json(JsonLibrary.Jackson, JustinCaseHyperlinkData.class)
    ;
  }

  private void getCaseHyperlinkNew() {
    // use method name as route id
    String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

    String path = "justin/api/v1/" + routeId;

    // IN: header = rcc_id
    from("platform-http:/" + path + "?httpMethodRestrict=GET")
    .routeId(routeId)
    .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html

    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        exchange.setProperty("exchangeId",exchange.getExchangeId());
      }
    })
    .log(LoggingLevel.INFO, "Received request (exchange id: ${exchangeProperty.exchangeId}) for case hyperlink. RCC_ID: ${header.rcc_id} ...")

    // check for credentials
    .choice()
      .when(simple("${header.authorization} != 'Bearer {{justin.in.token}}'"))
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(401))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            JustinCaseHyperlinkData body = new JustinCaseHyperlinkData();
            body.setMessage("Unauthorized.");
            exchange.getMessage().setBody(body);
          }
        })
        .marshal().json(JsonLibrary.Jackson, JustinCaseHyperlinkData.class)
        .log(LoggingLevel.ERROR,"HTTP response 401. Body: ${body}")
        .stop()
    .end()

    .setProperty("rcc_id", simple("${header.rcc_id}"))

    // check for required parameters
    .choice()
      .when(PredicateBuilder.or(
          header("rcc_id").isNull(),
          header("rcc_id").isEqualTo("")))
        .log(LoggingLevel.INFO, "RCC_ID is empty.")
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))

        .process(new Processor() {
          @Override
          public void process(Exchange exchange) throws Exception {
            JustinCaseHyperlinkData body = new JustinCaseHyperlinkData();
            exchange.setProperty("errorMessage", "Required parameter (RCC_ID) is missing.");
            body.setMessage(exchange.getProperty("errorMessage", String.class));
            exchange.getMessage().setBody(body);
          }
        })
        .marshal().json(JsonLibrary.Jackson, JustinCaseHyperlinkData.class)
        .log(LoggingLevel.ERROR,"HTTP response 400. Message: ${exchangeProperty.errorMessage}")
        .log(LoggingLevel.DEBUG,"Body: ${body}")
        .stop()
        .endChoice()
    .end()

    // attempt to retrieve case id using getCaseHyperlink lookup endpoint.
    .doTry()
      .removeHeader("CamelHttpUri")
      .removeHeader("CamelHttpBaseUri")
      .removeHeaders("CamelHttp*")
      .setHeader(Exchange.HTTP_METHOD, simple("GET"))
      .setHeader("key", simple("${exchangeProperty.rcc_id}"))
      .to("http://ccm-lookup-service/getCaseHyperlink")
      .endDoTry()
    .doCatch(HttpOperationFailedException.class)
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
          JustinCaseHyperlinkData body = new JustinCaseHyperlinkData();
          HttpOperationFailedException exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);

          if (exception.getStatusCode() == 404) {
            exchange.setProperty("errorMessage", "Case not found.");
          } else {
            exchange.setProperty("errorMessage", "Error retrieving case hyperlink.");
          }
          body.setMessage(exchange.getProperty("errorMessage", String.class));

          exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, exception.getStatusCode());
          exchange.getMessage().setBody(body);
        }
      })
      .marshal().json(JsonLibrary.Jackson, JustinCaseHyperlinkData.class)
      .log(LoggingLevel.ERROR, "HTTP response ${header.CamelHttpResponseCode}. Error message: ${exchangeProperty.errorMessage}")
      .log(LoggingLevel.DEBUG, "Body: ${body}.")
      .stop()
    .end()

    // prepare response
    .unmarshal().json(JsonLibrary.Jackson, CaseHyperlinkData.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        CaseHyperlinkData data = exchange.getMessage().getBody(CaseHyperlinkData.class);
        JustinCaseHyperlinkData body = new JustinCaseHyperlinkData(data);
        exchange.getMessage().setBody(body);
      }
    })
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .marshal().json(JsonLibrary.Jackson, JustinCaseHyperlinkData.class)
    .log(LoggingLevel.INFO, "Case (RCC_ID: ${exchangeProperty.rcc_id}) found.")
    .log(LoggingLevel.DEBUG, "Body: ${body}")
    ;
  }

}