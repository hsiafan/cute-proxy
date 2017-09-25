package net.dongliu.byproxy.netty.interceptor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import net.dongliu.byproxy.store.HttpBody;
import net.dongliu.byproxy.struct.Header;
import net.dongliu.byproxy.struct.HttpResponseHeader;
import net.dongliu.byproxy.struct.HttpRoundTripMessage;
import net.dongliu.byproxy.struct.StatusLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class HttpResponseInboundInterceptor extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(HttpResponseInboundInterceptor.class);

    private final HttpInterceptorContext interceptorContext;
    private final boolean pass;

    public HttpResponseInboundInterceptor(HttpInterceptorContext interceptorContext, boolean pass) {
        this.interceptorContext = interceptorContext;
        this.pass = pass;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof HttpObject)) {
            logger.error("not http message: {}", msg.getClass().getSimpleName());
            handleMessage(ctx, msg);
            return;
        }

        if (interceptorContext.isDiscard()) {
            handleMessage(ctx, msg);
            return;
        }

        HttpRoundTripMessage httpMessage = interceptorContext.getHttpMessage();


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
                        .thenRunAsync(() -> interceptorContext.setHttpMessage(null), ctx.executor());
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
