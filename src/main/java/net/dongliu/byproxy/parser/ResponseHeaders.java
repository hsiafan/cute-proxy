package net.dongliu.byproxy.parser;

import com.google.common.collect.Lists;
import jdk.nashorn.internal.ir.annotations.Immutable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.*;

/**
 * @author Liu Dong
 */
@Immutable
public class ResponseHeaders extends Headers implements Serializable {
    private static final long serialVersionUID = 299585070993883703L;
    private StatusLine statusLine;

    public ResponseHeaders(StatusLine statusLine, List<Header> headers) {
        super(headers);
        this.statusLine = statusLine;
    }

    public static ResponseHeaders parse(String rawStatueLine, List<String> rawHeaders) {
        return new ResponseHeaders(StatusLine.parse(rawStatueLine),
                rawHeaders.stream().map(Header::parse).collect(toList()));
    }

    @Override
    public String toString() {
        return "ResponseHeaders(statusLine=" + statusLine.raw() + ", headers=" + super.toString() + ")";
    }

    @Override
    public List<String> toRawLines() {
        List<String> rawLines = new ArrayList<>(getHeaders().size() + 1);
        rawLines.add(statusLine.raw());
        rawLines.addAll(Lists.transform(getHeaders(), Header::raw));
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
        int code = statusLine.getCode();
        return !(code >= 100 && code < 200 || code == 204 || code == 304);
    }

    @Override
    public List<KeyValue> getCookieValues() {
        return getHeaders("Set-Cookie").stream().map(CookieUtils::parseCookieHeader).collect(toList());
    }

    public StatusLine getStatusLine() {
        return statusLine;
    }
}
