package ccm.dems.useraccess.adapter;

import javax.enterprise.context.ApplicationScoped;

import ccm.dems.useraccess.adapter.models.event.CaseUserEvent;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class UserAccessAdded extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        processCaseUserAccessAdded();
        processCourtCaseAuthListChanged();
        processCourtCaseAuthListUpdated();
    }

    private void processCaseUserAccessAdded() {
        // use method name as route id
        String routeId = new Object() {
        }.getClass().getEnclosingMethod().getName();

        // IN
        // property: event_object
        from("direct:" + routeId).routeId(routeId).streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
                .log(LoggingLevel.DEBUG, "event_key = ${header[event_key]}").process(new Processor() {

                    @Override
                    public void process(Exchange exchange) {
                        CaseUserEvent event = (CaseUserEvent) exchange.getProperty("event_object");
                        exchange.getMessage().setHeader("event_key", event.getJustin_rcc_id());
                    }
                })
                .log(LoggingLevel.DEBUG,
                        "Calling route processCourtCaseAuthListChanged( rcc_id = ${header[event_key]} ) ...")
                .to("direct:processCourtCaseAuthListChanged")
                .log(LoggingLevel.DEBUG, "Returned from processCourtCaseAuthListChanged().");
    }

    private void processCourtCaseAuthListChanged() {
        // use method name as route id
        String routeId = new Object() {
        }.getClass().getEnclosingMethod().getName();

        from("direct:" + routeId).routeId(routeId).streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
                .log(LoggingLevel.DEBUG, "event_key = ${header[event_key]}")
                .setHeader("number", simple("${header[event_key]}")).to("http://ccm-lookup-service/getCourtCaseExists")
                .unmarshal().json().setProperty("caseFound").simple("${body[id]}").setProperty("autoCreateFlag")
                .simple("{{dems.case.auto.creation}}").choice().when(simple("${exchangeProperty.caseFound} != ''"))
                .to("direct:processCourtCaseAuthListUpdated").end();
    }

    private void processCourtCaseAuthListUpdated() {
        // use method name as route id
        String routeId = new Object() {
        }.getClass().getEnclosingMethod().getName();

        from("direct:" + routeId).routeId(routeId).streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
                .log(LoggingLevel.DEBUG, "event_key = ${header[event_key]}")
                .setHeader(Exchange.HTTP_METHOD, simple("GET"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json")).setHeader("number")
                .simple("${header.event_key}").log(LoggingLevel.DEBUG, "Retrieve court case auth list")
                .to("http://ccm-lookup-service/getCourtCaseAuthList")
                .log(LoggingLevel.DEBUG, "Update court case auth list in DEMS.  Court case auth list = ${body}")
                // JADE-1489 work around #1 -- not sure why body doesn't make it into dems-adapter
                .setHeader("temp-body", simple("${body}")).to("http://ccm-dems-adapter/syncCaseUserList");
    }

}
