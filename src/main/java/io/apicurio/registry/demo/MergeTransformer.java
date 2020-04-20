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

    @SuppressWarnings("unchecked")
	@Override
    public void init(ProcessorContext context) {
        //noinspection unchecked
        store = (KeyValueStore<String, Log.LogMerge>) context.getStateStore(ApplicationImpl.LOG_STORE);
    }

    @Override
    public KeyValue<String, Log.LogMerge> transform(String key, LogInput log) {
        Log.LogMerge merge = store.delete(key); // get + remove
        if (merge != null) {
            merge = Log.LogMerge.newBuilder(merge)
                                .setSnd(log.getTimestamp())
                                .build();

            logger.info("Merged logs: {}", merge);

            return new KeyValue<>(key, merge);
        } else {
            merge = Log.LogMerge.newBuilder()
                                .setFst(log.getTimestamp())
                                .build();
            store.put(key, merge);
            return null; // skip
        }
    }

    @Override
    public void close() {
    }
}
