package net.dongliu.byproxy.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import net.dongliu.byproxy.utils.NetAddress;
import net.dongliu.byproxy.utils.NetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_GATEWAY;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpProxyConnectHandler extends SimpleChannelInboundHandler<HttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(HttpProxyConnectHandler.class);

    private final Bootstrap bootstrap = new Bootstrap();

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
        Promise<Channel> promise = ctx.executor().newPromise();
        promise.addListener((FutureListener<Channel>) future -> {
            if (!future.isSuccess()) {
                ctx.channel().writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, BAD_GATEWAY));
                NettyUtils.closeOnFlush(ctx.channel());
                return;
            }

            Channel outboundChannel = future.getNow();
            ChannelFuture responseFuture = ctx.channel().writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, OK));
            responseFuture.addListener((ChannelFutureListener) channelFuture -> {
                ctx.pipeline().remove(HttpProxyConnectHandler.this);
                ctx.pipeline().remove(HttpServerCodec.class);
                outboundChannel.pipeline().addLast(new TunnelProxyHandler(ctx.channel()));
                ctx.pipeline().addLast(new TunnelProxyHandler(outboundChannel));
            });
        });

        Channel inboundChannel = ctx.channel();
        bootstrap.group(inboundChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new DirectClientHandler(promise));

        NetAddress address = NetUtils.parseAddress(request.uri());
        bootstrap.connect(address.getHost(), address.getPort()).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                ctx.channel().writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, BAD_GATEWAY));
                NettyUtils.closeOnFlush(ctx.channel());
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
        logger.error("", e);
        NettyUtils.closeOnFlush(ctx.channel());
    }
}