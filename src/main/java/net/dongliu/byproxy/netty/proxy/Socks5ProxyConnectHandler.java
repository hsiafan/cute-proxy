package net.dongliu.byproxy.netty.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.NettyUtils;
import net.dongliu.byproxy.ssl.SSLContextManager;
import net.dongliu.byproxy.utils.NetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import static io.netty.handler.codec.socksx.v5.Socks5CommandStatus.FAILURE;

public class Socks5ProxyConnectHandler extends SimpleChannelInboundHandler<Socks5CommandRequest>
        implements TcpProxyHandlerTraits {
    private static final Logger logger = LoggerFactory.getLogger(Socks5ProxyConnectHandler.class);

    @Nullable
    private final MessageListener messageListener;

    @Nullable
    private final SSLContextManager sslContextManager;

    public Socks5ProxyConnectHandler(@Nullable MessageListener messageListener,
                                     @Nullable SSLContextManager sslContextManager) {
        this.messageListener = messageListener;
        this.sslContextManager = sslContextManager;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Socks5CommandRequest request) throws Exception {
        Promise<Channel> promise = ctx.executor().newPromise();
        Bootstrap bootstrap = initBootStrap(promise, ctx.channel().eventLoop());

        bootstrap.connect(request.dstAddr(), request.dstPort()).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(FAILURE, request.dstAddrType()));
                NettyUtils.closeOnFlush(ctx.channel());
            }
        });

        promise.addListener((FutureListener<Channel>) future -> {
            if (!future.isSuccess()) {
                ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(FAILURE, request.dstAddrType()));
                NettyUtils.closeOnFlush(ctx.channel());
                return;
            }
            Channel outboundChannel = future.getNow();
            ChannelFuture responseFuture = ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                    Socks5CommandStatus.SUCCESS,
                    request.dstAddrType(),
                    request.dstAddr(),
                    request.dstPort()));

            responseFuture.addListener((ChannelFutureListener) f -> {
                ctx.pipeline().remove(Socks5ProxyConnectHandler.this);
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
}