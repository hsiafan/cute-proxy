package net.dongliu.proxy.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.*;
import net.dongliu.commons.net.HostPort;
import net.dongliu.proxy.MessageListener;
import net.dongliu.proxy.data.*;
import net.dongliu.proxy.store.Body;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static net.dongliu.proxy.netty.NettyUtils.causedByClientClose;

/**
 * Intercept http message. This interceptor is set on connection to target server.
 */
public class HttpInterceptor extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(HttpInterceptor.class);

    // the current http request/response
    private Http1Message httpMessage;

    private final boolean ssl;
    private final HostPort address;
    private final MessageListener messageListener;

    public HttpInterceptor(boolean ssl, HostPort address, MessageListener messageListener) {
        this.ssl = ssl;
        this.address = address;
        this.messageListener = messageListener;
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
            logger.debug("not http message: {}", msg.getClass().getName());
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
        if (causedByClientClose(cause)) {
            logger.warn("client closed connection: {}", cause.getMessage());
        } else {
            logger.error("http error", cause);
        }
        ctx.close();
    }

}
