package net.dongliu.byproxy.store;

import java.nio.ByteBuffer;

/**
 * Create, manage DataStores
 */
class DataStoreManager {
    private DataStore store = createStore();

    private static DataStoreManager instance = new DataStoreManager();

    /**
     * Get the singleton DataStoreManager instance
     */
    public static DataStoreManager getInstance() {
        return instance;
    }

    public synchronized Chunk store(ByteBuffer buffer) {
        int size = buffer.remaining();
        if (size > Stores.REGION_SIZE) {
            throw new RuntimeException("too large buffer size");
        }
        int pos = store.write(buffer);
        if (pos == -1) {
            store = createStore();
            pos = store.write(buffer);
        }
        return new Chunk(store, pos, size);
    }

    private DataStore createStore() {
        return new MMappedStore();
    }
}
