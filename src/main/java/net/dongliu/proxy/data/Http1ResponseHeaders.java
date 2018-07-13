package net.dongliu.proxy.data;

import net.dongliu.commons.collection.Lists;
import net.dongliu.proxy.utils.NameValues;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * @author Liu Dong
 */
public class Http1ResponseHeaders extends Http1Headers implements HttpResponseHeaders, Serializable {
    private static final long serialVersionUID = 299585070993883703L;
    private StatusLine statusLine;

    public Http1ResponseHeaders(StatusLine statusLine, List<Header> headers) {
        super(headers);
        this.statusLine = statusLine;
    }

    public static Http1ResponseHeaders parse(String rawStatueLine, List<String> rawHeaders) {
        return new Http1ResponseHeaders(StatusLine.parse(rawStatueLine),
                rawHeaders.stream().map(Header::parse).collect(toList()));
    }

    @Override
    public String toString() {
        return "Http1ResponseHeaders(statusLine=" + statusLine.rawStatusLine() + ", headers=" + super.toString() + ")";
    }

    @Override
    public List<String> rawLines() {
        List<String> rawLines = new ArrayList<>(headers().size() + 1);
        rawLines.add(statusLine.rawStatusLine());
        rawLines.addAll(NameValues.toAlignText(headers(), ": "));
        return rawLines;
    }

    /**
     * If this request/response has no body.
     */
    public boolean hasBody() {
        /*
         * For response, a message-body is explicitly forbidden in responses to HEAD requests
         * a message-body is explicitly forbidden in 1xx (informational), 204 (no content), and 304 (not modified)
         * responses
         */
        int code = statusLine.code();
        return !(code >= 100 && code < 200 || code == 204 || code == 304);
    }

    @Override
    public List<NameValue> cookieValues() {
        return Lists.convert(getHeaders("Set-Cookie"), CookieUtils::parseCookieHeader);
    }

    public StatusLine statusLine() {
        return statusLine;
    }

    @Override
    public int statusCode() {
        return statusLine.code();
    }
}
