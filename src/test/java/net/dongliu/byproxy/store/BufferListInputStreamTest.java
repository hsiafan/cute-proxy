package net.dongliu.byproxy.store;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class BufferListInputStreamTest {
    @Test
    public void read() throws Exception {
        BufferListInputStream input = new BufferListInputStream(ImmutableList.of(
                ByteBuffer.wrap("This is a ".getBytes()),
                ByteBuffer.wrap("test ".getBytes()),
                ByteBuffer.wrap("for ".getBytes()),
                ByteBuffer.wrap("BufferListInputStream".getBytes())
        ));

        byte[] bytes = ByteStreams.toByteArray(input);
        assertEquals("This is a test for BufferListInputStream", new String(bytes));
    }

}