package net.dongliu.byproxy.data;


import javax.annotation.concurrent.Immutable;

/**
 * Parameter
 *
 * @author Liu Dong
 */
@Immutable
public class Parameter implements KeyValue {
    private final String name;
    private final String value;


    public Parameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Parse header from header string
     */
    public static Parameter parse(String str) {
        int idx = str.indexOf('=');
        if (idx >= 0) {
            return new Parameter(str.substring(0, idx).trim(), str.substring(idx + 1).trim());
        } else {
            return new Parameter(str.trim(), "");
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return value;
    }
}
