package net.dongliu.byproxy.proxy;

import net.dongliu.byproxy.Context;
import net.dongliu.byproxy.parser.*;
import net.dongliu.byproxy.parser.TLSInputStream.ClientHello;
import net.dongliu.byproxy.parser.TLSInputStream.HandShakeMessage;
import net.dongliu.byproxy.parser.TLSInputStream.TLSPlaintextHeader;
import net.dongliu.byproxy.store.BodyStore;
import net.dongliu.byproxy.utils.NetUtils;
import net.dongliu.commons.Strings;
import net.dongliu.commons.collection.Sets;
import net.dongliu.commons.io.Closeables;
import net.dongliu.commons.io.InputStreams;
import net.dongliu.commons.io.TeeInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Handle connect method.
 * Connect may proxy maybe: https(1.x, 2.0), webSocket, or even plain http1.x traffics
 *
 * @author Liu Dong
 */
public class ConnectProxyHandler implements Handler {
    private static Logger logger = LoggerFactory.getLogger(ConnectProxyHandler.class);
    private final Socket serverSocket;
    private final MessageListener messageListener;
    private final String host;
    private final int port;
    private final SSLContextManager sslContextManager;
    private final ExecutorService executor;
    private static Context context = Context.getInstance();

    public ConnectProxyHandler(Socket serverSocket, MessageListener messageListener, String host, int port,
                               SSLContextManager sslContextManager, ExecutorService executor) {
        this.serverSocket = serverSocket;
        this.messageListener = messageListener;
        this.host = host;
        this.port = port;
        this.sslContextManager = sslContextManager;
        this.executor = executor;
    }

    public void handle() throws IOException {

        // read first two byte to see if is ssl/tls connection
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TLSInputStream tlsIn = new TLSInputStream(new TeeInputStream(serverSocket.getInputStream(), bos));
        TLSPlaintextHeader tlsPlaintextHeader = tlsIn.readPlaintextHeader();

        boolean ssl;
        Socket fromSocket;
        Socket toSocket;
        if (tlsPlaintextHeader.isValidHandShake()) {
            //TODO: Java8 not support alpn now, it is hard to know if target server support http2 by tls. Wait java9
            HandShakeMessage<?> handShakeMessage = tlsIn.readHandShakeMessage();
            ClientHello clientHello = (ClientHello) handShakeMessage.getMessage();
            if (clientHello.alpnHas("h2")) {
                // connect to sever, and check sever protocol
            }

            Socket wrappedSocket = new WrappedSocket(serverSocket, bos.toByteArray());
            SSLContext serverSslContext = sslContextManager.createSSlContext(host);
            SSLSocketFactory sslSocketFactory = serverSslContext.getSocketFactory();
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(wrappedSocket, null, serverSocket.getPort(),
                    false);
            sslSocket.setUseClientMode(false);
            fromSocket = sslSocket;
            ssl = true;

            try {
                SSLSocket sslClientSocket = context.createSSLSocket(host, port);
                sslClientSocket.startHandshake();
                toSocket = sslClientSocket;
            } catch (IOException e) {
                logger.error("create ssl socket to {}:{} failed", host, port);
                logger.debug("create ssl socket to {}:{} failed", host, port, e);
                return;
            }
        } else {
            fromSocket = new WrappedSocket(serverSocket, bos.toByteArray());
            ssl = false;
            try {
                toSocket = context.createSocket(host, port);
            } catch (IOException e) {
                logger.error("create socket to {}:{} failed: {}", host, port, e.getMessage());
                return;
            }
        }

        try {
            handle(fromSocket, toSocket, ssl, host + ":" + port, messageListener);
        } catch (SSLHandshakeException e) {
            // something wrong with ssl
            logger.error("SSL connection error for {}:{}.", host, port, e);
        } finally {
            Closeables.closeQuietly(toSocket);
        }

    }

    private void handle(Socket fromSocket, Socket toSocket, Boolean ssl, String target,
                        MessageListener messageListener) throws IOException {
        HttpInputStream fromIn = new HttpInputStream(fromSocket.getInputStream());
        HttpOutputStream fromOut = new HttpOutputStream(fromSocket.getOutputStream());
        HttpInputStream toIn = new HttpInputStream(toSocket.getInputStream());
        HttpOutputStream toOut = new HttpOutputStream(toSocket.getOutputStream());
        fromIn.enableBuffered();
        toIn.enableBuffered();

        while (true) {
            boolean finish = handleOneRequest(fromIn, fromOut, fromSocket, toIn, toOut, toSocket,
                    ssl, target, messageListener);
            if (finish) {
                break;
            }
        }

    }

    private static final Set<String> methods = Sets.of("GET", "POST", "HEAD", "PUT", "TRACE", "DELETE", "PATCH",
            "OPTIONS");

    private boolean handleOneRequest(HttpInputStream fromIn, HttpOutputStream fromOut, Socket fromSocket,
                                     HttpInputStream toIn, HttpOutputStream toOut, Socket toSocket,
                                     boolean ssl, String target,
                                     MessageListener messageListener) throws IOException {
        // If is http traffics
        fromIn.mark(4096);
        String firstLine;
        try {
            firstLine = fromIn.readLine();
        } catch (SocketTimeoutException e) {
            logger.debug("read client request timeout");
            return true;
        }
        fromIn.reset();
        if (firstLine == null) {
            return true;
        }

        String method = Strings.before(firstLine, " ");
        if (!methods.contains(method)) {
            // not http request
            logger.debug("non-http traffic proxy via http tunnel");
            Utils.tunnel(fromIn, fromOut, fromSocket, toIn, toOut, toSocket, executor);
            return true;
        }

        @Nullable RequestHeaders requestHeaders = fromIn.readRequestHeaders();
        // client close connection
        if (requestHeaders == null) {
            logger.debug("Client close connection");
            return true;
        }
        RequestLine requestLine = requestHeaders.getRequestLine();
        logger.debug("Accept new request: {}", requestLine.raw());

        // expect-100
        if ("100-continue".equalsIgnoreCase(requestHeaders.getFirst("Expect"))) {
            fromOut.writeLine("HTTP/1.1 100 Continue\r\n");
            //TODO: filter expect-100 header?
        }

        String id = MessageIdGenerator.getInstance().nextId();
        String upgrade = requestHeaders.getFirst("Upgrade");
        String host = requestHeaders.getFirst("Host");
        if (host == null) {
            host = NetUtils.getHost(target);
        }
        int port = NetUtils.getPort(target);

        String url = getUrl(ssl, upgrade, host, port, requestLine.getPath());
        boolean shouldClose = requestHeaders.shouldClose();

        BodyStore requestBodyStore = readRequestBody(fromIn, requestHeaders, url);
        messageListener.onHttpRequest(id, host, url, requestHeaders, requestBodyStore);
        toOut.writeRequestHeaders(filterChunkedHeader(requestHeaders, requestBodyStore.size()));
        if (requestBodyStore.size() > 0) {
            InputStreams.copyTo(requestBodyStore.originInput(), toOut);
        }

        ResponseHeaders responseHeaders = toIn.readResponseHeaders();
        if (responseHeaders == null) {
            logger.debug("Target server  close connection");
            return true;
        }
        int code = responseHeaders.getStatusLine().getCode();

        BodyStore responseBodyStore = readResponseBody(responseHeaders, toIn, url, method);
        messageListener.onHttpResponse(id, responseHeaders, responseBodyStore);
        fromOut.writeResponseHeaders(filterChunkedHeader(responseHeaders, responseBodyStore.size()));
        if (responseBodyStore.size() > 0) {
            InputStreams.copyTo(responseBodyStore.originInput(), fromOut);
        }
        fromOut.flush();

        // check if has upgrade
        if ("websocket".equals(upgrade) && code == 101) {
            // upgrade to websocket
            logger.debug("{} upgrade to websocket", url);
            String version = Strings.nullToEmpty(requestHeaders.getFirst("Sec-WebSocket-Version"));
            //TODO: server may not support the version. in this case server will send supported versions, client should
            WebSocketHandler webSocketHandler = new WebSocketHandler(executor);
            webSocketHandler.handle(fromIn, fromOut, fromSocket, toIn, toOut, toSocket,
                    host, url, messageListener);
            shouldClose = true;
        } else if ("h2c".equals(upgrade) && code == 101) {
            // http2 from http1 upgrade
            logger.info("{} upgrade to http2", url);
            String http2Settings = requestHeaders.getFirst("HTTP2-Settings");
            Http2Handler handler = new Http2Handler(executor);
            handler.handle(fromIn, fromOut, fromSocket, toIn, toOut, toSocket,
                    ssl, target, messageListener);
            shouldClose = true;
        }

        return shouldClose;
    }

    private static String getUrl(boolean ssl, @Nullable String upgrade, String host, int port, String path) {
        String protocol;
        if ("websocket".equals(upgrade)) {
            protocol = ssl ? "wss" : "ws";
        } else {
            protocol = ssl ? "https" : "http";
        }
        StringBuilder sb = new StringBuilder(protocol).append("://").append(host);
        if (!(port == 443 && ssl || port == 80 && !ssl)) {
            sb.append(":").append(port);
        }
        sb.append(path);
        return sb.toString();
    }

    private BodyStore readRequestBody(HttpInputStream input, RequestHeaders requestHeaders, String url)
            throws IOException {
        try (BodyStore bodyStore = BodyStore.create(requestHeaders.contentType(),
                requestHeaders.contentEncoding(), url)) {
            InputStream requestBody;
            if (requestHeaders.chunked()) {
                requestBody = input.getChunkedBody();
            } else if (requestHeaders.contentLen() >= 0) {
                requestBody = input.getFixLenBody(requestHeaders.contentLen());
            } else if (!requestHeaders.hasBody()) {
                requestBody = null;
            } else {
                requestBody = null;
            }

            if (requestBody != null) {
                InputStreams.copyTo(requestBody, bodyStore);
            }
            return bodyStore;
        }
    }

    private BodyStore readResponseBody(ResponseHeaders headers, HttpInputStream dstIn, String url, String method)
            throws IOException {
        try (BodyStore bodyStore = BodyStore.create(headers.contentType(), headers.contentEncoding(), url)) {
            InputStream responseBody;
            if (headers.chunked()) {
                responseBody = dstIn.getChunkedBody();
            } else if (headers.contentLen() >= 0) {
                responseBody = dstIn.getFixLenBody(headers.contentLen());
            } else if (!headers.hasBody() || method.equals("HEAD")) {
                responseBody = null;
            } else {
                responseBody = null;
            }
            if (responseBody != null) {
                InputStreams.copyTo(responseBody, bodyStore);
            }
            return bodyStore;
        }
    }

    private ResponseHeaders filterChunkedHeader(ResponseHeaders responseHeaders, long contentLen) {
        List<Header> headers = responseHeaders.getHeaders();
        List<Header> newHeaders = new ArrayList<>(headers.size());
        for (Header header : headers) {
            if (header.getName().equalsIgnoreCase("Transfer-Encoding")) {
                newHeaders.add(new Header("Content-Length", String.valueOf(contentLen)));
            } else {
                newHeaders.add(header);
            }
        }
        return new ResponseHeaders(responseHeaders.getStatusLine(), newHeaders);
    }

    private RequestHeaders filterChunkedHeader(RequestHeaders requestHeaders, long contentLen) {
        List<Header> headers = requestHeaders.getHeaders();
        List<Header> newHeaders = new ArrayList<>(headers.size());
        for (Header header : headers) {
            if (header.getName().equalsIgnoreCase("Transfer-Encoding")) {
                newHeaders.add(new Header("Content-Length", String.valueOf(contentLen)));
            } else {
                newHeaders.add(header);
            }
        }
        return new RequestHeaders(requestHeaders.getRequestLine(), newHeaders);
    }
}
