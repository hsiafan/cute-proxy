package net.dongliu.proxy.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http2.Http2Flags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.ByteToMessageDecoder.MERGE_CUMULATOR;
import static io.netty.handler.codec.http2.Http2FrameTypes.SETTINGS;

/**
 * Netty(4.1.19.Final) http2 codec require the first frame server returned is SETTING and Ack is not set.
 * However some Http2 server impl set the Ack flag, which cause Netty throw a exception.
 * This filter is used to clear the Ack bit of first SETTING frame.
 */
public class SettingFrameFixer extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SettingFrameFixer.class);

    private final ByteToMessageDecoder.Cumulator cumulator = MERGE_CUMULATOR;
    private ByteBuf buf;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ByteBuf)) {
            ctx.fireChannelRead(msg);
            return;
        }

        ByteBuf in = (ByteBuf) msg;
        if (buf == null) {
            buf = in;
        } else {
            buf = cumulator.cumulate(ctx.alloc(), buf, in);
        }

        if (buf.readableBytes() < 5) {
            return;
        }

        byte frameType = buf.getByte(buf.readerIndex() + 3);
        byte flags = buf.getByte(buf.readerIndex() + 4);
        if (frameType == SETTINGS && (flags & Http2Flags.ACK) != 0) {
            // need to clear the Ack bit
            logger.debug("fix setting frame");
            flags = (byte) (flags & 0xfe);
            buf.setByte(buf.readerIndex() + 4, flags);
        }
        ctx.pipeline().remove(this);
        ctx.fireChannelRead(buf);
        buf = null;
    }
}
