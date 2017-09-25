package net.dongliu.byproxy.netty.tcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Promise;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.ChannelActiveAwareHandler;
import net.dongliu.byproxy.netty.detector.AnyMatcher;
import net.dongliu.byproxy.netty.detector.ProtocolDetector;
import net.dongliu.byproxy.netty.detector.SSLMatcher;
import net.dongliu.byproxy.netty.interceptor.HttpInterceptorContext;
import net.dongliu.byproxy.netty.interceptor.HttpRequestInboundInterceptor;
import net.dongliu.byproxy.netty.interceptor.HttpResponseInboundInterceptor;
import net.dongliu.byproxy.ssl.SSLContextManager;
import net.dongliu.byproxy.ssl.SSLUtils;
import net.dongliu.byproxy.utils.NetAddress;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.util.function.Supplier;

public interface TcpProxyHandlerTraits {

    default Bootstrap initBootStrap(Promise<Channel> promise, EventLoopGroup eventLoopGroup) {
        return new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelActiveAwareHandler(promise));
    }

    default void initTcpProxyHandlers(ChannelHandlerContext ctx, NetAddress address, Channel outboundChannel) {
        boolean intercept = messageListener() != null;
        if (!intercept) {
            ctx.pipeline().addLast(new TcpTunnelHandler(outboundChannel, false));
            outboundChannel.pipeline().addLast(new TcpTunnelHandler(ctx.channel(), false));
            return;
        }

        SSLContextManager sslContextManager = sslContextManager();
        if (sslContextManager == null) {
            throw new RuntimeException("SSLContextManager must be set when use mitm");
        }

        Supplier<SSLEngine> serverSSLEngineSupplier = () -> {
            SSLContext sslContext = sslContextManager.createSSlContext(address.getHost());
            SSLEngine sslEngine = sslContext.createSSLEngine();
            sslEngine.setUseClientMode(false);
            return sslEngine;
        };
        ProtocolDetector protocolDetector = new ProtocolDetector(
                new SSLMatcher().onMatched(p -> {
                    //TODO: create ssl context is slow, should execute in another executor?
                    p.addLast("ssl", new SslHandler(serverSSLEngineSupplier.get()));

                    SSLEngine sslEngine = SSLUtils.createClientSSlContext().createSSLEngine();
                    sslEngine.setUseClientMode(true);
                    outboundChannel.pipeline().addLast(new SslHandler(sslEngine));
                    initPlainHandler(ctx, address, outboundChannel, true);
                }),
                new AnyMatcher().onMatched(p -> initPlainHandler(ctx, address, outboundChannel, false))
        );
        ctx.pipeline().addLast(protocolDetector);
    }

    default void initPlainHandler(ChannelHandlerContext ctx, NetAddress address, Channel outboundChannel, boolean ssl) {
        ctx.pipeline().addLast(new TcpTunnelHandler(outboundChannel, true));
        outboundChannel.pipeline().addLast(new TcpTunnelHandler(ctx.channel(), true));

        HttpInterceptorContext interceptorContext = new HttpInterceptorContext(ssl, address, messageListener());
        ctx.pipeline().addLast(new HttpRequestDecoder());
        ctx.pipeline().addLast(new HttpRequestInboundInterceptor(interceptorContext, false));
        outboundChannel.pipeline().addLast(new HttpResponseDecoder());
        outboundChannel.pipeline().addLast(new HttpResponseInboundInterceptor(interceptorContext, false));
    }

    @Nullable
    MessageListener messageListener();

    @Nullable
    SSLContextManager sslContextManager();
}
