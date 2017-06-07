package net.dongliu.byproxy;

import java.io.IOException;
import java.net.Socket;

/**
 * @author Liu Dong
 */
public interface Dialer {
    Socket dial(String host, int port) throws IOException;
}
