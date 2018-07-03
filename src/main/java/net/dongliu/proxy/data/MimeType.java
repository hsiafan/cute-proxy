package net.dongliu.proxy.data;

/**
 * Http content type
 *
 * @author Liu Dong
 */
public class MimeType {

    private final String type;
    private final String subType;

    public MimeType(String type, String subType) {
        this.type = type;
        this.subType = subType;
    }

    public static MimeType parse(String str) {
        int idx = str.indexOf('/');
        if (idx < 0) {
            return new MimeType(str, "");
        } else {
            return new MimeType(str.substring(0, idx), str.substring(idx + 1));
        }
    }

    public String getType() {
        return type;
    }

    public String getSubType() {
        return subType;
    }
}
