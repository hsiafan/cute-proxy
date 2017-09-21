package net.dongliu.byproxy.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v4.DefaultSocks4CommandResponse;
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest;
import io.netty.handler.codec.socksx.v4.Socks4CommandStatus;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.socksx.v4.Socks4CommandStatus.REJECTED_OR_FAILED;
import static io.netty.handler.codec.socksx.v5.Socks5CommandStatus.FAILURE;

@ChannelHandler.Sharable
public class SocksProxyConnectHandler extends SimpleChannelInboundHandler<SocksMessage> {
    private static final Logger logger = LoggerFactory.getLogger(SocksProxyConnectHandler.class);

    private final Bootstrap bootstrap = new Bootstrap();

    @Override
    public void channelRead0(ChannelHandlerContext ctx, SocksMessage message) throws Exception {
        if (message instanceof Socks4CommandRequest) {
            Socks4CommandRequest request = (Socks4CommandRequest) message;
            Promise<Channel> promise = ctx.executor().newPromise();
            promise.addListener((FutureListener<Channel>) future -> {
                Channel outboundChannel = future.getNow();
                if (future.isSuccess()) {
                    ChannelFuture responseFuture = ctx.channel().writeAndFlush(
                            new DefaultSocks4CommandResponse(Socks4CommandStatus.SUCCESS));

                    responseFuture.addListener((ChannelFutureListener) channelFuture -> {
                        ctx.pipeline().remove(SocksProxyConnectHandler.this);
                        outboundChannel.pipeline().addLast(new TunnelProxyHandler(ctx.channel()));
                        ctx.pipeline().addLast(new TunnelProxyHandler(outboundChannel));
                    });
                } else {
                    ctx.channel().writeAndFlush(new DefaultSocks4CommandResponse(REJECTED_OR_FAILED));
                    NettyUtils.closeOnFlush(ctx.channel());
                }
            });

            Channel inboundChannel = ctx.channel();
            bootstrap.group(inboundChannel.eventLoop())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new DirectClientHandler(promise));

            bootstrap.connect(request.dstAddr(), request.dstPort()).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    // Connection established use handler provided results
                } else {
                    // Close the connection if the connection attempt has failed.
                    ctx.channel().writeAndFlush(new DefaultSocks4CommandResponse(REJECTED_OR_FAILED));
                    NettyUtils.closeOnFlush(ctx.channel());
                }
            });
        } else if (message instanceof Socks5CommandRequest) {
            Socks5CommandRequest request = (Socks5CommandRequest) message;
            Promise<Channel> promise = ctx.executor().newPromise();
            promise.addListener((FutureListener<Channel>) future -> {
                Channel outboundChannel = future.getNow();
                if (future.isSuccess()) {
                    ChannelFuture responseFuture = ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                            Socks5CommandStatus.SUCCESS,
                            request.dstAddrType(),
                            request.dstAddr(),
                            request.dstPort()));

                    responseFuture.addListener((ChannelFutureListener) channelFuture -> {
                        ctx.pipeline().remove(SocksProxyConnectHandler.this);
                        outboundChannel.pipeline().addLast(new TunnelProxyHandler(ctx.channel()));
                        ctx.pipeline().addLast(new TunnelProxyHandler(outboundChannel));
                    });
                } else {
                    ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(FAILURE, request.dstAddrType()));
                    NettyUtils.closeOnFlush(ctx.channel());
                }
            });

            Channel inboundChannel = ctx.channel();
            bootstrap.group(inboundChannel.eventLoop())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new DirectClientHandler(promise));

            bootstrap.connect(request.dstAddr(), request.dstPort()).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    // Connection established use handler provided results
                } else {
                    // Close the connection if the connection attempt has failed.
                    ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(FAILURE, request.dstAddrType()));
                    NettyUtils.closeOnFlush(ctx.channel());
                }
            });
        } else {
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
        logger.error("", e);
        NettyUtils.closeOnFlush(ctx.channel());
    }
}