package com.redhat.lot;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.ValidationReport;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.CharsetUtil;
import java.util.Iterator;
import java.util.Map.Entry;
import javax.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.netty.http.NettyHttpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ProxyRoute extends RouteBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyRoute.class);

    @Override
    public void configure() throws Exception {

        from("netty-http:proxy://0.0.0.0:8000")
            .process(ProxyRoute::validate)
            .toD("netty-http:" 
                + "${headers." + Exchange.HTTP_SCHEME + "}://" 
                + "${headers." + Exchange.HTTP_HOST + "}:" 
                + "${headers." + Exchange.HTTP_PORT + "}" 
                + "${headers." + Exchange.HTTP_PATH + "}");

    }

    public static void validate(final Exchange exchange) {
        final OpenApiInteractionValidator validator = OpenApiInteractionValidator.createForSpecificationUrl(
                "http://registry.demo.router-default.apps.cluster-8d57.8d57.sandbox1409.opentlc.com/api/artifacts/person")
                // .withBasePathOverride("/")
                .build();

        final NettyHttpMessage _message = exchange.getIn(NettyHttpMessage.class);
        final FullHttpRequest request = _message.getHttpRequest();
        CustomRequest customRequest = new CustomRequest(request);

        if (request != null) {
            ProxyRoute.debugRequest(request);
            
            LOGGER.info("Validating...");
            final ValidationReport report = validator.validateRequest(customRequest);
            if (report.hasErrors()) {
                LOGGER.info("Erros found");
                exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
                exchange.getIn().setHeader(Exchange.CONTENT_TYPE,  "application/json");
                exchange.getIn().setBody("{ \"error\": \""+report.getMessages()+"\"");

                for (ValidationReport.Message m : report.getMessages()) {
                    LOGGER.info("Error: " + m);
                }
            }
        }
  
    }

    public static void debugRequest(FullHttpRequest req){
        LOGGER.info("uri:" + req.uri());
        LOGGER.info("method: " + req.method());
        if (req.content() != null){
            LOGGER.info("BODY: "+req.content().toString(CharsetUtil.UTF_8));
        }
        LOGGER.info("headers: ");
        Iterator<Entry<String, String>> it = req.headers().iteratorAsString();
        while (it.hasNext()) {
            LOGGER.info("header -> " + it.next());
        }
    }

}
