package net.dongliu.proxy.netty.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.socksx.v4.*;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import net.dongliu.commons.net.HostPort;
import net.dongliu.proxy.MessageListener;
import net.dongliu.proxy.netty.NettyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

import static io.netty.handler.codec.socksx.v4.Socks4CommandStatus.REJECTED_OR_FAILED;
import static io.netty.handler.codec.socksx.v4.Socks4CommandStatus.SUCCESS;

public class Socks4ProxyHandler extends TunnelProxyHandler<Socks4Message> {
    private static final Logger logger = LoggerFactory.getLogger(Socks4ProxyHandler.class);

    public Socks4ProxyHandler(MessageListener messageListener, ServerSSLContextManager sslContextManager,
                              Supplier<ProxyHandler> proxyHandlerSupplier) {
        super(messageListener, sslContextManager, proxyHandlerSupplier);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Socks4Message socksRequest) {
        Socks4CommandRequest command = (Socks4CommandRequest) socksRequest;
        if (command.type() != Socks4CommandType.CONNECT) {
            NettyUtils.closeOnFlush(ctx.channel());
            logger.error("unsupported socks4 command: {}", command.type());
            return;
        }
        Promise<Channel> promise = ctx.executor().newPromise();
        Bootstrap bootstrap = initBootStrap(promise, ctx.channel().eventLoop());

        bootstrap.connect(command.dstAddr(), command.dstPort()).addListener((ChannelFutureListener) future -> {
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
                ctx.pipeline().remove(Socks4ServerEncoder.class);
                ctx.pipeline().remove(Socks4ServerDecoder.class);
                var address = HostPort.of(command.dstAddr(), command.dstPort());
                initTcpProxyHandlers(ctx, address, outboundChannel);
            });
        });
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        logger.error("", e);
        NettyUtils.closeOnFlush(ctx.channel());
    }

}