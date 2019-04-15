package net.dongliu.proxy.utils;

import org.junit.Test;

import static net.dongliu.proxy.utils.Networks.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Liu Dong
 */
public class NetworksTest {
    @Test
    public void wildcardHost() throws Exception {
        assertEquals("www.baidu.com", Networks.wildcardHost("www.baidu.com"));
        assertEquals("127.0.0.1", Networks.wildcardHost("127.0.0.1"));
        assertEquals("*.p.baidu.com", Networks.wildcardHost("t.p.baidu.com"));

    }

    @Test
    public void genericMultiCDNS() throws Exception {
        String h = Networks.genericMultiCDNS("img1.fbcdn.com");
        assertEquals("img*.fbcdn.com", h);
    }

    @Test
    public void getHostType() throws Exception {
        assertEquals(HOST_TYPE_IPV6, Networks.getHostType("2031:0000:1F1F:0000:0000:0100:11A0:ADDF"));
        assertEquals(HOST_TYPE_IPV4, Networks.getHostType("202.38.64.14"));
        assertEquals(HOST_TYPE_DOMAIN, Networks.getHostType("v2ex.com"));
    }

}