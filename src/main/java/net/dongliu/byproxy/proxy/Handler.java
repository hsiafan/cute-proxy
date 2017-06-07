package net.dongliu.byproxy.proxy;

import java.io.IOException;

/**
 * Proxy handler
 *
 * @author Liu Dong
 */
public interface Handler {

    void handle() throws IOException;
}
