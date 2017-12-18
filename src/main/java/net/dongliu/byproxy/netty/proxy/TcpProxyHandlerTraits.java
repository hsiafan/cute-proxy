package net.dongliu.byproxy.netty.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameEncoder;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Promise;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.NettySettings;
import net.dongliu.byproxy.netty.detector.AnyMatcher;
import net.dongliu.byproxy.netty.detector.ProtocolDetector;
import net.dongliu.byproxy.netty.detector.SSLMatcher;
import net.dongliu.byproxy.netty.interceptor.HttpInterceptor;
import net.dongliu.byproxy.ssl.ClientSSLContextFactory;
import net.dongliu.byproxy.ssl.SSLContextManager;
import net.dongliu.byproxy.utils.NetAddress;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.util.function.Supplier;

/**
 * Common methods for handle http/socks proxy
 */
public interface TcpProxyHandlerTraits {

    default Bootstrap initBootStrap(Promise<Channel> promise, EventLoopGroup eventLoopGroup) {
        return new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, NettySettings.CONNECT_TIMEOUT)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        Supplier<ProxyHandler> proxyHandlerSupplier = proxyHandlerSupplier();
                        if (proxyHandlerSupplier != null) {
                            ProxyHandler proxyHandler = proxyHandlerSupplier.get();
                            ch.pipeline().addLast(proxyHandler);
                        }
                        ch.pipeline().addLast(new ChannelActiveAwareHandler(promise));
                    }
                });
    }

    default void initTcpProxyHandlers(ChannelHandlerContext ctx, NetAddress address, Channel outChannel) {
        MessageListener messageListener = messageListener();
        boolean intercept = messageListener != null;
        if (!intercept) {
            ctx.pipeline().addLast(new ReplayHandler(outChannel));
            outChannel.pipeline().addLast(new ReplayHandler(ctx.channel()));
            return;
        }

        SSLContextManager sslContextManager = sslContextManager();
        if (sslContextManager == null) {
            throw new RuntimeException("SSLContextManager must be set when use mitm");
        }

        ProtocolDetector protocolDetector = new ProtocolDetector(
                new SSLMatcher(pipeline -> {
                    SSLContext serverContext = sslContextManager.createSSlContext(address.getHost());
                    SSLEngine serverEngine = serverContext.createSSLEngine();
                    serverEngine.setUseClientMode(false);
                    pipeline.addLast("ssl", new SslHandler(serverEngine));

                    SSLContext sslContext = ClientSSLContextFactory.getInstance().get();
                    SSLEngine sslEngine = sslContext.createSSLEngine(address.getHost(), address.getPort()); // using SNI
                    sslEngine.setUseClientMode(true);
                    outChannel.pipeline().addLast(new SslHandler(sslEngine));
                    initInterceptorHandler(ctx, address, messageListener, outChannel, true);
                }),
                new AnyMatcher(p -> initInterceptorHandler(ctx, address, messageListener, outChannel, false))
        );
        ctx.pipeline().addLast(protocolDetector);
    }

    private static void initInterceptorHandler(ChannelHandlerContext ctx, NetAddress address,
                                               MessageListener messageListener,
                                               Channel outboundChannel, boolean ssl) {

        ctx.pipeline().addLast(new HttpServerCodec());
        ctx.pipeline().addLast("", new HttpServerExpectContinueHandler());
        ctx.pipeline().addLast("tcp-tunnel-handler", new ReplayHandler(outboundChannel));

        outboundChannel.pipeline().addLast(new HttpClientCodec());
        HttpInterceptor interceptor = new HttpInterceptor(ssl, address, messageListener).onUpgrade(() -> {
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

    @Nullable
    Supplier<ProxyHandler> proxyHandlerSupplier();
}
