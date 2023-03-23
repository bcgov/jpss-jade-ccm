// camel-k: dependency=camel:servlet
// camel-k: dependency=camel:rest
// camel-k: dependency=camel:jackson
// camel-k: dependency=camel:http
// camel-k: dependency=camel:base64
// camel-k: dependency=camel:attachments
// camel-k: property=quarkus.http.port=8080

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.Base64DataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.attachment.AttachmentMessage;

import javax.activation.DataHandler;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

public class FileTransferRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        Base64DataFormat base64 = new Base64DataFormat();

        restConfiguration()
            .component("servlet")
            .contextPath("/api")
            .bindingMode(RestBindingMode.auto);

        rest("/upload")
            .post()
            .consumes("application/json")
            .route()
            .unmarshal().json(JsonLibrary.Jackson)
            .process(exchange -> {
                String fileName = exchange.getMessage().getBody(Map.class).get("fileName").toString();
                String fileContentBase64 = exchange.getMessage().getBody(Map.class).get("fileContent").toString();
                exchange.getMessage().setHeader("CamelFileName", fileName);
                exchange.getMessage().setBody(fileContentBase64);
            })
            .unmarshal(base64)
            .process(exchange -> {
                String fileName = exchange.getMessage().getHeader("CamelFileName", String.class);
                byte[] fileContent = exchange.getMessage().getBody(byte[].class);
                InputStream inputStream = new ByteArrayInputStream(fileContent);
                DataHandler dataHandler = new DataHandler(inputStream, "application/octet-stream");
                AttachmentMessage inMessage = exchange.getMessage(AttachmentMessage.class);
                inMessage.addAttachment(fileName, dataHandler);
            })
            .setHeader("CamelHttpMethod", constant("PUT"))
            .to("http://remote-endpoint-url");
    }
}

