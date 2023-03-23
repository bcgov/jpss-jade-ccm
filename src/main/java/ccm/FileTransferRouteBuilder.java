// camel-k: dependency=camel:servlet
// camel-k: dependency=camel:rest
// camel-k: dependency=camel:jackson
// camel-k: dependency=camel:http
// camel-k: dependency=camel:base64
// camel-k: property=quarkus.http.port=8080

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.Base64DataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.support.builder.ValueBuilder;

import java.nio.charset.StandardCharsets;

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
            .setHeader(Exchange.CONTENT_TYPE, constant("multipart/form-data"))
            .process(exchange -> {
                String fileName = exchange.getMessage().getHeader("CamelFileName", String.class);
                String boundary = "simpleboundary";
                byte[] fileContent = exchange.getMessage().getBody(byte[].class);
                String multipartHeader = "--" + boundary + "\r\n" + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" + "Content-Type: application/octet-stream\r\n" + "\r\n";
                String multipartFooter = "\r\n" + "--" + boundary + "--";

                byte[] headerBytes = multipartHeader.getBytes(StandardCharsets.UTF_8);
                byte[] footerBytes = multipartFooter.getBytes(StandardCharsets.UTF_8);

                byte[] multipartBody = new byte[headerBytes.length + fileContent.length + footerBytes.length];

                System.arraycopy(headerBytes, 0, multipartBody, 0, headerBytes.length);
                System.arraycopy(fileContent, 0, multipartBody, headerBytes.length, fileContent.length);
                System.arraycopy(footerBytes, 0, multipartBody, headerBytes.length + fileContent.length, footerBytes.length);

                exchange.getMessage().setHeader("Content-Disposition", new ValueBuilder(simple("form-data; name=\"file\"; filename=\"${header.CamelFileName}\"")));
                exchange.getMessage().setHeader("CamelHttpMethod", constant("PUT"));
                exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, constant("multipart/form-data;boundary=" + boundary));
                exchange.getMessage().setBody(multipartBody);
            })
            .to("http://remote-endpoint-url");
    }
}

