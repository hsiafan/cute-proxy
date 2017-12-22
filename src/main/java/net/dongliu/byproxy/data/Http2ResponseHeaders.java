package net.dongliu.byproxy.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class Http2ResponseHeaders extends Http2Headers implements Serializable {
    private final String status;

    public Http2ResponseHeaders(String status, List<Header> headers) {
        super(headers);
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public List<String> toRawLines() {
        List<Header> headers = getHeaders();
        List<String> lines = new ArrayList<>(headers.size() + 3);
        lines.add(":status: " + status);
        headers.forEach(h -> lines.add(h.getName() + ": " + h.getValue()));
        return lines;
    }

    @Override
    public List<KeyValue> getCookieValues() {
        return getHeaders("Set-Cookie").stream().map(CookieUtils::parseCookieHeader).collect(toList());
    }
}
