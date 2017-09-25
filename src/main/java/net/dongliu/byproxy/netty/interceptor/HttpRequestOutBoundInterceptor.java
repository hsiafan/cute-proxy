package net.dongliu.byproxy.netty.interceptor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequestOutBoundInterceptor extends ChannelOutboundHandlerAdapter
        implements HttpRequestInterceptorTraits {
    private static final Logger logger = LoggerFactory.getLogger(HttpRequestOutBoundInterceptor.class);

    private final HttpInterceptorContext interceptorContext;

    public HttpRequestOutBoundInterceptor(HttpInterceptorContext interceptorContext) {
        this.interceptorContext = interceptorContext;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof HttpObject)) {
            logger.debug("not http message: {}", msg.getClass().getSimpleName());
            ctx.write(msg, promise);
            return;
        }

        handleHttpRequest(ctx, msg);
        ctx.write(msg, promise);
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
