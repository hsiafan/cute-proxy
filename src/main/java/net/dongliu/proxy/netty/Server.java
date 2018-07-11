package net.dongliu.proxy.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.dongliu.proxy.MessageListener;
import net.dongliu.proxy.netty.detector.*;
import net.dongliu.proxy.netty.handler.ServerSSLContextManager;
import net.dongliu.proxy.setting.ProxySetting;
import net.dongliu.proxy.setting.ServerSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Proxy and http server by netty
 */
public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private final ServerSetting setting;
    private final ServerSSLContextManager sslContextManager;
    private final MessageListener messageListener;
    private final Supplier<ProxyHandler> proxyHandlerSupplier;

    private ChannelFuture bindFuture;
    private EventLoopGroup master;
    private EventLoopGroup worker;


    public Server(ServerSetting setting, ServerSSLContextManager sslContextManager,
                  ProxySetting proxySetting, MessageListener messageListener) {
        this.setting = requireNonNull(setting);
        this.sslContextManager = requireNonNull(sslContextManager);
        this.messageListener = requireNonNull(messageListener);
        this.proxyHandlerSupplier = new ProxyHandlerSupplier(requireNonNull(proxySetting));
    }

    public void start() throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        master = new NioEventLoopGroup(1, new DefaultThreadFactory("netty-master"));
        worker = new NioEventLoopGroup(
                Math.min(Runtime.getRuntime().availableProcessors() * 2, 16),
                new DefaultThreadFactory("netty-worker"));
        bootstrap.group(master, worker)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(setting.port()))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        int timeoutSeconds = setting.timeout();
                        var idleStateHandler = new IdleStateHandler(timeoutSeconds, timeoutSeconds,
                                timeoutSeconds, TimeUnit.SECONDS);
                        ch.pipeline().addLast(idleStateHandler);
                        ch.pipeline().addLast(new CloseTimeoutChannelHandler());
                        var protocolDetector = new ProtocolDetector(
                                new Socks5ProxyMatcher(messageListener, sslContextManager, proxyHandlerSupplier),
                                new Socks4ProxyMatcher(messageListener, sslContextManager, proxyHandlerSupplier),
                                new HttpConnectProxyMatcher(messageListener, sslContextManager, proxyHandlerSupplier),
                                new HttpProxyMatcher(messageListener, proxyHandlerSupplier),
                                new HttpMatcher(sslContextManager)
                        );
                        ch.pipeline().addLast("protocol-detector", protocolDetector);
                    }
                });

        bindFuture = bootstrap.bind().sync();
        logger.info("proxy server start");
    }

    public void stop() {
        try {
            bindFuture.channel().close().sync();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        try {
            master.shutdownGracefully(0, 0, TimeUnit.SECONDS).sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            worker.shutdownGracefully(0, 0, TimeUnit.SECONDS).sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

}
