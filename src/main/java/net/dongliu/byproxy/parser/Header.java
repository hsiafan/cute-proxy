package net.dongliu.byproxy.parser;

import java.io.Serializable;
import java.util.Objects;

/**
 * Header with name and value
 *
 * @author Liu Dong
 */
public class Header implements KeyValue, Serializable {
    private static final long serialVersionUID = 1616771076198243392L;
    private final String name;
    private final String value;
    private transient String raw;


    public Header(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Parse header from header string
     */
    public static Header parse(String str) {
        Objects.requireNonNull(str);
        Header header;
        int idx = str.indexOf(':');
        if (idx >= 0) {
            header = new Header(str.substring(0, idx), str.substring(idx + 1).trim());
        } else {
            header = new Header(str, "");
        }
        header.raw = str;
        return header;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return value;
    }

    public String raw() {
        if (raw == null) {
            raw = name + ": " + value;
        }
        return raw;
    }

    @Override
    public String toString() {
        return "Header(" + name + "=" + value + ")";
    }
}
