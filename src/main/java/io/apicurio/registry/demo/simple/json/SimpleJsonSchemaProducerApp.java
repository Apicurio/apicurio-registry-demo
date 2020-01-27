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


import java.util.Date;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.apicurio.registry.demo.utils.PropertiesUtil;
import io.apicurio.registry.utils.serde.AbstractKafkaSerDe;
import io.apicurio.registry.utils.serde.AbstractKafkaSerializer;
import io.apicurio.registry.utils.serde.JsonSchemaKafkaSerializer;
import io.apicurio.registry.utils.serde.strategy.FindLatestIdStrategy;
import io.apicurio.registry.utils.serde.strategy.SimpleTopicIdStrategy;

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
        props.putIfAbsent(AbstractKafkaSerializer.REGISTRY_ARTIFACT_ID_STRATEGY_CONFIG_PARAM, SimpleTopicIdStrategy.class.getName());
        props.putIfAbsent(AbstractKafkaSerializer.REGISTRY_GLOBAL_ID_STRATEGY_CONFIG_PARAM, FindLatestIdStrategy.class.getName());
        props.putIfAbsent(JsonSchemaKafkaSerializer.REGISTRY_JSON_SCHEMA_SERIALIZER_VALIDATION_ENABLED, Boolean.TRUE);
        
        // Create the Kafka producer
        Producer<Object, Message> producer = new KafkaProducer<>(props);

        String topicName = SimpleJsonSchemaAppConstants.TOPIC_NAME;
        String subjectName = SimpleJsonSchemaAppConstants.SUBJECT_NAME;
        
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
}
