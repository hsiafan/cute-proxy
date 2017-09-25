package net.dongliu.byproxy.netty.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Promise;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.detector.AnyMatcher;
import net.dongliu.byproxy.netty.detector.ProtocolDetector;
import net.dongliu.byproxy.netty.detector.SSLMatcher;
import net.dongliu.byproxy.netty.interceptor.HttpInterceptor;
import net.dongliu.byproxy.ssl.SSLContextManager;
import net.dongliu.byproxy.ssl.SSLUtils;
import net.dongliu.byproxy.utils.NetAddress;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

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
            ctx.pipeline().addLast(new ReplayHandler(outboundChannel));
            outboundChannel.pipeline().addLast(new ReplayHandler(ctx.channel()));
            return;
        }

        SSLContextManager sslContextManager = sslContextManager();
        if (sslContextManager == null) {
            throw new RuntimeException("SSLContextManager must be set when use mitm");
        }

        ProtocolDetector protocolDetector = new ProtocolDetector(
                new SSLMatcher().onMatched(p -> {
                    //TODO: create ssl context is slow, should execute in another executor?
                    SSLContext sslContext = sslContextManager.createSSlContext(address.getHost());
                    SSLEngine serverSSLEngine = sslContext.createSSLEngine();
                    serverSSLEngine.setUseClientMode(false);
                    p.addLast("ssl", new SslHandler(serverSSLEngine));

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

        ctx.pipeline().addLast(new HttpServerCodec());
        ctx.pipeline().addLast("tcp-tunnel-handler", new ReplayHandler(outboundChannel));

        outboundChannel.pipeline().addLast(new HttpClientCodec());
        HttpInterceptor interceptor = new HttpInterceptor(ssl, address, messageListener()).onUpgrade(() -> {
            ctx.pipeline().remove(HttpServerCodec.class);
            WebSocketFrameDecoder frameDecoder = new WebSocket13FrameDecoder(true, true, 65536, false);
            WebSocketFrameEncoder frameEncoder = new WebSocket13FrameEncoder(false);
            ctx.pipeline().addBefore("tcp-tunnel-handler", "ws-decoder", frameDecoder);
            ctx.pipeline().addBefore("tcp-tunnel-handler", "ws-encoder", frameEncoder);
        });
        outboundChannel.pipeline().addLast("http-interceptor", interceptor);
        outboundChannel.pipeline().addLast("tcp-tunnel-handler", new ReplayHandler(ctx.channel()));

    }

    @Nullable
    MessageListener messageListener();

    @Nullable
    SSLContextManager sslContextManager();
}
