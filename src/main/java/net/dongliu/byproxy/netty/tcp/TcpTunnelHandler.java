package net.dongliu.byproxy.netty.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import net.dongliu.byproxy.netty.NettyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler tunnel proxy traffic, for socks proxy or http connect proxy.
 */
public class TcpTunnelHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(TcpTunnelHandler.class);

    private final Channel targetChannel;
    private final boolean pass;

    public TcpTunnelHandler(Channel targetChannel, boolean pass) {
        this.targetChannel = targetChannel;
        this.pass = pass;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ByteBuf)) {
            logger.error("not ByteBuf message: {}", msg);
            if (pass) {
                ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
            }
            return;
        }

        ByteBuf buf = (ByteBuf) msg;
        if (pass) {
            ctx.fireChannelRead(buf.retainedDuplicate());
        }
        if (targetChannel.isActive()) {
            targetChannel.writeAndFlush(msg);
        } else {
            logger.error("proxy channel inactive");
            buf.release();
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
