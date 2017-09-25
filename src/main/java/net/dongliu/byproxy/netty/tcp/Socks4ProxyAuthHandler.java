package net.dongliu.byproxy.netty.tcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest;
import io.netty.handler.codec.socksx.v4.Socks4CommandType;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.NettyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

public class Socks4ProxyAuthHandler extends SimpleChannelInboundHandler<SocksMessage> {
    private static final Logger logger = LoggerFactory.getLogger(Socks4ProxyAuthHandler.class);

    @Nullable
    private final MessageListener messageListener;

    public Socks4ProxyAuthHandler(@Nullable MessageListener messageListener) {
        this.messageListener = messageListener;
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
            ctx.pipeline().addLast(new Socks4ProxyConnectHandler(messageListener));
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