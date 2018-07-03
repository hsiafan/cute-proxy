package net.dongliu.proxy.store;

import io.netty.util.concurrent.FastThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Create, manage DataStores, using thread-local.
 */
class DataStoreManager {
    private static final Logger logger = LoggerFactory.getLogger(DataStoreManager.class);
    private static final int CHUNK_SIZE = 4 * 1024 * 1024; // 4M
    private static DataStoreManager instance = new DataStoreManager();

    private FastThreadLocal<DataStore> storeLocal = new FastThreadLocal<>();
    private AtomicLong totalSize = new AtomicLong();


    /**
     * Get the singleton DataStoreManager instance
     */
    public static DataStoreManager getInstance() {
        return instance;
    }

    /**
     * Return one data store can store the size of data.
     */
    public DataStore fetchStore(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("size less than 0");
        }
        if (size > CHUNK_SIZE) {
            throw new RuntimeException("too large buffer size");
        }
        DataStore dataStore = storeLocal.get();
        if (dataStore != null && dataStore.remaining() > size) {
            return dataStore;
        }
        dataStore = createStore();
        storeLocal.set(dataStore);
        return dataStore;
    }

    private DataStore createStore() {
        logger.debug("allocate data store with size {}, total size:{}", CHUNK_SIZE, totalSize.get());
        DataStore store;
        if (totalSize.getAndAdd(CHUNK_SIZE) < 128 * 1024 * 1024) {
            store = new OffHeapStore(CHUNK_SIZE);
        } else {
            store = new MMappedStore(CHUNK_SIZE);
        }
        return store;
    }
}
