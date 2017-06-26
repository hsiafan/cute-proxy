package net.dongliu.byproxy.parser;


import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.Serializable;
import java.util.List;

import static java.util.stream.Collectors.*;

/**
 * Commons for request headers and response headers
 *
 * @author Liu Dong
 */
@Immutable
public abstract class Headers implements Serializable {
    private static final long serialVersionUID = 8364988912653478880L;
    private List<Header> headers;

    public Headers(List<Header> headers) {
        this.headers = headers;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Headers(");
        Joiner.on(",").appendTo(sb, Lists.transform(headers, Header::raw));
        sb.append(")");
        return sb.toString();
    }

    /**
     * Get first header value by name
     *
     * @return null if not found
     */
    @Nullable
    public String getFirst(String name) {
        for (Header header : headers) {
            if (header.getName().equalsIgnoreCase(name)) {
                return header.getValue();
            }
        }
        return null;
    }

    /**
     * Get first header value by name
     *
     * @return null if not found
     */
    public List<String> getHeaders(String name) {
        return headers.stream().filter(h -> h.getName().equalsIgnoreCase(name)).map(Header::getValue).collect(toList());
    }

    /**
     * If is chunked http body
     */
    public boolean chunked() {
        return "chunked".equalsIgnoreCase(getFirst("Transfer-Encoding"));
    }

    /**
     * The content-len set in header.
     *
     * @return -1 if not found
     */
    public long contentLen() {
        String value = getFirst("Content-Length");
        if (value == null) {
            return -1;
        }
        return Long.parseLong(value);
    }

    /**
     * If should close connection after this msg.
     * Note: Only for http 1.1, 1.0 not supported
     */
    public boolean shouldClose() {
        String connection = getFirst("Connection");
        if ("close".equalsIgnoreCase(connection)) {
            return true;
        }
        return false;
    }

    /**
     * Get content type from http header
     */
    @Nullable
    public ContentType contentType() {
        String contentType = getFirst("Content-Type");
        if (contentType == null) {
            return null;
        }
        return ContentType.parse(contentType);
    }

    @Nullable
    public String contentEncoding() {
        return getFirst("Content-Encoding");
    }

    /**
     * Convert to raw lines string
     */
    public abstract List<String> toRawLines();

    public abstract List<KeyValue> getCookieValues();

    public List<Header> getHeaders() {
        return headers;
    }
}
