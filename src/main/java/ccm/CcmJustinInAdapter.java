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
import ccm.models.common.data.CaseHyperlinkDataList;
import ccm.models.common.data.CommonCaseList;
import ccm.models.common.versioning.Version;

public class CcmJustinInAdapter extends RouteBuilder {
  @Override
  public void configure() throws Exception {
    version();
    getCaseHyperlink();
    //as part of jade 2425
    getCaseListHyperlink();
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

  //as part of jade 2425
  private void getCaseListHyperlink() {
   // use method name as route id
   String routeId = new Object() {}.getClass().getEnclosingMethod().getName();
   String path = "justin/api/v1/" + routeId;

   // IN: header = rcc_id
   from("platform-http:/" + path + "?httpMethodRestrict=POST")
   .routeId(routeId)
   .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
   .log(LoggingLevel.INFO,"getCaseListHyperlink request received.")
   .log(LoggingLevel.DEBUG,"${body}")
   .process(new Processor() {
     @Override
     public void process(Exchange exchange) throws Exception {
       exchange.setProperty("exchangeId",exchange.getExchangeId());
     }
   })
   .log(LoggingLevel.INFO, "Received request (exchange id: ${exchangeProperty.exchangeId}) for case list hyperlink.")
   // check for credentials
   .choice()
     .when(simple("${header.authorization} != 'Bearer {{justin.in.token}}'"))
       .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(401))
       .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
       .process(new Processor() {
         @Override
         public void process(Exchange exchange) throws Exception {
          JustinRccCaseList body = new JustinRccCaseList();
           exchange.getMessage().setBody(body);
         }
       })
       .log(LoggingLevel.DEBUG,"${body}")
       .marshal().json(JsonLibrary.Jackson, JustinRccCaseList.class)
       .log(LoggingLevel.ERROR,"HTTP response 401. Body: ${body}")
       .stop()
   .end()
   .setProperty("keys", simple("${body}"))
   .log(LoggingLevel.DEBUG,"Keys : ${exchangeProperty.keys}")
   .unmarshal().json(JsonLibrary.Jackson, JustinRccCaseList.class)
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        JustinRccCaseList data = exchange.getMessage().getBody(JustinRccCaseList.class);
        CommonCaseList body = new CommonCaseList(data);
        exchange.getMessage().setBody(body);
      }
    })
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .marshal().json(JsonLibrary.Jackson, CommonCaseList.class)
    .log(LoggingLevel.DEBUG, "Body : ${body}")
    //call to lookup service
    .doTry()
    .removeHeader("CamelHttpUri")
    .removeHeader("CamelHttpBaseUri")
    .removeHeaders("CamelHttp*")
    .setHeader(Exchange.HTTP_METHOD, simple("GET"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .setHeader("key", simple("${body}"))
    .to("http://ccm-lookup-service/getCaseListHyperlink")
    .endDoTry()
  .doCatch(HttpOperationFailedException.class)
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .process(new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        JustinCaseHyperlinkDataList body = new JustinCaseHyperlinkDataList();
        CaseHyperlinkDataList c = new CaseHyperlinkDataList();
        HttpOperationFailedException exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);
        
        if (exception.getStatusCode() == 404) {
          exchange.setProperty("errorMessage", "Case not found.");
        } else {
          exchange.setProperty("errorMessage", "Error retrieving case hyperlink.");
        }
        body.setMessage(exchange.getProperty("errorMessage", String.class));

        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, exception.getStatusCode());
        exchange.getMessage().setBody(c);
      }
    })
    .marshal().json(JsonLibrary.Jackson, JustinCaseHyperlinkDataList.class)
    .log(LoggingLevel.ERROR, "HTTP response ${header.CamelHttpResponseCode}. Error message: ${exchangeProperty.errorMessage}")
    .log(LoggingLevel.DEBUG, "Body: ${body}.")
    .stop()
  .end()
 // prepare response
 .log(LoggingLevel.DEBUG, "Body got from dems: ${body}.")
 .unmarshal().json(JsonLibrary.Jackson, CaseHyperlinkDataList.class)
 .process(new Processor() {
   @Override
   public void process(Exchange exchange) throws Exception {
     CaseHyperlinkDataList data = exchange.getMessage().getBody(CaseHyperlinkDataList.class);
     //JustinCaseHyperlinkDataList body = exchange.getMessage().getBody(JustinCaseHyperlinkDataList.class);
     JustinCaseHyperlinkDataList jchd = new JustinCaseHyperlinkDataList(data);
     jchd.setMessage("");
     exchange.getMessage().setBody(jchd,JustinCaseHyperlinkDataList.class);
   }
 })
 .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
 .marshal().json(JsonLibrary.Jackson, JustinCaseHyperlinkDataList.class)
 .log(LoggingLevel.DEBUG, "Body: ${body}")
  ;
  }
}