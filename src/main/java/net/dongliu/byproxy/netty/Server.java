package net.dongliu.byproxy.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.dongliu.byproxy.netty.switcher.HttpProtocolMatcher;
import net.dongliu.byproxy.netty.switcher.ProtocolSwitcher;
import net.dongliu.byproxy.netty.switcher.SocksProxyProtocolMatcher;

import java.net.InetSocketAddress;

public class Server {
    private final int port;

    public Server(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(EventLoopGroups.master, EventLoopGroups.worker)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(port))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ProtocolSwitcher protocolSwitcher = new ProtocolSwitcher(
                                new SocksProxyProtocolMatcher(),
                                new HttpProtocolMatcher()
                        );
                        ch.pipeline().addLast(protocolSwitcher);
                    }
                });

        ChannelFuture f = bootstrap.bind().sync();
        f.channel().closeFuture().sync();
    }

    public static void main(String[] args) throws Exception {
        new Server(2080).start();

    }
}
