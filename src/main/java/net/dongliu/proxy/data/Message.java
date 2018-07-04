package net.dongliu.proxy.data;

import net.dongliu.proxy.ui.component.Item;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

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
        this.host = requireNonNull(host);
        this.url = requireNonNull(url);
    }

    /**
     * For show in abstract
     */
    public abstract String displayText();

    public String host() {
        return host;
    }

    public String url() {
        return url;
    }
}
