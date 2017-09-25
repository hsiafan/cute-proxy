package net.dongliu.byproxy.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.switcher.HttpProtocolMatcher;
import net.dongliu.byproxy.netty.switcher.ProtocolSwitcher;
import net.dongliu.byproxy.netty.switcher.SocksProxyProtocolMatcher;
import net.dongliu.byproxy.setting.ServerSetting;
import net.dongliu.byproxy.ssl.SSLContextManager;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;

public class Server {

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
        worker = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
        bootstrap.group(master, worker)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(setting.getPort()))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ProtocolSwitcher protocolSwitcher = new ProtocolSwitcher(
                                new SocksProxyProtocolMatcher(),
                                new HttpProtocolMatcher(messageListener, sslContextManager)
                        );
                        ch.pipeline().addLast(protocolSwitcher);
                    }
                });

        bindFuture = bootstrap.bind().sync();
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
    }

    public static void main(String[] args) throws Exception {
        ServerSetting setting = new ServerSetting("127.0.0.1", 2080, 1200);
        new Server(setting).start();

    }
}