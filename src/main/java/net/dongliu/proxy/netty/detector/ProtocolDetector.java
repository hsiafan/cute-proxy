package net.dongliu.proxy.netty.detector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.ByteToMessageDecoder.Cumulator;
import net.dongliu.proxy.netty.NettyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.ByteToMessageDecoder.MERGE_CUMULATOR;

/**
 * Switcher to distinguish different protocols
 */
public class ProtocolDetector extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ProtocolDetector.class);

    private final Cumulator cumulator = MERGE_CUMULATOR;
    private final ProtocolMatcher[] matcherList;
    private int index;

    private ByteBuf buf;

    public ProtocolDetector(ProtocolMatcher... matchers) {
        if (matchers.length == 0) {
            throw new IllegalArgumentException("No matcher for ProtocolDetector");
        }
        this.matcherList = matchers;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ByteBuf)) {
            logger.error("unexpected message type for ProtocolDetector: {}", msg.getClass());
            NettyUtils.closeOnFlush(ctx.channel());
            return;
        }
        ByteBuf in = (ByteBuf) msg;
        if (buf == null) {
            buf = in;
        } else {
            buf = cumulator.cumulate(ctx.alloc(), buf, in);
        }

        for (int i = index; i < matcherList.length; i++) {
            ProtocolMatcher matcher = matcherList[i];
            int match = matcher.match(buf.duplicate());
            if (match == ProtocolMatcher.MATCH) {
                logger.debug("matched by {}", matcher.getClass().getName());
                matcher.handlePipeline(ctx.pipeline());
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(buf);
                return;
            }

            if (match == ProtocolMatcher.PENDING) {
                index = i;
                return;
            }
        }

        // all miss
        logger.error("unsupported protocol");
        buf.release();
        buf = null;
        NettyUtils.closeOnFlush(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (buf != null) {
            buf.release();
            buf = null;
        }
        logger.error("", cause);
        NettyUtils.closeOnFlush(ctx.channel());
    }

}
