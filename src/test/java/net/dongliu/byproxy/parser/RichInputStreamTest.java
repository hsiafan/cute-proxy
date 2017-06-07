package net.dongliu.byproxy.parser;

import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Liu Dong
 */
public class RichInputStreamTest {
    @Test
    public void readLine() throws Exception {
        RichInputStream input = new RichInputStream(
                new ByteArrayInputStream(("This is a test\r\ntest is a test\r\r\ntest").getBytes()));
        String line = input.readLine();
        assertEquals("This is a test", line);
        line = input.readLine();
        assertEquals("test is a test\r", line);
        line = input.readLine();
        assertEquals("test", line);
        line = input.readLine();
        assertNull(line);
    }

}