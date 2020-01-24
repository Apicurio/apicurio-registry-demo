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

import java.sql.Date;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.apicurio.registry.demo.utils.PropertiesUtil;
import io.apicurio.registry.utils.serde.AbstractKafkaSerDe;
import io.apicurio.registry.utils.serde.JsonSchemaKafkaDeserializer;

/**
 * Kafka application that does the following:
 * 
 * 1) Consumes messages from the topic!
 * 
 * The application uses the JSON Schema Kafka Deserializer to deserialize the message, which
 * will fetch the Schema from the Service Registry by its global identifier.
 * 
 * @author eric.wittmann@gmail.com
 */
public class SimpleJsonSchemaConsumerApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleJsonSchemaConsumerApp.class);
    
    public static void main(String [] args) throws Exception {
        // Config properties!
        Properties props = PropertiesUtil.properties(args);

        // Configure Kafka
        props.putIfAbsent(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.putIfAbsent(ConsumerConfig.GROUP_ID_CONFIG, "Consumer-" + SimpleJsonSchemaAppConstants.TOPIC_NAME);
        props.putIfAbsent(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.putIfAbsent(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.putIfAbsent(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.putIfAbsent(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonSchemaKafkaDeserializer.class.getName());

        // Configure the deserializer
        props.putIfAbsent(AbstractKafkaSerDe.REGISTRY_URL_CONFIG_PARAM, "http://localhost:8080");
        props.putIfAbsent(JsonSchemaKafkaDeserializer.REGISTRY_JSON_SCHEMA_DESERIALIZER_VALIDATION_ENABLED, Boolean.TRUE);

        // Create the Kafka Consumer
        KafkaConsumer<Long, Message> consumer = new KafkaConsumer<>(props);

        // Subscribe to the topic
        LOGGER.info("=====> Subscribing to topic: {}", SimpleJsonSchemaAppConstants.TOPIC_NAME);
        consumer.subscribe(Collections.singletonList(SimpleJsonSchemaAppConstants.TOPIC_NAME));

        // Consume messages!!
        LOGGER.info("=====> Consuming messages...");
        try {
            while (Boolean.TRUE) {
                final ConsumerRecords<Long, Message> records = consumer.poll(Duration.ofSeconds(1));
                if (records.count() == 0) {
                    // Do nothing - no messages waiting.
                } else records.forEach(record -> {
                    Message msg = record.value();
                    LOGGER.info("=====> CONSUMED: {} {} {} {}@{}", record.topic(),
                            record.partition(), record.offset(), msg.getMessage(), new Date(msg.getTime()));
                });
            }
        } finally {
            consumer.close();
        }

    }
}
