package net.dongliu.byproxy.netty.interceptor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.*;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.store.HttpBody;
import net.dongliu.byproxy.struct.*;
import net.dongliu.byproxy.utils.NetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Interceptor the http requests sent to proxy server
 */
public class HttpMessageInterceptor extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(HttpMessageInterceptor.class);
    private HttpRoundTripMessage httpMessage;
    private boolean discard = true;

    private String scheme;
    private NetAddress address;
    private MessageListener messageListener;

    public HttpMessageInterceptor(String scheme, NetAddress address, MessageListener messageListener) {
        this.scheme = scheme;
        this.address = address;
        this.messageListener = Objects.requireNonNull(messageListener);
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof HttpObject)) {
            logger.debug("not http message: {}", msg.getClass().getSimpleName());
            ctx.write(msg, promise);
            return;
        }

        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            HttpRequestHeader requestHeader = convertHeader(request);
            HttpBody httpBody = HttpBody.create(requestHeader.contentType(), requestHeader.contentEncoding());
            if (httpMessage != null) {
                // consume too slow, discard
                discard = true;
                logger.warn("save too slow, discard http request: {}", request.uri());
            } else {
                String url;
                if (address.getPort() == 80) {
                    url = scheme + "://" + address.getHost() + request.uri();
                } else {
                    url = scheme + "://" + address.toString() + request.uri();
                }
                httpMessage = new HttpRoundTripMessage(address.getHost(), url, requestHeader, httpBody);
                discard = false;
            }
        }

        if (msg instanceof HttpContent) {
            if (!discard) {
                HttpBodySaver saver = HttpBodySaver.getInstance();
                ByteBuf content = ((HttpContent) msg).content();
                if (content.readableBytes() > 0) {
                    content.retain();
                    saver.save(httpMessage.getRequestBody(), content.nioBuffer())
                            .thenRunAsync(content::release, ctx.executor());
                }
                if (msg instanceof LastHttpContent) {
                    saver.finish(httpMessage.getRequestBody()).thenRunAsync(() -> {
                        messageListener.onHttpRequest(httpMessage);
                        logger.debug("send request:{}", httpMessage.getUrl());
                    });
                }
            }
        }
        ctx.write(msg, promise);
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof HttpObject)) {
            logger.error("not http message: {}", msg.getClass().getSimpleName());
            ctx.fireChannelRead(msg);
            return;
        }

        if (discard) {
            ctx.fireChannelRead(msg);
            return;
        }

        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            HttpResponseHeader responseHeader = convertHeader(response);
            httpMessage.setResponseHeader(responseHeader);
            HttpBody httpBody = HttpBody.create(responseHeader.contentType(), responseHeader.contentEncoding());
            httpMessage.setResponseBody(httpBody);
        }

        if (msg instanceof HttpContent) {
            HttpBodySaver saver = HttpBodySaver.getInstance();
            ByteBuf content = ((HttpContent) msg).content();
            if (content.readableBytes() > 0) {
                content.retain();
                saver.save(httpMessage.getResponseBody(), content.nioBuffer())
                        .thenRunAsync(content::release, ctx.executor());
            }
            if (msg instanceof LastHttpContent) {
                saver.finish(httpMessage.getResponseBody())
                        .thenRunAsync(() -> logger.debug("response finished for url: {}", httpMessage.getUrl()))
                        .thenRunAsync(() -> httpMessage = null, ctx.executor());
            }
        }

        ctx.fireChannelRead(msg);
    }

    private static HttpRequestHeader convertHeader(HttpRequest request) {
        RequestLine requestLine = new RequestLine(request.method().name(), request.uri(),
                request.protocolVersion().text());
        List<Header> headers = new ArrayList<>();
        request.headers().iteratorAsString().forEachRemaining(h -> headers.add(new Header(h.getKey(), h.getValue())));
        return new HttpRequestHeader(requestLine, headers);
    }

    private static HttpResponseHeader convertHeader(HttpResponse response) {
        StatusLine statusLine = new StatusLine(response.protocolVersion().text(), response.status().code(),
                response.status().reasonPhrase());
        List<Header> headers = new ArrayList<>();
        response.headers().iteratorAsString().forEachRemaining(h -> headers.add(new Header(h.getKey(), h.getValue())));
        return new HttpResponseHeader(statusLine, headers);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("", cause);
        super.exceptionCaught(ctx, cause);
    }
}
