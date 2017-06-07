package net.dongliu.byproxy.parser;

import java.io.Serializable;

/**
 * Message parent class
 *
 * @author Liu Dong
 */
public abstract class Message implements Serializable {
    private static final long serialVersionUID = 434844783179505084L;
    private String id;
    private String host;
    private String url;

    protected Message(String id, String host, String url) {
        this.id = id;
        this.host = host;
        this.url = url;
    }

    /**
     * For show in abstract
     */
    public abstract String getDisplay();

    public String getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public String getUrl() {
        return url;
    }
}
