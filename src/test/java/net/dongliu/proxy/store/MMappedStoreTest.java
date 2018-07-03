package net.dongliu.proxy.store;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public class MMappedStoreTest {
    @Test
    public void write() throws Exception {
        ByteBuffer bb = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5, 6});
        MMappedStore store = new MMappedStore(1024);
        int pos = store.write(bb);
        assertEquals(0, pos);
        bb.rewind();
        pos = store.write(bb);
        assertEquals(6, pos);

        ByteBuffer b = store.read(6, 6);
        assertEquals(6, b.limit());
        assertEquals(1, b.get(0));
        assertEquals(6, b.get(5));
    }

    @Test
    public void read() throws Exception {
    }

}