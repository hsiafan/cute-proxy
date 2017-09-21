package net.dongliu.byproxy.netty.switcher;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.ByteToMessageDecoder.Cumulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.ByteToMessageDecoder.MERGE_CUMULATOR;

/**
 * Switcher to distinguish different protocols
 */
public class ProtocolSwitcher extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ProtocolSwitcher.class);

    private final Cumulator cumulator = MERGE_CUMULATOR;
    private final ProtocolMatcher[] matcherList;
    private final boolean[] results;

    private ByteBuf buf;

    public ProtocolSwitcher(ProtocolMatcher... matcherList) {
        if (matcherList.length == 0) {
            throw new IllegalArgumentException("No matcher for ProtocolSwitcher");
        }
        this.matcherList = matcherList;
        this.results = new boolean[matcherList.length];
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof ByteBuf)) {
            logger.error("unexpected message type for ProtocolSwitcher: {}", msg.getClass());
            ctx.close();
            return;
        }
        ByteBuf in = (ByteBuf) msg;
        if (buf == null) {
            buf = in;
        } else {
            buf = cumulator.cumulate(ctx.alloc(), buf, in);
        }

        boolean allMiss = true;
        for (int i = 0; i < matcherList.length; i++) {
            if (results[i]) {
                continue;
            }

            ProtocolMatcher matcher = matcherList[i];
            int match = matcher.match(buf.duplicate());
            if (match == ProtocolMatcher.MATCH) {
                logger.debug("matched by {}", matcher.getClass().getSimpleName());
                matcher.handlePipeline(ctx.pipeline());
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(buf);
                buf = null;
                return;
            }

            if (match == ProtocolMatcher.DISMATCH) {
                results[i] = true;
            }

            if (match == ProtocolMatcher.PENDING) {
                allMiss = false;
            }
        }

        if (allMiss) {
            logger.error("unsupported protocol");
            buf.release();
            buf = null;
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (buf != null) {
            buf.release();
            buf = null;
        }
        logger.error("", cause);
        ctx.close();
    }

}
