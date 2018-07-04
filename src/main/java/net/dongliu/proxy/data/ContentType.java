package net.dongliu.proxy.data;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Http content type
 *
 * @author Liu Dong
 */
public class ContentType {
    private static final Logger logger = LoggerFactory.getLogger(ContentType.class);

    private final String rawMimeType;
    private final MimeType mimeType;
    private final Optional<Charset> charset;

    public static ContentType binary = ContentType.parse("application/octet-stream");

    /**
     * Construct a ContentType with charset.
     */
    public ContentType(String rawMimeType, Optional<Charset> charset) {
        this.rawMimeType = requireNonNull(rawMimeType);
        this.mimeType = MimeType.parse(rawMimeType);
        this.charset = requireNonNull(charset);
    }

    /**
     * Construct a ContentType with no charset.
     */
    public ContentType(String rawMimeType) {
        this.rawMimeType = requireNonNull(rawMimeType);
        this.mimeType = MimeType.parse(rawMimeType);
        this.charset = Optional.empty();
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
        return new ContentType(type, parseCharset(encoding));
    }

    private static Optional<Charset> parseCharset(String encoding) {
        if (encoding == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Charset.forName(encoding));
        } catch (IllegalCharsetNameException e) {
            logger.warn("unknown charset: {}", encoding);
            return Optional.empty();
        }
    }

    private static Set<String> textTypes = Set.of("text");
    private static Set<String> textSubTypes = Set.of("json", "x-www-form-urlencoded", "xml", "x-javascript",
            "javascript", "html");

    public boolean isText() {
        return textTypes.contains(mimeType.getType())
                || textSubTypes.contains(mimeType.getSubType());
    }

    public boolean isImage() {
        return mimeType.getType().equals("image");
    }

    public String rawMimeType() {
        return rawMimeType;
    }

    public MimeType mimeType() {
        return mimeType;
    }

    public Optional<Charset> charset() {
        return charset;
    }

}
