package net.dongliu.byproxy.store;

import java.nio.ByteBuffer;

/**
 * Store byte data
 */
interface DataStore {

    /**
     * Write data to store, return the start position at the store.
     * If store do not have enough space, return -1
     */
    int write(ByteBuffer buffer);

    /**
     * Read data at specified position
     */
    ByteBuffer read(int offset, int size);
}
