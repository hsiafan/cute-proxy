package net.dongliu.proxy.data;


/**
 * Parameter
 *
 * @author Liu Dong
 */
public class Parameter implements NameValue {
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
    public String name() {
        return name;
    }

    @Override
    public String value() {
        return value;
    }
}
