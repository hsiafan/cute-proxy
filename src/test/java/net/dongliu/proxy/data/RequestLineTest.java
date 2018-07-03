package net.dongliu.proxy.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Liu Dong
 */
public class RequestLineTest {
    @Test
    public void parse() throws Exception {
        assertEquals(new RequestLine("GET", "/macpicface/interface/get_hotlist.php", "HTTP/1.1"),
                RequestLine.parse("GET /macpicface/interface/get_hotlist.php HTTP/1.1"));
    }

}