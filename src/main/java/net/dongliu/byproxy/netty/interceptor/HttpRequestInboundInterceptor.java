package net.dongliu.byproxy.netty.interceptor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.store.HttpBody;
import net.dongliu.byproxy.struct.Header;
import net.dongliu.byproxy.struct.HttpRequestHeader;
import net.dongliu.byproxy.struct.HttpRoundTripMessage;
import net.dongliu.byproxy.struct.RequestLine;
import net.dongliu.byproxy.utils.NetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class HttpRequestInboundInterceptor extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(HttpRequestInboundInterceptor.class);

    private final HttpInterceptorContext interceptorContext;
    private final boolean pass;

    public HttpRequestInboundInterceptor(HttpInterceptorContext interceptorContext, boolean pass) {
        this.interceptorContext = interceptorContext;
        this.pass = pass;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof HttpObject)) {
            logger.debug("not http message: {}", msg.getClass().getSimpleName());
            handleMessage(ctx, msg);
            return;
        }

        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            HttpRequestHeader requestHeader = convertHeader(request);
            HttpBody httpBody = HttpBody.create(requestHeader.contentType(), requestHeader.contentEncoding());
            if (interceptorContext.getHttpMessage() != null) {
                // consume too slow, discard
                interceptorContext.setDiscard(true);
                logger.warn("save too slow, discard http request: {}", request.uri());
            } else {
                String url;
                NetAddress address = interceptorContext.getAddress();
                String scheme = interceptorContext.isSsl() ? "https" : "http";
                if (address.getPort() == 80) {
                    url = scheme + "://" + address.getHost() + request.uri();
                } else {
                    url = scheme + "://" + address.toString() + request.uri();
                }
                interceptorContext.setHttpMessage(new HttpRoundTripMessage(address.getHost(), url, requestHeader,
                        httpBody));
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
        handleMessage(ctx, msg);
    }

    private void handleMessage(ChannelHandlerContext ctx, Object msg) {
        if (pass) {
            ctx.fireChannelRead(msg);
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    private static HttpRequestHeader convertHeader(HttpRequest request) {
        RequestLine requestLine = new RequestLine(request.method().name(), request.uri(),
                request.protocolVersion().text());
        List<Header> headers = new ArrayList<>();
        request.headers().iteratorAsString().forEachRemaining(h -> headers.add(new Header(h.getKey(), h.getValue())));
        return new HttpRequestHeader(requestLine, headers);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("", cause);
        super.exceptionCaught(ctx, cause);
    }
}
