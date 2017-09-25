package net.dongliu.byproxy.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler tunnel proxy traffic, for socks proxy or http connect proxy.
 */
public class TunnelProxyHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(TunnelProxyHandler.class);

    private final Channel targetChannel;

    public TunnelProxyHandler(Channel targetChannel) {
        this.targetChannel = targetChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (targetChannel.isActive()) {
            targetChannel.writeAndFlush(msg);
        } else {
            logger.error("proxy channel inactive");
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (targetChannel.isActive()) {
            NettyUtils.closeOnFlush(targetChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        logger.error("", e);
        ctx.close();
    }
}
