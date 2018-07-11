package net.dongliu.proxy.netty.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import net.dongliu.proxy.netty.NettyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.dongliu.proxy.netty.NettyUtils.causedByClientClose;

/**
 * Handler tunnel proxy traffic, for socks proxy or http connect proxy.
 */
public class ReplayHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ReplayHandler.class);

    private final Channel targetChannel;

    public ReplayHandler(Channel targetChannel) {
        this.targetChannel = targetChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        logger.debug("from {} to {}, replay message: {}", ctx.channel().remoteAddress(),
                targetChannel.remoteAddress(), msg.getClass());
        ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
        if (targetChannel.isActive()) {
            targetChannel.writeAndFlush(msg);
        } else {
            logger.warn("proxy target channel {} inactive", targetChannel.remoteAddress());
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
        if (causedByClientClose(e)) {
            logger.warn("client closed connection: {}", e.getMessage());
        } else {
            logger.error("something error", e);
        }
        NettyUtils.closeOnFlush(ctx.channel());
    }
}
