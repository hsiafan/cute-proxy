package net.dongliu.byproxy.netty.tcp;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.interceptor.HttpInterceptorContext;
import net.dongliu.byproxy.netty.interceptor.HttpRequestInboundInterceptor;
import net.dongliu.byproxy.netty.interceptor.HttpResponseInboundInterceptor;
import net.dongliu.byproxy.utils.NetAddress;

import javax.annotation.Nullable;

public interface TcpProxyHandlerTraits {

    default void initTcpProxyHandlers(ChannelHandlerContext ctx, NetAddress address, Channel outboundChannel,
                                      @Nullable MessageListener messageListener) {
        boolean intercept = messageListener != null;
        ctx.pipeline().addLast(new TcpTunnelHandler(outboundChannel, intercept));
        outboundChannel.pipeline().addLast(new TcpTunnelHandler(ctx.channel(), intercept));

        if (intercept) {
            HttpInterceptorContext interceptorContext = new HttpInterceptorContext(false, address,
                    messageListener);
            ctx.pipeline().addLast(new HttpRequestDecoder());
            ctx.pipeline().addLast(new HttpRequestInboundInterceptor(interceptorContext, false));
            outboundChannel.pipeline().addLast(new HttpResponseDecoder());
            outboundChannel.pipeline().addLast(new HttpResponseInboundInterceptor(interceptorContext, false));
        }
    }
}
