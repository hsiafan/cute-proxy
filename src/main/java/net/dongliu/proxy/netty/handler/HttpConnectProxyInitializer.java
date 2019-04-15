package net.dongliu.proxy.netty.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import net.dongliu.commons.net.HostPort;
import net.dongliu.proxy.MessageListener;
import net.dongliu.proxy.netty.NettyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_GATEWAY;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Handle http connect tunnel proxy request
 */
public class HttpConnectProxyInitializer extends TunnelProxyHandler<HttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(HttpConnectProxyInitializer.class);
    private boolean removed;

    public HttpConnectProxyInitializer(MessageListener messageListener, ServerSSLContextManager sslContextManager,
                                       Supplier<ProxyHandler> proxyHandlerSupplier) {
        super(messageListener, sslContextManager, proxyHandlerSupplier);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpRequest request) {
        Promise<Channel> promise = ctx.executor().newPromise();
        Bootstrap bootstrap = initBootStrap(promise, ctx.channel().eventLoop());

        var address = HostPort.parse(request.uri());
        bootstrap.connect(address.host(), address.ensurePort()).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                ctx.channel().writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, BAD_GATEWAY));
                NettyUtils.closeOnFlush(ctx.channel());
            }
        });

        promise.addListener((FutureListener<Channel>) future -> {
            if (!future.isSuccess()) {
                ctx.channel().writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, BAD_GATEWAY));
                NettyUtils.closeOnFlush(ctx.channel());
                return;
            }

            Channel outboundChannel = future.getNow();
            ChannelFuture responseFuture = ctx.channel().writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, OK));
            responseFuture.addListener((ChannelFutureListener) channelFuture -> {
                logger.debug("try to remove HttpConnectProxyInitializer, pipeline: {}", ctx.pipeline());
                //FIXME: throw NoSuchElementException
                if (removed) {
                    logger.warn("HttpConnectProxyInitializer removed by others?");
                    ctx.close();
                    return;
                }
                ctx.pipeline().remove(HttpConnectProxyInitializer.this);
                ctx.pipeline().remove(HttpServerCodec.class);
                initTcpProxyHandlers(ctx, address, outboundChannel);
            });
        });
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
        removed = true;
        logger.debug("HttpConnectProxyInitializer removed, pipeline: {}", ctx.pipeline());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        logger.error("", e);
        NettyUtils.closeOnFlush(ctx.channel());
    }
}