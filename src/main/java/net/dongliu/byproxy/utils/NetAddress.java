package net.dongliu.byproxy.utils;

import java.util.Objects;

/**
 * Simple address
 */
public class NetAddress {
    private String host;
    private int port;

    public NetAddress(String host, int port) {
        this.host = Objects.requireNonNull(host);
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetAddress that = (NetAddress) o;

        if (port != that.port) return false;
        return host.equals(that.host);
    }

    @Override
    public int hashCode() {
        int result = host.hashCode();
        result = 31 * result + port;
        return result;
    }
}
