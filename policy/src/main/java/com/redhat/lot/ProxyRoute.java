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

        from("netty-http:proxy://0.0.0.0:8000").process(ProxyRoute::uppercase)
                .toD("netty-http:" + "${headers." + Exchange.HTTP_SCHEME + "}://" + "${headers." + Exchange.HTTP_HOST
                        + "}:" + "${headers." + Exchange.HTTP_PORT + "}" + "${headers." + Exchange.HTTP_PATH + "}")
                .process(ProxyRoute::uppercase);

        // from("timer:foo?repeatCount=1").process(ProxyRoute::uppercase);

    }

    public static void uppercase(final Exchange exchange) {
        final OpenApiInteractionValidator validator = OpenApiInteractionValidator.createForSpecificationUrl(
                "https://raw.githubusercontent.com/OAI/OpenAPI-Specification/main/examples/v3.0/petstore-expanded.json")
                // .withBasePathOverride("/")
                .build();

        final NettyHttpMessage _message = exchange.getIn(NettyHttpMessage.class);
        final FullHttpRequest request = _message.getHttpRequest();
        CustomRequest customRequest = new CustomRequest(request);

        if (request == null) {
            System.out.println("null prestou não...");
        } else {
            System.out.println("uri:" + request.uri());
            System.out.println("method: " + request.method());
            if (request.content() != null){
                System.out.println("BODY: "+request.content().toString(CharsetUtil.UTF_8));
            }
            System.out.println("headers: ");
            Iterator<Entry<String, String>> it = request.headers().iteratorAsString();
            while (it.hasNext()) {
                System.out.println("header -> " + it.next());
            }
            System.out.println("Começando a validação...");
            final ValidationReport report = validator.validateRequest(customRequest);
            if (report.hasErrors()) {
                exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 403);
                exchange.getIn().setHeader(Exchange.CONTENT_TYPE,  "text/plain");
                exchange.getIn().setBody("Forbidden");
                for (ValidationReport.Message m : report.getMessages()) {
                    System.out.println("Error: " + m);
                    final FullHttpResponse response = _message.getHttpResponse();
                    if (response != null){
                        response.setStatus(HttpResponseStatus.BAD_REQUEST);
                    }
                }
            } else {
                System.out.println("converting message");
                final Message message = exchange.getIn();
                final String body = message.getBody(String.class);
                message.setBody(body.toUpperCase(Locale.US));
                System.out.println("message converted");
            }
        }

        // String registryUrl = "http://registry.demo.router-default.apps.cluster-5f88.5f88.sandbox1482.opentlc.com";
        // RegistryClient client = RegistryClientFactory.create(registryUrl);

        // ArtifactMetaData oas = getSchemaFromRegistry(client, "petstore");
        // System.out.println("Get the OAS: " + oas.getId());

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
