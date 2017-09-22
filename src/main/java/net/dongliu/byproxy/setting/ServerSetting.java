package net.dongliu.byproxy.setting;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }

    /**
     * Get mainSetting file path
     */
    public static Path configPath() {
        return Settings.getParentPath().resolve(Paths.get("config"));
    }


    public static ServerSetting getDefault() {
        return new ServerSetting("", 2080, 1800);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getTimeout() {
        return timeout;
    }
}
