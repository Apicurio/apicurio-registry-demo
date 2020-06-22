package io.apicurio.registry.demo;

import io.apicurio.registry.client.RegistryClient;
import io.apicurio.registry.client.RegistryService;
import io.apicurio.registry.demo.domain.Log;
import io.apicurio.registry.demo.domain.LogInput;
import io.apicurio.registry.demo.utils.PropertiesUtil;
import io.apicurio.registry.demo.utils.ProtoSerde;
import io.apicurio.registry.utils.serde.AvroKafkaDeserializer;
import io.apicurio.registry.utils.serde.AvroKafkaSerializer;
import io.apicurio.registry.utils.serde.avro.DefaultAvroDatumProvider;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Vetoed;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static io.apicurio.registry.demo.utils.PropertiesUtil.property;

/**
 * @author Ales Justin
 */
@Vetoed
public class ApplicationImpl implements Lifecycle {
    private static final Logger log = LoggerFactory.getLogger(ApplicationImpl.class);

    public static final String INPUT_TOPIC = "input-topic";
    public static final String LOG_STORE = "log-store";

    private KafkaStreams streams;

    public ApplicationImpl(String[] args) {
        Properties properties = PropertiesUtil.properties(args);
        properties.put(
            CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
            property(properties, CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
        );
        properties.put(
            StreamsConfig.APPLICATION_ID_CONFIG,
            property(properties, StreamsConfig.APPLICATION_ID_CONFIG, "registry-demo")
        );

        String registryUrl = property(properties, "registry.url", "http://localhost:8080/api");
        RegistryService service = RegistryClient.cached(registryUrl);

        StreamsBuilder builder = new StreamsBuilder();

        // Demo topology

        Map<String, String> configuration = new HashMap<>();
        configuration.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT);
        configuration.put(TopicConfig.MIN_COMPACTION_LAG_MS_CONFIG, "0");
        configuration.put(TopicConfig.SEGMENT_BYTES_CONFIG, String.valueOf(64 * 1024 * 1024));

        Deserializer<LogInput> deserializer = new AvroKafkaDeserializer<>(
            service,
            new DefaultAvroDatumProvider<LogInput>().setUseSpecificAvroReader(true)
        );
        Serde<LogInput> logSerde = Serdes.serdeFrom(
            new AvroKafkaSerializer<>(service),
            deserializer
        );
        KStream<String, LogInput> input = builder.stream(
            INPUT_TOPIC,
            Consumed.with(Serdes.String(), logSerde)
        );

        Serde<Log.LogMerge> logMergeSerde = new ProtoSerde<>(Log.LogMerge.parser());

        StoreBuilder<KeyValueStore<String, Log.LogMerge>> storageStoreBuilder =
            Stores
                .keyValueStoreBuilder(
                    Stores.inMemoryKeyValueStore(LOG_STORE),
                    Serdes.String(), logMergeSerde
                )
                .withCachingEnabled()
                .withLoggingEnabled(configuration);
        builder.addStateStore(storageStoreBuilder);

        KStream<String, Log.LogMerge> output = input.transform(
            MergeTransformer::new,
            LOG_STORE
        );

        // for Kafka console consumer show-case, pure String
        output.mapValues(value -> String.format("Log diff: %s", Math.abs(value.getSnd() - value.getFst())))
              .to("logx-topic", Produced.with(Serdes.String(), Serdes.String()));

        output.to("dbx-topic", Produced.with(Serdes.String(), logMergeSerde));

        Topology topology = builder.build(properties);
        streams = new KafkaStreams(topology, properties);
    }

    public void start() {
        log.info("Demo application starting ...");
        streams.start();
        log.info("Demo application started ...");
    }

    public void stop() {
        log.info("Demo application stopping ...");
        if (streams != null) {
            streams.close();
        }
        log.info("Demo application stopped ...");
    }
}
