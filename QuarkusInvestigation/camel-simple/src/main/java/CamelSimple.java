import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped

public class CamelSimple extends RouteBuilder {


    static Logger mainLog = LoggerFactory.getLogger(CamelSimple.class);

    @Override
    public void configure() throws Exception {
       publishEventKPI();
       getCourtCaseDataById();
        publishTimer();
        mainLog.debug("{{dems.token}}");
        System.setProperty("quarkus.log.console.level", "FINE");
        System.out.println("dems token : {{dems.token}}");

    }

    private  void publishTimer() {
        from("timer://simpleTimer?period={{timer.period}}").process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                mainLog.info("Processing ...");
                mainLog.trace("Processing ...");

            }

        }).setBody().simple("hello timer").to("direct:publishEventKPI").end();
               // .setBody().constant("{{dems.token}}")
               // .log(LoggingLevel.INFO,"DEMS token : {{dems.token}}")
               // .to("stream:out")
               // .to("direct:publishEventKPI");
        // .to("stream:file?/deployments/quarkus.log");
        //.to("{{dems.host.url}}").end();
    }

    private void getCourtCaseDataById() {
        // use method name as route id
        String routeId = new Object() {
        }.getClass().getEnclosingMethod().getName();

        // IN: exchangeProperty.id
        from("direct:" + routeId).routeId(routeId).streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
                .log(LoggingLevel.DEBUG, "Processing request (id=${exchangeProperty.id})...")
                .removeHeader("CamelHttpUri").removeHeader("CamelHttpBaseUri").removeHeaders("CamelHttp*")
                .setHeader(Exchange.HTTP_METHOD, simple("GET"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json")).setHeader("Authorization")
                .simple("Bearer " + "{{dems.token}}").toD("https://{{dems.host}}/cases/${exchangeProperty.id}")
                .log(LoggingLevel.DEBUG, "Retrieved court case data by id.");
    }
    private void publishEventKPI() {
        // use method name as route id
        String routeId = new Object() {}.getClass().getEnclosingMethod().getName();

        //IN: property = kpi_event_object
        //IN: property = kpi_event_topic_name
        //IN: property = kpi_event_topic_offset
        //IN: property = kpi_status
        //IN: property = kpi_component_route_name
        from("direct:" + routeId)
                .routeId(routeId)
                .streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        //String kpi_status = (String) exchange.getProperty("kpi_status");
                        // KPI
                        String kpi_status =  EventKPI.STATUS.EVENT_PROCESSING_STARTED.name();
                        EventKPI kpi = new EventKPI(null, kpi_status);
                        kpi.setKpi_status(EventKPI.STATUS.EVENT_CREATED.name());
                        kpi.setEvent_topic_name((String)exchange.getProperty("kpi_event_topic_name"));
                        kpi.setEvent_topic_offset(exchange.getProperty("kpi_event_topic_offset"));
                        kpi.setIntegration_component_name("camel-Simple");
                        kpi.setComponent_route_name((String)exchange.getProperty("kpi_component_route_name"));
                        exchange.getMessage().setBody(kpi);
                    }
                })
                .marshal().json(JsonLibrary.Jackson, EventKPI.class)
                .log(LoggingLevel.DEBUG,"Event kpi: ${body}")
                .to("kafka:{{kafka.topic.kpis.name}}");
    }


}
