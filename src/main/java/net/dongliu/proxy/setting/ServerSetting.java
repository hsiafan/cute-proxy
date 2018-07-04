package net.dongliu.proxy.setting;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;

/**
 * The proxy mainSetting infos
 *
 * @author Liu Dong
 */
public class ServerSetting implements Serializable {
    private static final long serialVersionUID = -1828819182428842928L;
    private final String host;
    private final int port;
    // timeout in seconds
    private final int timeout;

    public ServerSetting(String host, int port, int timeout) {
        this.host = requireNonNull(host);
        this.port = port;
        this.timeout = timeout;
    }

    /**
     * Get mainSetting file path
     */
    public static Path configPath() {
        return Settings.getParentPath().resolve(Paths.get("config"));
    }


    public static ServerSetting newDefaultServerSetting() {
        return new ServerSetting("", 2080, 1800);
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public int timeout() {
        return timeout;
    }
}
