package net.dongliu.byproxy.netty;

import com.google.common.base.Strings;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.detector.HttpMatcher;
import net.dongliu.byproxy.netty.detector.ProtocolDetector;
import net.dongliu.byproxy.netty.detector.SocksProxyMatcher;
import net.dongliu.byproxy.setting.ProxySetting;
import net.dongliu.byproxy.setting.ServerSetting;
import net.dongliu.byproxy.ssl.SSLContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Proxy and http server by netty
 */
public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private ServerSetting setting;
    @Nullable
    private SSLContextManager sslContextManager;
    @Nullable
    private MessageListener messageListener;

    private ChannelFuture bindFuture;
    private EventLoopGroup master;
    private EventLoopGroup worker;
    @Nullable
    private Supplier<ProxyHandler> proxyHandlerSupplier;

    public Server(ServerSetting setting) {
        this.setting = setting;
    }

    public void start() throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        master = new NioEventLoopGroup(1, new DefaultThreadFactory("netty-master"));
        worker = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2,
                new DefaultThreadFactory("netty-worker"));
        bootstrap.group(master, worker)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(setting.getPort()))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ProtocolDetector protocolDetector = new ProtocolDetector(
                                new SocksProxyMatcher(messageListener, sslContextManager, proxyHandlerSupplier),
                                new HttpMatcher(messageListener, sslContextManager, proxyHandlerSupplier)
                        );
                        ch.pipeline().addLast("protocol-detector", protocolDetector);
                    }
                });

        bindFuture = bootstrap.bind().sync();
        logger.info("proxy server start");
    }


    public void setSslContextManager(@Nullable SSLContextManager sslContextManager) {
        this.sslContextManager = sslContextManager;
    }

    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public void setProxySetting(ProxySetting proxySetting) {
        if (proxySetting == null && !proxySetting.isUse()) {
            proxyHandlerSupplier = null;
            return;
        }
        InetSocketAddress address = new InetSocketAddress(proxySetting.getHost(), proxySetting.getPort());
        this.proxyHandlerSupplier = () -> {
            switch (proxySetting.getType()) {
                case ProxySetting.TYPE_HTTP:
                    if (Strings.isNullOrEmpty(proxySetting.getUser())) {
                        return new HttpProxyHandler(address);
                    } else {
                        return new HttpProxyHandler(address, proxySetting.getUser(), proxySetting.getPassword());
                    }

                case ProxySetting.TYPE_SOCKS5:
                    if (Strings.isNullOrEmpty(proxySetting.getUser())) {
                        return new Socks5ProxyHandler(address);
                    } else {
                        return new Socks5ProxyHandler(address, proxySetting.getUser(), proxySetting.getPassword());
                    }
                case ProxySetting.TYPE_SOCKS4:
                    if (Strings.isNullOrEmpty(proxySetting.getUser())) {
                        return new Socks4ProxyHandler(address);
                    } else {
                        return new Socks4ProxyHandler(address, proxySetting.getUser());
                    }
                default:
                    throw new RuntimeException("unknown proxy type: " + proxySetting.getType());
            }
        };
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
