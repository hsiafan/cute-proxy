package net.dongliu.byproxy.parser;

import net.dongliu.commons.io.InputStreams;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

/**
 * @author Liu Dong
 */
public class ChunkedInputStreamTest {
    @Test
    public void read() throws Exception {
        String data = "b\r\n" +
                "01234567890\r\n" +
                "5\r\n" +
                "12345\r\n" +
                "0\r\n" +
                "\r\n";
        try (ByteArrayInputStream input = new ByteArrayInputStream(data.getBytes());
             ChunkedInputStream stream = new ChunkedInputStream(input)) {
            byte[] bytes = InputStreams.readAll(stream);
            assertEquals("0123456789012345", new String(bytes, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void skip() throws Exception {

    }

    @Test
    public void available() throws Exception {

    }

    @Test
    public void close() throws Exception {

    }

}