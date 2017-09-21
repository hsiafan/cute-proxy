package net.dongliu.byproxy.store;

import java.nio.ByteBuffer;

/**
 * Create, manage DataStores
 */
class DataStoreManager {
    private static final int MAX_REGION_SIZE = 16 * 1024 * 1024; // 16M
    private static final int INITIAL_REGION_SIZE = 1024 * 1024; // 1M
    private static DataStoreManager instance = new DataStoreManager();

    private DataStore store = createStore();
    private int regionSize = INITIAL_REGION_SIZE;


    /**
     * Get the singleton DataStoreManager instance
     */
    public static DataStoreManager getInstance() {
        return instance;
    }

    public synchronized Chunk store(ByteBuffer buffer) {
        int size = buffer.remaining();
        if (size > INITIAL_REGION_SIZE) {
            throw new RuntimeException("too large buffer size");
        }
        int pos = store.write(buffer);
        if (pos == -1) {
            store = createStore();
            pos = store.write(buffer);
        }
        return new Chunk(store, pos, size);
    }

    private synchronized DataStore createStore() {
        DataStore store;
        if (regionSize < MAX_REGION_SIZE) {
            store = new OffHeapStore(regionSize);
            regionSize *= 2;
        } else {
            store = new MMappedStore(regionSize);
        }
        return store;
    }
}
