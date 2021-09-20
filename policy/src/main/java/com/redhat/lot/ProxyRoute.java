package com.redhat.lot;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.ValidationReport;
import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.v2.beans.ArtifactMetaData;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import javax.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.netty.http.NettyHttpMessage;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link RouteBuilder} demonstrating the use of CDI (Contexts and Dependency Injection).
 * <p>
 * Note that for the {@code @Inject} and {@code @ConfigProperty} annotations to work, this class has to be annotated
 * with {@code @ApplicationScoped}.
 */
@ApplicationScoped
public class ProxyRoute extends RouteBuilder {

    /**
     * {@code timer.period} is defined in {@code src/main/resources/application.properties}
     */
    @ConfigProperty(name = "timer.period", defaultValue = "1000")
    String period;

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyRoute.class);

    @Override
    public void configure() throws Exception {

        from("netty-http:proxy://0.0.0.0:8000")
            .process(ProxyRoute::uppercase)
            .toD("netty-http:" 
                + "${headers." + Exchange.HTTP_SCHEME + "}://" 
                + "${headers." + Exchange.HTTP_HOST + "}:" 
                + "${headers." + Exchange.HTTP_PORT + "}" 
                + "${headers." + Exchange.HTTP_PATH + "}");

    }

    public static void uppercase(final Exchange exchange) {
        final OpenApiInteractionValidator validator = OpenApiInteractionValidator.createForSpecificationUrl(
                "http://registry.demo.router-default.apps.cluster-8d57.8d57.sandbox1409.opentlc.com/api/artifacts/person")
                // .withBasePathOverride("/")
                .build();

        final NettyHttpMessage _message = exchange.getIn(NettyHttpMessage.class);
        final FullHttpRequest request = _message.getHttpRequest();
        CustomRequest customRequest = new CustomRequest(request);

        if (request != null) {
            ProxyRoute.debugRequest(request);
            
            System.out.println("Validating...");
            final ValidationReport report = validator.validateRequest(customRequest);
            if (report.hasErrors()) {
                exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
                exchange.getIn().setHeader(Exchange.CONTENT_TYPE,  "application/json");
                exchange.getIn().setBody("{ \"error\": \""+report.getMessages()+"\"");

                for (ValidationReport.Message m : report.getMessages()) {
                    System.out.println("Error: " + m);
                }
            }
        }
        
        System.out.println("converting message");
        final Message message = exchange.getIn();
        final String body = message.getBody(String.class);
        message.setBody(body.toUpperCase(Locale.US));
        System.out.println("message converted");
    }

    public static void debugRequest(FullHttpRequest req){
        System.out.println("uri:" + req.uri());
        System.out.println("method: " + req.method());
        if (req.content() != null){
            System.out.println("BODY: "+req.content().toString(CharsetUtil.UTF_8));
        }
        System.out.println("headers: ");
        Iterator<Entry<String, String>> it = req.headers().iteratorAsString();
        while (it.hasNext()) {
            System.out.println("header -> " + it.next());
        }
    }

    public static ArtifactMetaData getSchemaFromRegistry(RegistryClient client, String artifactId) {

        LOGGER.info("---------------------------------------------------------");
        LOGGER.info("=====> Fetching artifact from the registry for JSON Schema with ID: {}", artifactId);
        try {
            final ArtifactMetaData metaData = client.getArtifactMetaData(null, artifactId);
            LOGGER.info("=====> Successfully fetched JSON Schema artifact in Service Registry: {}", metaData);
            LOGGER.info("---------------------------------------------------------");
            return metaData;
        } catch (Exception t) {
            throw t;
        }
    }
}
