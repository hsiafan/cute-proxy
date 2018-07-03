package net.dongliu.proxy.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Liu Dong
 */
public class StatusLineTest {
    @Test
    public void parse() throws Exception {
        assertEquals(new StatusLine("HTTP/1.1", 200, "OK"), StatusLine.parse("HTTP/1.1 200 OK"));
        assertEquals(new StatusLine("HTTP/1.1", 405, "Method Not Allowed"),
                StatusLine.parse("HTTP/1.1 405 Method Not Allowed"));

    }

}