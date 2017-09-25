package net.dongliu.byproxy.netty.tcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v4.DefaultSocks4CommandResponse;
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest;
import io.netty.handler.codec.socksx.v4.Socks4CommandStatus;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.ChannelActiveAwareHandler;
import net.dongliu.byproxy.netty.NettyUtils;
import net.dongliu.byproxy.utils.NetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import static io.netty.handler.codec.socksx.v4.Socks4CommandStatus.REJECTED_OR_FAILED;

public class Socks4ProxyConnectHandler extends SimpleChannelInboundHandler<Socks4CommandRequest>
        implements TcpProxyHandlerTraits {
    private static final Logger logger = LoggerFactory.getLogger(Socks4ProxyConnectHandler.class);

    private final Bootstrap bootstrap = new Bootstrap();

    @Nullable
    private final MessageListener messageListener;

    public Socks4ProxyConnectHandler(@Nullable MessageListener messageListener) {
        this.messageListener = messageListener;
    }

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
                NetAddress address = new NetAddress(request.dstAddr(), request.dstPort());
                initTcpProxyHandlers(ctx, address, outboundChannel, messageListener);
            });
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
        logger.error("", e);
        NettyUtils.closeOnFlush(ctx.channel());
    }
}