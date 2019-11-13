package io.apicurio.registry.demo;

import io.apicurio.registry.demo.domain.Log;
import io.apicurio.registry.demo.domain.LogInput;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ales Justin
 */
class MergeTransformer implements Transformer<String, LogInput, KeyValue<String, Log.LogMerge>> {
    private static final Logger logger = LoggerFactory.getLogger(MergeTransformer.class);

    private KeyValueStore<String, Log.LogMerge> store;

    @Override
    public void init(ProcessorContext context) {
        //noinspection unchecked
        store = (KeyValueStore<String, Log.LogMerge>) context.getStateStore(ApplicationImpl.LOG_STORE);
    }

    @Override
    public KeyValue<String, Log.LogMerge> transform(String key, LogInput log) {
        String identifier = log.getIdentifier().toString();
        Log.LogMerge merge = store.delete(identifier); // get + remove
        if (merge != null) {
            merge = Log.LogMerge.newBuilder(merge)
                                .setSnd(log.getTimestamp())
                                .build();

            logger.info("Merged logs: {}", merge);

            return new KeyValue<>(identifier, merge);
        } else {
            merge = Log.LogMerge.newBuilder()
                                .setFst(log.getTimestamp())
                                .build();
            store.put(identifier, merge);
            return null; // skip
        }
    }

    @Override
    public void close() {
    }
}
