package net.dongliu.proxy.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameEncoder;
import net.dongliu.proxy.MessageListener;
import net.dongliu.proxy.data.*;
import net.dongliu.proxy.store.Body;
import net.dongliu.proxy.utils.NetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Intercept http message. This interceptor is set on connection to target server.
 */
public class HttpInterceptor extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(HttpInterceptor.class);

    // the current http request/response
    private Http1Message httpMessage;
    private boolean upgradeWebSocket;

    private final boolean ssl;
    private final NetAddress address;
    private final MessageListener messageListener;
    private final ChannelPipeline clientPipeline;

    public HttpInterceptor(boolean ssl, NetAddress address, MessageListener messageListener,
                           ChannelPipeline clientPipeline) {
        this.ssl = ssl;
        this.address = address;
        this.messageListener = messageListener;
        this.clientPipeline = clientPipeline;
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
            Http1RequestHeaders requestHeader = convertHeader(request);
            Body body = requestHeader.createBody();
            httpMessage = new Http1Message(ssl ? "https" : "http", address, requestHeader, body);
            messageListener.onMessage(httpMessage);

            // check connection upgrade
            HttpHeaders headers = request.headers();
            if ("Upgrade".equalsIgnoreCase(headers.get("Connection"))) {
                if ("webSocket".equalsIgnoreCase(headers.get("Upgrade"))) {
                    // web-socket
                    upgradeWebSocket = true;
                }
                // TODO: upgrade h2c
            }
        }

        if (msg instanceof HttpContent) {
            Http1Message message = this.httpMessage;
            ByteBuf content = ((HttpContent) msg).content();
            if (content.readableBytes() > 0) {
                message.requestBody().append(content.nioBuffer());
            }
            if (msg instanceof LastHttpContent) {
                message.requestBody().finish();

            }
        }

        ctx.write(msg, promise);
    }

    private static Http1RequestHeaders convertHeader(HttpRequest request) {
        RequestLine requestLine = new RequestLine(request.method().name(), request.uri(),
                request.protocolVersion().text());
        List<Header> headers = new ArrayList<>();
        request.headers().iteratorAsString().forEachRemaining(h -> headers.add(new Header(h.getKey(), h.getValue())));
        return new Http1RequestHeaders(requestLine, headers);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof HttpObject)) {
            logger.error("not http message: {}", msg.getClass().getName());
            ctx.fireChannelRead(msg);
            return;
        }

        Http1Message message = this.httpMessage;
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            Http1ResponseHeaders responseHeader = convertHeader(response);
            message.responseHeader(responseHeader);
            Body body = responseHeader.createBody();
            message.responseBody(body);
        }

        if (msg instanceof HttpContent) {
            ByteBuf content = ((HttpContent) msg).content();
            if (content.readableBytes() > 0) {
                message.responseBody().append(content.nioBuffer());
            }
            if (msg instanceof LastHttpContent) {
                message.responseBody().finish();
                this.httpMessage = null;

                // connection upgrade
                //TODO: should check response header?
                if (upgradeWebSocket) {
                    ctx.fireChannelRead(msg);
                    String url = message.url().replaceAll("^http", "ws");
                    ctx.pipeline().replace("http-interceptor", "ws-interceptor",
                            new WebSocketInterceptor(address.getHost(), url, messageListener));
                    ctx.pipeline().remove(HttpClientCodec.class);
                    WebSocketFrameDecoder frameDecoder = new WebSocket13FrameDecoder(false, true, 65536, false);
                    WebSocketFrameEncoder frameEncoder = new WebSocket13FrameEncoder(true);
                    ctx.pipeline().addBefore("ws-interceptor", "ws-decoder", frameDecoder);
                    ctx.pipeline().addBefore("ws-interceptor", "ws-encoder", frameEncoder);


                    clientPipeline.remove(HttpServerCodec.class);
                    WebSocketFrameDecoder clientFrameDecoder = new WebSocket13FrameDecoder(true, true, 65536, false);
                    WebSocketFrameEncoder clientFrameEncoder = new WebSocket13FrameEncoder(false);
                    clientPipeline.addBefore("replay-handler", "ws-decoder", clientFrameDecoder);
                    clientPipeline.addBefore("replay-handler", "ws-encoder", clientFrameEncoder);

                    // do not fire the last empty content, httpCodec already removed
                    // the last http content is always empty?
                    return;
                }
            }
        }

        ctx.fireChannelRead(msg);
    }

    private static Http1ResponseHeaders convertHeader(HttpResponse response) {
        StatusLine statusLine = new StatusLine(response.protocolVersion().text(), response.status().code(),
                response.status().reasonPhrase());
        List<Header> headers = new ArrayList<>();
        response.headers().iteratorAsString().forEachRemaining(h -> headers.add(new Header(h.getKey(), h.getValue())));
        return new Http1ResponseHeaders(statusLine, headers);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause.getMessage() == null || !cause.getMessage().contains("Connection reset by peer")) {
            logger.error("", cause);
        }
        super.exceptionCaught(ctx, cause);
    }

}
