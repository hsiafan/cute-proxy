package net.dongliu.byproxy.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.detector.HttpProtocolMatcher;
import net.dongliu.byproxy.netty.detector.ProtocolDetector;
import net.dongliu.byproxy.netty.detector.SocksProxyProtocolMatcher;
import net.dongliu.byproxy.setting.ServerSetting;
import net.dongliu.byproxy.ssl.SSLContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;

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

    public Server(ServerSetting setting) {
        this.setting = setting;
    }

    public void start() throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        master = new NioEventLoopGroup(1);
        worker = new NioEventLoopGroup();
        bootstrap.group(master, worker)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(setting.getPort()))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ProtocolDetector protocolDetector = new ProtocolDetector(
                                new SocksProxyProtocolMatcher(messageListener, sslContextManager),
                                new HttpProtocolMatcher(messageListener, sslContextManager)
                        );
                        ch.pipeline().addLast(protocolDetector);
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

    public void stop() {
        try {
            bindFuture.channel().close().sync();
        } catch (InterruptedException ignored) {
        }
        master.shutdownGracefully();
        worker.shutdownGracefully();
        logger.info("proxy server stop");
    }

    public static void main(String[] args) throws Exception {
        ServerSetting setting = new ServerSetting("127.0.0.1", 2080, 1200);
        new Server(setting).start();

    }
}
