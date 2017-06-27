package net.dongliu.byproxy.parser;

import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

/**
 * Http request headers
 *
 * @author Liu Dong
 */
public class RequestHeaders extends Headers implements Serializable {
    private static final long serialVersionUID = 6625148408370480848L;
    private RequestLine requestLine;

    public RequestHeaders(RequestLine requestLine, List<Header> headers) {
        super(headers);
        this.requestLine = requestLine;
    }

    public static RequestHeaders parse(String rawRequestLine, List<String> rawHeaders) {
        return new RequestHeaders(RequestLine.parse(rawRequestLine),
                rawHeaders.stream().map(Header::parse).collect(toList()));
    }

    @Override
    public String toString() {
        return "RequestHeaders(requestLine=" + requestLine + ", headers=" + super.toString() + ")";
    }

    @Override
    public List<String> toRawLines() {
        List<String> rawLines = new ArrayList<>(getHeaders().size() + 1);
        rawLines.add(requestLine.raw());
        rawLines.addAll(Lists.transform(getHeaders(), Header::raw));
        return rawLines;
    }

    /**
     * If this request/response has body.
     */
    public boolean hasBody() {
        return !"TRACE".equalsIgnoreCase(requestLine.getMethod())
                && !"GET".equalsIgnoreCase(requestLine.getMethod())
                && !"OPTIONS".equalsIgnoreCase(requestLine.getMethod());
    }

    @Override
    public List<KeyValue> getCookieValues() {
        String value = getFirst("Cookie");
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        return Stream.of(value.split(";")).map(String::trim).map(Parameter::parse).collect(toList());
    }

    public RequestLine getRequestLine() {
        return requestLine;
    }
}
