package net.dongliu.byproxy.data;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Set;

/**
 * Http content type
 *
 * @author Liu Dong
 */
public class ContentType {
    private static final Logger logger = LoggerFactory.getLogger(ContentType.class);

    private final String rawMimeType;
    private final MimeType mimeType;
    // may be null
    private final Charset charset;

    public static ContentType binary = ContentType.parse("application/octet-stream");

    public ContentType(String rawMimeType, Charset charset) {
        this.rawMimeType = rawMimeType;
        this.mimeType = MimeType.parse(rawMimeType);
        this.charset = charset;
    }

    public static ContentType parse(String str) {
        String[] items = str.split(";");
        String type = "";
        String encoding = null;
        for (int i = 0; i < items.length; i++) {
            if (i == 0) {
                type = items[i];
                continue;
            }
            String item = items[i].trim();
            int idx = item.indexOf("=");
            if (idx > 0) {
                if (item.substring(0, idx).trim().equalsIgnoreCase("charset")) {
                    encoding = item.substring(idx + 1).trim().replace("\"", "").replace("'", "");
                }
            }
        }
        return new ContentType(type, toCharsetSafe(encoding));
    }

    private static Charset toCharsetSafe(String encoding) {
        if (encoding == null) {
            return null;
        }
        try {
            return Charset.forName(encoding);
        } catch (IllegalCharsetNameException e) {
            logger.warn("unknown charset: {}", encoding);
            return null;
        }
    }

    private Set<String> textTypes = Set.of("text");
    private Set<String> textSubTypes = Set.of("json", "x-www-form-urlencoded", "xml", "x-javascript",
            "javascript", "html");

    public boolean isText() {
        return textTypes.contains(mimeType.getType())
                || textSubTypes.contains(mimeType.getSubType());
    }

    public boolean isImage() {
        return mimeType.getType().equals("image");
    }

    public String getRawMimeType() {
        return rawMimeType;
    }

    public MimeType getMimeType() {
        return mimeType;
    }

    public Charset getCharset() {
        return charset;
    }

    public Set<String> getTextTypes() {
        return textTypes;
    }

    public Set<String> getTextSubTypes() {
        return textSubTypes;
    }

    public static void main(String[] args) {
        System.out.println(Charset.forName("utf-8"));
    }
}
