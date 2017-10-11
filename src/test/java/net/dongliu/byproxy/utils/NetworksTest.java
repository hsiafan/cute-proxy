package net.dongliu.byproxy.utils;

import org.junit.Test;

import static net.dongliu.byproxy.utils.Networks.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Liu Dong
 */
public class NetworksTest {
    @Test
    public void getAddresses() throws Exception {

    }

    @Test
    public void parseAddress() throws Exception {
        NetAddress address = Networks.parseAddress("mvnrepository.com:443");
        assertEquals("mvnrepository.com", address.getHost());
        assertEquals(443, address.getPort());
        NetAddress netAddress = Networks.parseAddress("mvnrepository.com");
        assertEquals("mvnrepository.com", netAddress.getHost());
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