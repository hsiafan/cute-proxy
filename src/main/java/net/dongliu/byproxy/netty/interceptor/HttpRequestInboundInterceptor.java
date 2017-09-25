package net.dongliu.byproxy.netty.interceptor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpObject;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequestInboundInterceptor extends ChannelInboundHandlerAdapter
        implements HttpRequestInterceptorTraits {
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
            releaseOrFireMessage(ctx, msg);
            return;
        }

        handleHttpRequest(ctx, msg);

        releaseOrFireMessage(ctx, msg);
    }

    private void releaseOrFireMessage(ChannelHandlerContext ctx, Object msg) {
        if (pass) {
            ctx.fireChannelRead(msg);
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("", cause);
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public HttpInterceptorContext interceptorContext() {
        return interceptorContext;
    }
}
