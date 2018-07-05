package net.dongliu.proxy.data;

import net.dongliu.proxy.store.Body;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * Http headers.
 */
public abstract class HttpHeaders {
    private final List<Header> headers;

    protected HttpHeaders(List<Header> headers) {
        this.headers = requireNonNull(headers);
    }

    public Optional<String> getHeader(String name) {
        return headers.stream()
                .filter(h -> h.name().equalsIgnoreCase(name))
                .map(Header::value)
                .findFirst();
    }

    /**
     * Get first header value by name
     *
     * @return null if not found
     */
    public List<String> headers(String name) {
        return headers.stream()
                .filter(h -> h.name().equalsIgnoreCase(name))
                .map(Header::value)
                .collect(toList());
    }

    /**
     * Get content type from http header
     */
    public Optional<ContentType> contentType() {
        return getHeader("Content-type").map(ContentType::parse);
    }

    public Optional<String> contentEncoding() {
        return getHeader("Content-Encoding");
    }

    public List<Header> headers() {
        return headers;
    }

    public Body createBody() {
        return Body.create(contentType().orElse(ContentType.binary), contentEncoding().orElse(""));
    }

    /**
     * Convert to raw lines string
     */
    public abstract List<String> rawLines();

    public abstract List<NameValue> cookieValues();
}