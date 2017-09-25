package net.dongliu.byproxy.netty.proxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest;
import io.netty.handler.codec.socksx.v4.Socks4CommandType;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.NettyUtils;
import net.dongliu.byproxy.ssl.SSLContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

public class Socks4ProxyAuthHandler extends SimpleChannelInboundHandler<SocksMessage> {
    private static final Logger logger = LoggerFactory.getLogger(Socks4ProxyAuthHandler.class);

    @Nullable
    private final MessageListener messageListener;
    @Nullable
    private final SSLContextManager sslContextManager;

    public Socks4ProxyAuthHandler(@Nullable MessageListener messageListener,
                                  @Nullable SSLContextManager sslContextManager) {
        this.messageListener = messageListener;
        this.sslContextManager = sslContextManager;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, SocksMessage socksRequest) throws Exception {
        if (socksRequest.version() != SocksVersion.SOCKS4a) {
            logger.error("unexpected socks version: {}", socksRequest.version());
            ctx.close();
            return;
        }
        Socks4CommandRequest socksV4CmdRequest = (Socks4CommandRequest) socksRequest;
        if (socksV4CmdRequest.type() == Socks4CommandType.CONNECT) {
            ctx.pipeline().addLast("socks4-proxy-connector", new Socks4ProxyConnectHandler(messageListener, sslContextManager));
            ctx.pipeline().remove(this);
            ctx.fireChannelRead(socksRequest);
        } else {
            ctx.close();
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