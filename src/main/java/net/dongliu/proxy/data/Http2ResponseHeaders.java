package net.dongliu.proxy.data;

import net.dongliu.commons.collection.Lists;
import net.dongliu.proxy.utils.NameValues;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
        allHeaders.addAll(headers());
        return NameValues.toAlignText(allHeaders, ": ");
    }

    @Override
    public List<NameValue> cookieValues() {
        return Lists.convert(getHeaders("Set-Cookie"), CookieUtils::parseCookieHeader);
    }

    @Override
    public int statusCode() {
        return status;
    }
}
