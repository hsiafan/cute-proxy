package net.dongliu.byproxy.netty.interceptor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameEncoder;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.store.Body;
import net.dongliu.byproxy.struct.*;
import net.dongliu.byproxy.utils.NetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HttpInterceptor extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(HttpInterceptor.class);

    // the current http request/response
    private HttpRoundTripMessage httpMessage;
    private boolean upgradeWebSocket;

    private final boolean ssl;
    private final NetAddress address;
    private final MessageListener messageListener;

    private final Runnable onUpgrade;

    public HttpInterceptor(boolean ssl, NetAddress address, MessageListener messageListener, Runnable onUpgrade) {
        this.ssl = ssl;
        this.address = address;
        this.messageListener = messageListener;
        this.onUpgrade = onUpgrade;
    }

    public HttpInterceptor(boolean ssl, NetAddress address, MessageListener messageListener) {
        this(ssl, address, messageListener, () -> {
        });
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (!(msg instanceof HttpObject)) {
            logger.debug("not http message: {}", msg.getClass().getName());
            ctx.write(msg, promise);
            return;
        }

        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            RequestHeaders requestHeader = convertHeader(request);
            Body body = Body.create(requestHeader.contentType(), requestHeader.contentEncoding());
            String host = address.getHost();
            String url = joinUrl(request.uri());
            httpMessage = new HttpRoundTripMessage(host, url, requestHeader, body);
            messageListener.onHttpRequest(httpMessage);

            // check connection upgrade
            HttpHeaders headers = request.headers();
            if ("Upgrade".equalsIgnoreCase(headers.get("Connection"))) {
                if ("webSocket".equalsIgnoreCase(headers.get("Upgrade"))) {
                    // web-socket
                    upgradeWebSocket = true;
                }
                // h2c
            }
        }

        if (msg instanceof HttpContent) {
            HttpRoundTripMessage message = this.httpMessage;
            ByteBuf content = ((HttpContent) msg).content();
            if (content.readableBytes() > 0) {
                message.getRequestBody().append(content.nioBuffer());
            }
            if (msg instanceof LastHttpContent) {
                message.getRequestBody().finish();

            }
        }

        ctx.write(msg, promise);
    }

    private static RequestHeaders convertHeader(HttpRequest request) {
        RequestLine requestLine = new RequestLine(request.method().name(), request.uri(),
                request.protocolVersion().text());
        List<Header> headers = new ArrayList<>();
        request.headers().iteratorAsString().forEachRemaining(h -> headers.add(new Header(h.getKey(), h.getValue())));
        return new RequestHeaders(requestLine, headers);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof HttpObject)) {
            logger.error("not http message: {}", msg.getClass().getName());
            ctx.fireChannelRead(msg);
            return;
        }

        HttpRoundTripMessage message = this.httpMessage;
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            ResponseHeaders responseHeader = convertHeader(response);
            message.setResponseHeader(responseHeader);
            Body body = Body.create(responseHeader.contentType(), responseHeader.contentEncoding());
            message.setResponseBody(body);
        }

        if (msg instanceof HttpContent) {
            ByteBuf content = ((HttpContent) msg).content();
            if (content.readableBytes() > 0) {
                message.getResponseBody().append(content.nioBuffer());
            }
            if (msg instanceof LastHttpContent) {
                message.getResponseBody().finish();
                this.httpMessage = null;

                // connection upgrade
                //TODO: should check response header?
                if (upgradeWebSocket) {
                    ctx.fireChannelRead(msg);
                    String url = message.getUrl().replaceAll("^http", "ws");
                    ctx.pipeline().replace("http-interceptor", "ws-interceptor",
                            new WebSocketInterceptor(address.getHost(), url, messageListener));
                    ctx.pipeline().remove(HttpClientCodec.class);
                    WebSocketFrameDecoder frameDecoder = new WebSocket13FrameDecoder(false, true, 65536, false);
                    WebSocketFrameEncoder frameEncoder = new WebSocket13FrameEncoder(true);
                    ctx.pipeline().addBefore("ws-interceptor", "ws-decoder", frameDecoder);
                    ctx.pipeline().addBefore("ws-interceptor", "ws-encoder", frameEncoder);
                    onUpgrade.run();

                    // do not fire the last empty content, httpCodec already removed
                    // the last http content is always empty?
                    return;
                }
            }
        }

        ctx.fireChannelRead(msg);
    }

    private static ResponseHeaders convertHeader(HttpResponse response) {
        StatusLine statusLine = new StatusLine(response.protocolVersion().text(), response.status().code(),
                response.status().reasonPhrase());
        List<Header> headers = new ArrayList<>();
        response.headers().iteratorAsString().forEachRemaining(h -> headers.add(new Header(h.getKey(), h.getValue())));
        return new ResponseHeaders(statusLine, headers);
    }

    private String joinUrl(String path) {
        StringBuilder sb = new StringBuilder();
        if (ssl) {
            sb.append("https");
        } else {
            sb.append("http");
        }
        sb.append("://").append(address.getHost());
        if (!(ssl && address.getPort() == 443 || !ssl && address.getPort() == 80)) {
            sb.append(":").append(address.getPort());
        }
        sb.append(path);
        return sb.toString();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause.getMessage() == null || !cause.getMessage().contains("Connection reset by peer")) {
            logger.error("", cause);
        }
        super.exceptionCaught(ctx, cause);
    }

}
