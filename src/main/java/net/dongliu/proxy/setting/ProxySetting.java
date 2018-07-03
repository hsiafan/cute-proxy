package net.dongliu.proxy.setting;

import java.io.Serializable;

/**
 * @author Liu Dong
 */
public class ProxySetting implements Serializable {
    private static final long serialVersionUID = 7257755061846443485L;
    private String type;
    private String host;
    private int port;
    private String user;
    private String password;
    private boolean use;

    public static final String TYPE_SOCKS5 = "socks5";
    public static final String TYPE_SOCKS4 = "socks4";
    public static final String TYPE_HTTP = "http";

    public ProxySetting(String type, String host, int port, String user, String password, boolean use) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.use = use;
    }

    public static ProxySetting getDefault() {
        return new ProxySetting(TYPE_SOCKS5, "", 0, "", "", false);
    }

    public String getType() {
        return type;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public boolean isUse() {
        return use;
    }
}
