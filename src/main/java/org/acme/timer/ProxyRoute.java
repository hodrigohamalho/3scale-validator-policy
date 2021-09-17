/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.acme.timer;

import java.util.Locale;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.apicurio.registry.rest.v2.beans.ArtifactMetaData;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
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

    /**
     * An injected bean
     */
    // @Inject
    // Counter counter;

    @Override
    public void configure() throws Exception {

        from("netty-http:proxy://0.0.0.0:8080")
                .process(ProxyRoute::uppercase)
                .toD("netty-http:"
                        + "${headers." + Exchange.HTTP_SCHEME + "}://"
                        + "${headers." + Exchange.HTTP_HOST + "}:"
                        + "${headers." + Exchange.HTTP_PORT + "}"
                        + "${headers." + Exchange.HTTP_PATH + "}")
                .process(ProxyRoute::uppercase);

        // fromF("timer:foo?period=%s", period)
        //         .setBody(exchange -> "Incremented the counter: " + counter.increment())
        //         // the configuration of the log component is done programmatically using CDI
        //         // by the org.acme.timer.Beans::log method.
        //         .to("log:example");
    }

    public static void uppercase(final Exchange exchange) {

        String registryUrl = "http://registry.apim.router-default.apps.cluster-5f88.5f88.sandbox1482.opentlc.com/apis/registry/v2";
        RegistryClient client = RegistryClientFactory.create(registryUrl);

        ArtifactMetaData oas = getSchemaFromRegistry(client, "petstore");
        System.out.println("Get the OAS: " + oas.getId());

        System.out.println("converting message");
        final Message message = exchange.getIn();
        final String body = message.getBody(String.class);
        message.setBody(body.toUpperCase(Locale.US));
        System.out.println("message converted");
    }

    public static ArtifactMetaData getSchemaFromRegistry(RegistryClient client, String artifactId) {

        LOGGER.info("---------------------------------------------------------");
        LOGGER.info("=====> Fetching artifact from the registry for JSON Schema with ID: {}", artifactId);
        try {
            final ArtifactMetaData metaData = client.getArtifactMetaData("default", artifactId);
            assert metaData != null;
            LOGGER.info("=====> Successfully fetched JSON Schema artifact in Service Registry: {}", metaData);
            LOGGER.info("---------------------------------------------------------");
            return metaData;
        } catch (Exception t) {
            throw t;
        }
    }
}
