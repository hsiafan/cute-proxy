package net.dongliu.proxy.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class Http2RequestHeaders extends Http2Headers implements Serializable {
    private final String scheme;
    private final String method;
    private final String path;
    // only for connect
    private final String authority;

    public Http2RequestHeaders(List<Header> headers, String scheme, String method, String path) {
        this(headers, scheme, method, path, "");
    }

    public Http2RequestHeaders(List<Header> headers, String scheme, String method, String path, String authority) {
        super(headers);
        this.scheme = scheme;
        this.method = method;
        this.path = path;
        this.authority = authority;
    }

    @Override
    public String toString() {
        return "Http2RequestHeaders{" +
                "scheme='" + scheme + '\'' +
                ", method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", authority='" + authority + '\'' +
                '}';
    }

    public String getScheme() {
        return scheme;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    @Override
    public List<String> toRawLines() {
        List<Header> headers = getHeaders();
        List<String> lines = new ArrayList<>(headers.size() + 3);
        lines.add(":scheme: " + scheme);
        lines.add(":method: " + method);
        lines.add(":path: " + path);
        headers.forEach(h -> lines.add(h.getName() + ": " + h.getValue()));
        return lines;
    }

    @Override
    public List<KeyValue> getCookieValues() {
        return getHeader("Cookie").stream().flatMap(v -> Stream.of(v.split(";")))
                .map(String::trim)
                .map(Parameter::parse)
                .collect(toList());
    }
}
