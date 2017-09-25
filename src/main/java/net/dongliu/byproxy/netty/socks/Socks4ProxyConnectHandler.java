package net.dongliu.byproxy.netty.socks;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v4.DefaultSocks4CommandResponse;
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest;
import io.netty.handler.codec.socksx.v4.Socks4CommandStatus;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import net.dongliu.byproxy.netty.ChannelActiveAwareHandler;
import net.dongliu.byproxy.netty.NettyUtils;
import net.dongliu.byproxy.netty.TunnelProxyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.socksx.v4.Socks4CommandStatus.REJECTED_OR_FAILED;

@ChannelHandler.Sharable
public class Socks4ProxyConnectHandler extends SimpleChannelInboundHandler<Socks4CommandRequest> {
    private static final Logger logger = LoggerFactory.getLogger(Socks4ProxyConnectHandler.class);

    private final Bootstrap bootstrap = new Bootstrap();

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Socks4CommandRequest request) throws Exception {
        Promise<Channel> promise = ctx.executor().newPromise();

        Channel inboundChannel = ctx.channel();
        bootstrap.group(inboundChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelActiveAwareHandler(promise));

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
            ChannelFuture responseFuture = ctx.channel().writeAndFlush(
                    new DefaultSocks4CommandResponse(Socks4CommandStatus.SUCCESS));

            responseFuture.addListener((ChannelFutureListener) channelFuture -> {
                ctx.pipeline().remove(Socks4ProxyConnectHandler.this);
                outboundChannel.pipeline().addLast(new TunnelProxyHandler(ctx.channel()));
                ctx.pipeline().addLast(new TunnelProxyHandler(outboundChannel));
            });
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
        logger.error("", e);
        NettyUtils.closeOnFlush(ctx.channel());
    }
}