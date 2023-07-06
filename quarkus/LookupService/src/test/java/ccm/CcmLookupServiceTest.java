package ccm;

import java.io.InputStream;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import ccm.models.common.event.BaseEvent;
import ccm.models.common.event.EventKPI;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CcmLookupServiceTest extends CamelQuarkusTestSupport {

	@Inject
	CamelContext context;

	
	@Produce("direct:publishEventKPI")
	ProducerTemplate producer;

	Exchange mockExchange; 

	MockEndpoint mockKafka;

	

	@BeforeAll
	public void setup() throws Exception {
		mockKafka = context.getEndpoint("mock:kafkaIntegration", MockEndpoint.class);
		
		mockExchange = new DefaultExchange(context);
	}

	
	@Override
	protected RoutesBuilder createRouteBuilder() {
		return new CcmLookupService(); 
		
	}

	@Test
	// not sure what we can test here, lots of external connections
	public void testPublishEventKPI() throws Exception {
		
		/**
		 * @author mcostell
		 * weave into the camelcontext and replace this endpoint id with our mock kafka endpoint 
		 */
		/*AdviceWith.adviceWith(context, "publishEventKPI", a -> {

			a.weaveById("kafka-send-to-event-kpi").replace().to(mockKafka);
		});*/
		/**
		 * @author mcostell
		 * 
		 * stub a BaseEvent and add the following headers before putting the 
		 * kpi_event_object
		 * kpi_status
		 * 
		 */
		
		BaseEvent stubbedBaseEvent = new BaseEvent(); 
		stubbedBaseEvent.setEvent_key("1");
		stubbedBaseEvent.setEvent_source("test");
		stubbedBaseEvent.setEvent_status("test");

		
		
		
		context.createFluentProducerTemplate().withHeader("header[number]", 12).to("direct:getCourtCaseAuthList").send();

		
		/*context.createFluentProducerTemplate().withProcessor(
			e->{
				e.setProperty("kpi_event_object",stubbedBaseEvent);
				e.setProperty("kpi_status", "EVENT_CREATED");
				e.setProperty("kpi_event_topic_name", "ccm-dems-user-access");
				e.setProperty("kpi_event_topic_offset", "0");
				e.setProperty("kpi_component_route_name", "test");
			})
		.to("direct:publishEventKPI").send();*/
		/**
		 * @author mcostell
		 * produce a message to the route with the afforementioned headers 
		 */
	
		//producer.sendBodyAndHeaders("direct:publishEventKPI","test", headers);
		/**
		 * assert that we should receive exactly 1 message 
		 */
		//mockKafka.expectedMessageCount(1);
		
		//mockKafka.assertIsSatisfied();
		
		/**
		 * let's ensure the object we serialized before sending to the mockendpoint is actually an EventKPI obj
		 */
		ObjectMapper jsonMapper = new ObjectMapper();
		EventKPI eventKPI=null; 
		
		for (Exchange e:mockKafka.getExchanges()) {
				
			eventKPI = jsonMapper.readValue((InputStream)e.getIn().getBody(), EventKPI.class);
		}
		//assert that objectmapper was able to create an EventKPI object our of the serialized json
		Assertions.assertTrue(eventKPI != null);
		//lets introspect the eventKPI instance to ensure we received a value we expected 
		Assertions.assertTrue(eventKPI.getKpi_status().equals("EVENT_CREATED"));
		
	}
	

}
