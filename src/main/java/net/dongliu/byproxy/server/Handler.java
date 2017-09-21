package net.dongliu.byproxy.server;

import java.io.IOException;

/**
 * Proxy handler
 *
 * @author Liu Dong
 */
public interface Handler {

    void handle() throws IOException;
}
