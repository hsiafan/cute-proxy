package net.dongliu.byproxy.server;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import net.dongliu.byproxy.Context;
import net.dongliu.byproxy.parser.*;
import net.dongliu.byproxy.store.HttpBody;
import net.dongliu.byproxy.struct.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toList;

/**
 * Non-connect http proxy request handler
 *
 * @author Liu Dong
 */
public class CommonProxyHandler implements Handler {
    private static Logger logger = LoggerFactory.getLogger(CommonProxyHandler.class);
    private final Socket serverSocket;
    private final HttpInputStream input;
    private final MessageListener messageListener;

    public CommonProxyHandler(Socket serverSocket, HttpInputStream input, MessageListener messageListener) {
        this.serverSocket = serverSocket;
        this.input = input;
        this.messageListener = messageListener;
    }

    @Override
    public void handle() throws IOException {
        input.enableBuffered();
        HttpOutputStream output = new HttpOutputStream(serverSocket.getOutputStream());

        while (true) {
            boolean shouldBreak = handleOneRequest(output);
            if (shouldBreak) {
                logger.debug("Server close connection");
                break;
            }
        }
    }

    //TODO: HttpUrlConnection always resolve dns before send request when using a proxy.
    private boolean handleOneRequest(HttpOutputStream fromOut) throws IOException {
        @Nullable HttpRequestHeader requestHeaders;
        try {
            requestHeaders = input.readRequestHeaders();
        } catch (SocketTimeoutException e) {
            logger.debug("read client request timeout", e);
            return true;
        }
        // client close connection
        if (requestHeaders == null) {
            logger.debug("Client close connection");
            return true;
        }
        RequestLine requestLine = requestHeaders.getRequestLine();
        logger.debug("Accept new request: {}", requestLine.raw());

        // expect-100. just tell client continue to send http body
        if ("100-continue".equalsIgnoreCase(requestHeaders.getFirst("Expect"))) {
            fromOut.writeLine("HTTP/1.1 100 Continue\r\n");
        }

        String messageId = MessageIdGenerator.getInstance().nextId();
        String method = requestLine.getMethod();
        Iterable<Header> newRequestHeaders = requestHeaders.getHeaders().stream()
                .filter(h -> !proxyRemoveHeaders.contains(h.getName())).collect(toList());
        String url = requestLine.getPath();
        boolean shouldClose = requestHeaders.shouldClose();

        Proxy proxy = Context.getInstance().getProxy();
        int timeout = Context.getInstance().getMainSetting().getTimeout() * 1000;
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection(proxy);
        conn.setRequestMethod(method);
        conn.setReadTimeout(timeout);
        conn.setConnectTimeout(timeout);
        conn.setInstanceFollowRedirects(false);
        for (Header header : newRequestHeaders) {
            conn.setRequestProperty(header.getName(), String.valueOf(header.getValue()));
        }
        if (shouldClose) {
            conn.setRequestProperty("Connection", "close");
        }

        HttpBody requestHttpBody = readRequestBody(input, requestHeaders);
        messageListener.onHttpRequest(messageId, new URL(url).getHost(), url, new HttpMessage(requestHeaders,
                requestHttpBody));

        if (requestHttpBody.size() > 0) {
            conn.setDoOutput(true);
        }
        try {
            conn.connect();
        } catch (SocketException e) {
            logger.error("connect to {} failed: {}", url, e.getMessage());
            return true;
        }
        if (requestHttpBody.size() > 0) {
            try (InputStream in = requestHttpBody.getInputStream()) {
                ByteStreams.copy(in, conn.getOutputStream());
            }
        }

        int statusCode;
        try {
            statusCode = conn.getResponseCode();
        } catch (SocketException | SocketTimeoutException e) {
            logger.error("Proxy request {} failed: {}", url, e.getMessage());
            return true;
        }

        String statusLine = null;
        // headers and cookies
        List<Header> headerList = new ArrayList<>();
        int index = 0;
        while (true) {
            String key = conn.getHeaderFieldKey(index);
            String value = conn.getHeaderField(index);
            if (value == null) {
                break;
            }
            index++;
            //status line
            if (key == null) {
                statusLine = value;
                continue;
            }
            headerList.add(new Header(key, value));
        }
        InputStream responseInput;
        try {
            responseInput = conn.getInputStream();
        } catch (IOException e) {
            responseInput = conn.getErrorStream();
        }

        Objects.requireNonNull(statusLine);
        fromOut.writeLine(statusLine);
        HttpResponseHeader responseHeaders = toResponseHeaders(statusLine, headerList);
        HttpBody responseHttpBody = readResponseBody(responseHeaders, responseInput);
        messageListener.onHttpResponse(messageId, new HttpMessage(responseHeaders, responseHttpBody));
        List<Header> newResponseHeaders = filterResponseHeaders(shouldClose, responseHeaders, responseHttpBody.size());
        fromOut.writeHeaders(newResponseHeaders);
        if (responseHttpBody.size() > 0) {
            try (InputStream in = responseHttpBody.getInputStream()) {
                ByteStreams.copy(in, fromOut);
            }
        }

        return shouldClose;
    }

    private HttpBody readRequestBody(HttpInputStream input, HttpRequestHeader requestHeaders) throws IOException {
        HttpBody httpBody = HttpBody.create(requestHeaders.contentType(), requestHeaders.contentEncoding());
        InputStream requestBody;
        long len = requestHeaders.contentLen();
        if (requestHeaders.chunked()) {
            requestBody = input.getChunkedBody();
        } else if (len >= 0) {
            requestBody = input.getFixLenBody(len);
        } else if (!requestHeaders.hasBody()) {
            requestBody = null;
        } else {
            requestBody = null;
        }

        if (requestBody != null) {
            try (InputStream in = requestBody) {
                httpBody.loadFromInput(in, len);
            }
        }
        httpBody.finish();
        return httpBody;
    }

    private HttpBody readResponseBody(HttpResponseHeader headers, @Nullable InputStream responseIn)
            throws IOException {
        HttpBody httpBody = HttpBody.create(headers.contentType(), headers.contentEncoding());
        if (responseIn != null) {
            try (InputStream in = responseIn) {
                httpBody.loadFromInput(in, headers.contentLen());
            }
        }
        httpBody.finish();
        return httpBody;
    }


    private List<Header> filterResponseHeaders(boolean shouldClose, HttpResponseHeader responseHeaders, long contentLen) {
        List<Header> newResponseHeaders = new ArrayList<>(responseHeaders.getHeaders());
        Set<String> removeHeaders = ImmutableSet.of("Transfer-Encoding", "Connection", "Content-Length");
        newResponseHeaders.removeIf(h -> removeHeaders.contains(h.getName()));
        if (!shouldClose) {
            newResponseHeaders.add(new Header("Connection", "Keep-Alive"));
        } else {
            newResponseHeaders.add(new Header("Connection", "Close"));
        }
        newResponseHeaders.add(new Header("Content-Length", String.valueOf(contentLen)));
        return newResponseHeaders;
    }

    private Set<String> proxyRemoveHeaders = ImmutableSet.of("Connection", "Proxy-Authenticate", "Proxy-Connection",
            "Transfer-Encoding");


    private HttpResponseHeader toResponseHeaders(String statusLine, List<Header> headers) {
        return new HttpResponseHeader(StatusLine.parse(statusLine), headers);
    }


}
