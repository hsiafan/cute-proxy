package net.dongliu.byproxy.data;

import net.dongliu.byproxy.store.Body;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public abstract class HttpHeaders {
    private final List<Header> headers;

    protected HttpHeaders(List<Header> headers) {
        this.headers = headers;
    }

    public Optional<String> getHeader(String name) {
        return headers.stream()
                .filter(h -> h.getName().equalsIgnoreCase(name))
                .map(Header::getValue)
                .findFirst();
    }

    /**
     * Get first header value by name
     *
     * @return null if not found
     */
    public List<String> getHeaders(String name) {
        return headers.stream()
                .filter(h -> h.getName().equalsIgnoreCase(name))
                .map(Header::getValue)
                .collect(toList());
    }

    /**
     * Get content type from http header
     */
    public Optional<ContentType> getContentType() {
        return getHeader("Content-Type").map(ContentType::parse);
    }

    public Optional<String> getContentEncoding() {
        return getHeader("Content-Encoding");
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public Body createBody() {
        return Body.create(getContentType().orElse(ContentType.binary), getContentEncoding().orElse(""));
    }

    /**
     * Convert to raw lines string
     */
    public abstract List<String> toRawLines();

    public abstract List<KeyValue> getCookieValues();
}
