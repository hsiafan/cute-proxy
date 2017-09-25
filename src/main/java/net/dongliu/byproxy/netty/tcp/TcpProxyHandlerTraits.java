package net.dongliu.byproxy.netty.tcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.util.concurrent.Promise;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.ChannelActiveAwareHandler;
import net.dongliu.byproxy.netty.interceptor.HttpInterceptorContext;
import net.dongliu.byproxy.netty.interceptor.HttpRequestInboundInterceptor;
import net.dongliu.byproxy.netty.interceptor.HttpResponseInboundInterceptor;
import net.dongliu.byproxy.netty.switcher.ProtocolDetector;
import net.dongliu.byproxy.utils.NetAddress;

import javax.annotation.Nullable;

public interface TcpProxyHandlerTraits {

    default Bootstrap initBootStrap(Promise<Channel> promise, EventLoopGroup eventLoopGroup) {
        return new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelActiveAwareHandler(promise));
    }

    default void initTcpProxyHandlers(ChannelHandlerContext ctx, NetAddress address, Channel outboundChannel,
                                      @Nullable MessageListener messageListener) {
        boolean intercept = messageListener != null;
        ctx.pipeline().addLast(new TcpTunnelHandler(outboundChannel, intercept));
        outboundChannel.pipeline().addLast(new TcpTunnelHandler(ctx.channel(), intercept));

        if (intercept) {
//            ProtocolDetector detector = new ProtocolDetector();
            HttpInterceptorContext interceptorContext = new HttpInterceptorContext(false, address,
                    messageListener);
            ctx.pipeline().addLast(new HttpRequestDecoder());
            ctx.pipeline().addLast(new HttpRequestInboundInterceptor(interceptorContext, false));
            outboundChannel.pipeline().addLast(new HttpResponseDecoder());
            outboundChannel.pipeline().addLast(new HttpResponseInboundInterceptor(interceptorContext, false));
        }
    }
}
