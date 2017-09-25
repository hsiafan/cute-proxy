package net.dongliu.byproxy.netty.interceptor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.store.HttpBody;
import net.dongliu.byproxy.struct.Header;
import net.dongliu.byproxy.struct.HttpRequestHeader;
import net.dongliu.byproxy.struct.HttpRoundTripMessage;
import net.dongliu.byproxy.struct.RequestLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public interface HttpRequestInterceptorTraits {

    Logger logger = LoggerFactory.getLogger(HttpRequestInterceptorTraits.class);

    default void handleHttpRequest(ChannelHandlerContext ctx, Object msg) {
        HttpInterceptorContext interceptorContext = interceptorContext();
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            HttpRequestHeader requestHeader = convertHeader(request);
            HttpBody httpBody = HttpBody.create(requestHeader.contentType(), requestHeader.contentEncoding());
            if (interceptorContext.getHttpMessage() != null) {
                // consume too slow, discard
                interceptorContext.setDiscard(true);
                logger.warn("save too slow, discard http request: {}", request.uri());
            } else {
                String host = interceptorContext.getAddress().getHost();
                String url = interceptorContext.joinUrl(request.uri());
                interceptorContext.setHttpMessage(new HttpRoundTripMessage(host, url, requestHeader, httpBody));
                interceptorContext.setDiscard(false);
            }
        }

        if (msg instanceof HttpContent) {
            if (!interceptorContext.isDiscard()) {
                HttpRoundTripMessage httpMessage = interceptorContext.getHttpMessage();
                HttpBodySaver saver = HttpBodySaver.getInstance();
                ByteBuf content = ((HttpContent) msg).content();
                if (content.readableBytes() > 0) {
                    content.retain();
                    saver.save(httpMessage.getRequestBody(), content.nioBuffer())
                            .thenRunAsync(content::release, ctx.executor());
                }
                if (msg instanceof LastHttpContent) {
                    MessageListener messageListener = interceptorContext.getMessageListener();
                    saver.finish(httpMessage.getRequestBody()).thenRunAsync(() -> {
                        messageListener.onHttpRequest(httpMessage);
                        logger.debug("send request:{}", httpMessage.getUrl());
                    });
                }
            }
        }
    }

    static HttpRequestHeader convertHeader(HttpRequest request) {
        RequestLine requestLine = new RequestLine(request.method().name(), request.uri(),
                request.protocolVersion().text());
        List<Header> headers = new ArrayList<>();
        request.headers().iteratorAsString().forEachRemaining(h -> headers.add(new Header(h.getKey(), h.getValue())));
        return new HttpRequestHeader(requestLine, headers);
    }

    HttpInterceptorContext interceptorContext();
}
