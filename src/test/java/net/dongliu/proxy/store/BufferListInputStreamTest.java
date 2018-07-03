package net.dongliu.proxy.store;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BufferListInputStreamTest {
    @Test
    public void read() throws Exception {
        BufferListInputStream input = new BufferListInputStream(List.of(
                ByteBuffer.wrap("This is a ".getBytes()),
                ByteBuffer.wrap("test ".getBytes()),
                ByteBuffer.wrap("for ".getBytes()),
                ByteBuffer.wrap("BufferListInputStream".getBytes())
        ));

        byte[] bytes = input.readAllBytes();
        assertEquals("This is a test for BufferListInputStream", new String(bytes));
    }

}