package net.dongliu.proxy.data;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

/**
 * Header with name and value
 *
 * @author Liu Dong
 */
public class Header implements NameValue, Serializable {
    private static final long serialVersionUID = 1616771076198243392L;
    private final String name;
    private final String value;
    private transient String rawHeader;


    public Header(String name, String value) {
        this.name = requireNonNull(name);
        this.value = requireNonNull(value);
    }

    /**
     * Parse header from header string
     */
    public static Header parse(String str) {
        requireNonNull(str);
        Header header;
        int idx = str.indexOf(':');
        if (idx >= 0) {
            header = new Header(str.substring(0, idx), str.substring(idx + 1).trim());
        } else {
            header = new Header(str, "");
        }
        header.rawHeader = str;
        return header;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String value() {
        return value;
    }

    public String rawHeader() {
        if (rawHeader == null) {
            rawHeader = name + ": " + value;
        }
        return rawHeader;
    }

    @Override
    public String toString() {
        return rawHeader();
    }
}
