package net.dongliu.proxy.data;

import net.dongliu.proxy.utils.Texts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Http2 response headers
 */
public class Http2ResponseHeaders extends Http2Headers implements HttpResponseHeaders, Serializable {
    private static final long serialVersionUID = -7574758006808314305L;
    private final int status;

    public Http2ResponseHeaders(int status, List<Header> headers) {
        super(headers);
        this.status = status;
    }

    public int status() {
        return status;
    }

    @Override
    public List<String> rawLines() {
        List<Header> allHeaders = new ArrayList<>(1 + headers().size());
        allHeaders.add(new Header(":status", String.valueOf(status)));
        allHeaders.addAll(headers());
        return Texts.toAlignText(allHeaders, ": ");
    }

    @Override
    public List<NameValue> cookieValues() {
        return headers("Set-Cookie").stream().map(CookieUtils::parseCookieHeader).collect(toList());
    }

    @Override
    public int statusCode() {
        return status;
    }
}
