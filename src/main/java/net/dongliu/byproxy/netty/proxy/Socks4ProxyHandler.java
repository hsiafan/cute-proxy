package net.dongliu.byproxy.netty.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.socksx.v4.DefaultSocks4CommandResponse;
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.NettyUtils;
import net.dongliu.byproxy.ssl.SSLContextManager;
import net.dongliu.byproxy.utils.NetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.function.Supplier;

import static io.netty.handler.codec.socksx.v4.Socks4CommandStatus.REJECTED_OR_FAILED;
import static io.netty.handler.codec.socksx.v4.Socks4CommandStatus.SUCCESS;

public class Socks4ProxyHandler extends SimpleChannelInboundHandler<Socks4CommandRequest>
        implements TunnelProxyHandlerTraits {
    private static final Logger logger = LoggerFactory.getLogger(Socks4ProxyHandler.class);

    @Nullable
    private final MessageListener messageListener;

    @Nullable
    private final SSLContextManager sslContextManager;
    @Nullable
    private final Supplier<ProxyHandler> proxyHandlerSupplier;

    public Socks4ProxyHandler(@Nullable MessageListener messageListener,
                              @Nullable SSLContextManager sslContextManager,
                              @Nullable Supplier<ProxyHandler> proxyHandlerSupplier) {
        this.messageListener = messageListener;
        this.sslContextManager = sslContextManager;
        this.proxyHandlerSupplier = proxyHandlerSupplier;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Socks4CommandRequest request) {
        Promise<Channel> promise = ctx.executor().newPromise();
        Bootstrap bootstrap = initBootStrap(promise, ctx.channel().eventLoop());

        bootstrap.connect(request.dstAddr(), request.dstPort()).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                ctx.channel().writeAndFlush(new DefaultSocks4CommandResponse(REJECTED_OR_FAILED));
                NettyUtils.closeOnFlush(ctx.channel());
            }
        });

        promise.addListener((FutureListener<Channel>) future -> {
            Channel outboundChannel = future.getNow();
            if (!future.isSuccess()) {
                ctx.channel().writeAndFlush(new DefaultSocks4CommandResponse(REJECTED_OR_FAILED));
                NettyUtils.closeOnFlush(ctx.channel());
                return;
            }
            ChannelFuture responseFuture = ctx.channel().writeAndFlush(new DefaultSocks4CommandResponse(SUCCESS));

            responseFuture.addListener((ChannelFutureListener) channelFuture -> {
                ctx.pipeline().remove(Socks4ProxyHandler.this);
                NetAddress address = new NetAddress(request.dstAddr(), request.dstPort());
                initTcpProxyHandlers(ctx, address, outboundChannel);
            });
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
        logger.error("", e);
        NettyUtils.closeOnFlush(ctx.channel());
    }

    @Nullable
    @Override
    public MessageListener messageListener() {
        return messageListener;
    }

    @Nullable
    @Override
    public SSLContextManager sslContextManager() {
        return sslContextManager;
    }

    @Nullable
    @Override
    public Supplier<ProxyHandler> proxyHandlerSupplier() {
        return proxyHandlerSupplier;
    }
}