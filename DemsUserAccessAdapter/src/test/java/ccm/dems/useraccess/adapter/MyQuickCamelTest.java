package ccm.dems.useraccess.adapter;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

//@QuarkusTest
public class MyQuickCamelTest extends CamelTestSupport {

    @Test
    public void testMock() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:output");

        template.sendBody("direct:start", "Hello World");
        var route = createRouteBuilder();
        assertMockEndpointsSatisfied();

    }

    @Test
    public void TestSyncUser() throws Exception {
        // AdviceWith.adviceWith(context, "start", a -> a.mockEndpoints());
        // MockEndpoint mock = getMockEndpoint("mock:direct://syncCaseUserList");
        // template.sendBody("direct://syncCaseUserList", new AuthUserList());

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
