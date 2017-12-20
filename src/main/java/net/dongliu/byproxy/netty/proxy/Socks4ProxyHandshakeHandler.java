package net.dongliu.byproxy.netty.proxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest;
import io.netty.handler.codec.socksx.v4.Socks4CommandType;
import io.netty.handler.proxy.ProxyHandler;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.NettyUtils;
import net.dongliu.byproxy.netty.SSLContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class Socks4ProxyHandshakeHandler extends SimpleChannelInboundHandler<SocksMessage> {
    private static final Logger logger = LoggerFactory.getLogger(Socks4ProxyHandshakeHandler.class);

    @Nullable
    private final MessageListener messageListener;
    @Nullable
    private final SSLContextManager sslContextManager;
    @Nullable
    private final Supplier<ProxyHandler> proxyHandlerSupplier;

    public Socks4ProxyHandshakeHandler(@Nullable MessageListener messageListener,
                                       @Nullable SSLContextManager sslContextManager,
                                       @Nullable Supplier<ProxyHandler> proxyHandlerSupplier) {
        this.messageListener = messageListener;
        this.sslContextManager = sslContextManager;
        this.proxyHandlerSupplier = proxyHandlerSupplier;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, SocksMessage socksRequest) throws Exception {
        if (socksRequest.version() != SocksVersion.SOCKS4a) {
            logger.error("unexpected socks version: {}", socksRequest.version());
            NettyUtils.closeOnFlush(ctx.channel());
            return;
        }
        Socks4CommandRequest socksV4CmdRequest = (Socks4CommandRequest) socksRequest;
        if (socksV4CmdRequest.type() == Socks4CommandType.CONNECT) {
            ctx.pipeline().addLast(new Socks4ProxyHandler(messageListener, sslContextManager,
                    proxyHandlerSupplier));
            ctx.pipeline().remove(this);
            ctx.fireChannelRead(socksRequest);
        } else {
            NettyUtils.closeOnFlush(ctx.channel());
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        logger.error("", e);
        NettyUtils.closeOnFlush(ctx.channel());
    }
}