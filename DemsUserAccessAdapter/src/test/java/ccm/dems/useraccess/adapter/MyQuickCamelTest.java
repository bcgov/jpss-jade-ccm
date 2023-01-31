package ccm.dems.useraccess.adapter;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

public class MyQuickCamelTest extends CamelTestSupport {

    @Test
    public void testMock() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:output");

        template.sendBody("direct:start", "Hello World");
        var route = createRouteBuilder();
        assertMockEndpointsSatisfied();

    }

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start").to("mock:result");
            }
        };
    }

}
