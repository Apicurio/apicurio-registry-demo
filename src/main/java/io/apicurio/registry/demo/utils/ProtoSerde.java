package io.apicurio.registry.demo.utils;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.UncheckedIOException;
import java.util.Map;

/**
 * @author Ales Justin
 */
public class ProtoSerde<T extends MessageLite> implements Serde<T>, Serializer<T>, Deserializer<T> {
    private final Parser<T> parser;

    public ProtoSerde(Parser<T> parser) {
        this.parser = parser;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, T data) {
        return data.toByteArray();
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        try {
            return parser.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public Serializer<T> serializer() {
        return this;
    }

    @Override
    public Deserializer<T> deserializer() {
        return this;
    }
}
