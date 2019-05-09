package net.dongliu.proxy.netty.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import net.dongliu.commons.net.HostPort;
import net.dongliu.proxy.MessageListener;
import net.dongliu.proxy.netty.NettyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

import static io.netty.handler.codec.socksx.v5.Socks5CommandStatus.FAILURE;

public class Socks5ProxyHandler extends TunnelProxyHandler<Socks5Message> {
    private static final Logger logger = LoggerFactory.getLogger(Socks5ProxyHandler.class);

    public Socks5ProxyHandler(MessageListener messageListener, ServerSSLContextManager sslContextManager,
                              Supplier<ProxyHandler> proxyHandlerSupplier) {
        super(messageListener, sslContextManager, proxyHandlerSupplier);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Socks5Message socksRequest) {
        if (socksRequest instanceof Socks5InitialRequest) {
            ctx.pipeline().addFirst("socks5-command-decoder", new Socks5CommandRequestDecoder());
            ctx.pipeline().remove("socks5-initial-decoder");
            ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
            return;
        }
        if (socksRequest instanceof Socks5PasswordAuthRequest) {
            ctx.pipeline().addFirst("socks5-command-decoder", new Socks5CommandRequestDecoder());
            ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
            return;
        }
        if (!(socksRequest instanceof Socks5CommandRequest)) {
            logger.error("unknown socks5 command: {}", socksRequest.getClass().getName());
            NettyUtils.closeOnFlush(ctx.channel());
            return;
        }
        Socks5CommandRequest command = (Socks5CommandRequest) socksRequest;
        if (command.type() != Socks5CommandType.CONNECT) {
            // only support connect command
            logger.error("unsupported socks5 command: {}", command.type());
            NettyUtils.closeOnFlush(ctx.channel());
            return;
        }
        Promise<Channel> promise = ctx.executor().newPromise();
        Bootstrap bootstrap = initBootStrap(promise, ctx.channel().eventLoop());

        bootstrap.connect(command.dstAddr(), command.dstPort()).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(FAILURE, command.dstAddrType()));
                NettyUtils.closeOnFlush(ctx.channel());
            }
        });

        promise.addListener((FutureListener<Channel>) future -> {
            if (!future.isSuccess()) {
                ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(FAILURE, command.dstAddrType()));
                NettyUtils.closeOnFlush(ctx.channel());
                return;
            }
            Channel outboundChannel = future.getNow();
            ChannelFuture responseFuture = ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                    Socks5CommandStatus.SUCCESS,
                    command.dstAddrType(),
                    command.dstAddr(),
                    command.dstPort()));

            responseFuture.addListener((ChannelFutureListener) f -> {
                ctx.pipeline().remove("socks5-server-encoder");
                ctx.pipeline().remove("socks5-command-decoder");
                ctx.pipeline().remove(Socks5ProxyHandler.this);
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