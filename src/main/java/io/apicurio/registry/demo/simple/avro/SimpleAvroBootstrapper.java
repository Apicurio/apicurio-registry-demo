/*
 * Copyright 2020 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apicurio.registry.demo.simple.avro;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.apicurio.registry.client.RegistryClient;
import io.apicurio.registry.client.RegistryService;
import io.apicurio.registry.rest.beans.ArtifactMetaData;
import io.apicurio.registry.types.ArtifactType;

/**
 * This command line application is used to register the schema used by the producer and consumer in the
 * Apicurio registry.  This must be run before the producer or consumer.  It only needs to be run one
 * time as it simply stores a schema in the registry.  If the registry is non-persistent (in-memory only)
 * then this will need to be executed once per registry startup.
 * 
 * @author eric.wittmann@gmail.com
 */
public class SimpleAvroBootstrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleAvroBootstrapper.class);
    
    private static RegistryService service; 
    static {
        // Create a Service Registry client
        String registryUrl = "http://localhost:8080";
        service = RegistryClient.create(registryUrl);
    }
    
    public static final void main(String [] args) throws Exception {
        try {
            LOGGER.info("\n\n--------------\nBootstrapping the Avro Schema demo.\n--------------\n");
            String topicName = SimpleAvroAppConstants.TOPIC_NAME;
    
            // Register the Avro Schema schema in the Apicurio registry.
            String artifactId = topicName;
            try {
                createSchemaInServiceRegistry(artifactId, SimpleAvroAppConstants.SCHEMA);
            } catch (Exception e) {
                if (is409Error(e)) {
                    LOGGER.warn("\n\n--------------\nWARNING: Schema already existed in registry!\n--------------\n");
                    return;
                } else {
                    throw e;
                }
            }
    
            LOGGER.info("\n\n--------------\nBootstrapping complete.\n--------------\n");
        } finally {
            service.close();
        }
    }

    /**
     * Create the artifact in the registry (or update it if it already exists).
     * @param artifactId
     * @param schema
     * @throws Exception 
     */
    private static void createSchemaInServiceRegistry(String artifactId, String schema) throws Exception {

        LOGGER.info("---------------------------------------------------------");
        LOGGER.info("=====> Creating artifact in the registry for Avro Schema with ID: {}", artifactId);
        try {
            ByteArrayInputStream content = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
            CompletionStage<ArtifactMetaData> artifact = service.createArtifact(ArtifactType.AVRO, artifactId, content);
            ArtifactMetaData metaData = artifact.toCompletableFuture().get();
            LOGGER.info("=====> Successfully created Avro Schema artifact in Service Registry: {}", metaData);
            LOGGER.info("---------------------------------------------------------");
        } catch (Exception t) {
            throw t;
        }
    }

    private static boolean is409Error(Exception e) {
        if (e.getCause() instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) e.getCause();
            if (wae.getResponse().getStatus() == 409) {
                return true;
            }
        }
        return false;
    }

}
