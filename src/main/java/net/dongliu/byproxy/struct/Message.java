package net.dongliu.byproxy.struct;

import net.dongliu.byproxy.ui.component.Item;

import java.io.Serializable;

/**
 * Message parent class
 *
 * @author Liu Dong
 */
public abstract class Message extends Item implements Serializable {
    private static final long serialVersionUID = 434844783179505084L;
    private String host;
    private String url;

    protected Message(String host, String url) {
        this.host = host;
        this.url = url;
    }

    /**
     * For show in abstract
     */
    public abstract String displayText();

    public String getHost() {
        return host;
    }

    public String getUrl() {
        return url;
    }
}
