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

package io.apicurio.registry.demo.simple.json;


import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.WebApplicationException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.apicurio.registry.client.RegistryClient;
import io.apicurio.registry.client.RegistryService;
import io.apicurio.registry.demo.utils.PropertiesUtil;
import io.apicurio.registry.rest.beans.ArtifactMetaData;
import io.apicurio.registry.types.ArtifactType;
import io.apicurio.registry.utils.serde.AbstractKafkaSerDe;
import io.apicurio.registry.utils.serde.JsonSchemaKafkaSerializer;

/**
 * Kafka application that does the following:
 * 
 * 1) Registers the JSON Schema in the Service Registry
 * 2) Produces a {@link Message} every 5s on the topic
 * 
 * @author eric.wittmann@gmail.com
 */
public class SimpleJsonSchemaProducerApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleJsonSchemaProducerApp.class);
    
    public static void main(String [] args) throws Exception {
        // Config properties!
        Properties props = PropertiesUtil.properties(args);

        // Configure kafka.
        props.putIfAbsent(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.putIfAbsent(ProducerConfig.CLIENT_ID_CONFIG, "Producer-" + SimpleJsonSchemaAppConstants.TOPIC_NAME);
        props.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
        props.putIfAbsent(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.putIfAbsent(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSchemaKafkaSerializer.class.getName());

        // Configure Service Registry location and ID strategies
        props.putIfAbsent(AbstractKafkaSerDe.REGISTRY_URL_CONFIG_PARAM, "http://localhost:8080");
        props.putIfAbsent(JsonSchemaKafkaSerializer.REGISTRY_JSON_SCHEMA_SERIALIZER_VALIDATION_ENABLED, Boolean.TRUE);

        // Create the Kafka producer
        Producer<Object, Message> producer = new KafkaProducer<>(props);

        String topicName = SimpleJsonSchemaAppConstants.TOPIC_NAME;
        String subjectName = SimpleJsonSchemaAppConstants.SUBJECT_NAME;
        
        // Create the schema and then register it in the Service Registry
        String registryUrl = props.getProperty(AbstractKafkaSerDe.REGISTRY_URL_CONFIG_PARAM);
        String artifactId = topicName;
        createOrUpdateSchemaInServiceRegistry(registryUrl, artifactId, SimpleJsonSchemaAppConstants.SCHEMA);
        
        
        // Now start producing messages!
        int producedMessages = 0;
        try {
            while (Boolean.TRUE) {
                Date now = new Date();
                
                // Create the message to send
                Message message = new Message();
                message.setMessage("Hello (" + producedMessages++ + ")!");
                message.setTime(now.getTime());
                
                // Send/produce the message on the Kafka Producer
                LOGGER.info("=====> Sending message {} to topic {}", message, topicName);
                ProducerRecord<Object, Message> producedRecord = new ProducerRecord<>(topicName, subjectName, message);
                producer.send(producedRecord);
                
                Thread.sleep(3000);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to PRODUCE message!", e);
        } finally {
            producer.flush();
            producer.close();
            System.exit(1);
        }
    }

    /**
     * Create the artifact in the registry (or update it if it already exists).
     * @param registryUrl
     * @param artifactId
     * @param schema
     * @throws Exception 
     */
    private static void createOrUpdateSchemaInServiceRegistry(String registryUrl, String artifactId,
            String schema) throws Exception {
        // Create a Service Registry client
        RegistryService service = RegistryClient.cached(registryUrl);

        LOGGER.info("---------------------------------------------------------");
        LOGGER.info("=====> Creating artifact in the registry for JSON Schema with ID: {}", artifactId);
        try {
            ByteArrayInputStream content = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
            CompletionStage<ArtifactMetaData> artifact = service.createArtifact(ArtifactType.AVRO, artifactId, content);
            ArtifactMetaData metaData = artifact.toCompletableFuture().get();
            LOGGER.info("=====> Successfully created JSON Schema artifact in Service Registry: {}", metaData);
            LOGGER.info("---------------------------------------------------------");
            return;
        } catch (Exception t) {
            if (!is409Error(t)) {
                LOGGER.error("=====> Failed to create artifact in Service Registry!", t);
                LOGGER.info("---------------------------------------------------------");
                throw t;
            }
        }
        
        // If we get here, we need to update the artifact
        try {
            ByteArrayInputStream content = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
            CompletionStage<ArtifactMetaData> artifact = service.updateArtifact(artifactId, ArtifactType.AVRO, content);
            ArtifactMetaData metaData = artifact.toCompletableFuture().get();
            LOGGER.info("=====> Successfully **updated** JSON Schema artifact in Service Registry: {}", metaData);
            LOGGER.info("---------------------------------------------------------");
            return;
        } catch (Exception t) {
            if (!is409Error(t)) {
                LOGGER.error("=====> Failed to create artifact in Service Registry!", t);
                LOGGER.info("---------------------------------------------------------");
                throw t;
            }
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
