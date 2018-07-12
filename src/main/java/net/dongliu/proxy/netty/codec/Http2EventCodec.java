package net.dongliu.proxy.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.ByteToMessageDecoder.Cumulator;
import io.netty.handler.codec.http2.*;
import net.dongliu.proxy.netty.codec.frame.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.buffer.Unpooled.unreleasableBuffer;
import static io.netty.handler.codec.ByteToMessageDecoder.MERGE_CUMULATOR;
import static io.netty.handler.codec.http2.Http2CodecUtil.connectionPrefaceBuf;

/**
 * Wrap Http2FrameReader to decode http2 frames.
 */
public class Http2EventCodec extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(Http2EventCodec.class);
    private static final ByteBuf CONNECTION_PREFACE = unreleasableBuffer(connectionPrefaceBuf());
    private static final Cumulator prefaceCumulator = MERGE_CUMULATOR;
    private ByteBuf prefaceBuffer;
    private static final Cumulator frameDataCumulator = MERGE_CUMULATOR;
    private ByteBuf frameDataBuffer;

    private boolean expectPreface = true;

    private Http2FrameReader frameReader;
    private Http2FrameWriter frameWriter;

    public Http2EventCodec() throws Http2Exception {
        //TODO: fix HPACK - invalid max dynamic table size
        // we cannot get a HpackDecoder due to internal access constructor
        var headersDecoder = new DefaultHttp2HeadersDecoder(true);
        headersDecoder.maxHeaderTableSize(40960);
        var frameReader = new DefaultHttp2FrameReader(headersDecoder);
        frameReader.maxFrameSize(0xffffff);
        this.frameReader = frameReader;
        var frameWriter = new DefaultHttp2FrameWriter();
//        frameWriter.maxFrameSize(0xffffff);
        this.frameWriter = frameWriter;
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (!(msg instanceof Http2Event)) {
            // may be preface, or other message
            logger.debug("no http2 event found: {}", msg.getClass());
            ctx.write(msg, promise);
            return;
        }
        logger.debug("write http2 event to {}, type: {}", ctx.channel().remoteAddress(),
                msg.getClass().getSimpleName());

        if (msg instanceof Http2DataEvent) {
            var event = (Http2DataEvent) msg;
            frameWriter.writeData(ctx, event.streamId(), event.data(), event.padding(), event.endOfStream(), promise);
        } else if (msg instanceof Http2HeadersEvent) {
            Http2HeadersEvent event = (Http2HeadersEvent) msg;
            frameWriter.writeHeaders(ctx, event.streamId(), event.headers(), event.padding(), event.endOfStream(),
                    promise);
        } else if (msg instanceof Http2PriorityHeadersEvent) {
            var event = (Http2PriorityHeadersEvent) msg;
            frameWriter.writeHeaders(ctx, event.streamId(), event.headers(), event.streamDependency(),
                    event.weight(), event.exclusive(), event.padding(), event.endOfStream(), promise);
        } else if (msg instanceof Http2PriorityEvent) {
            var event = (Http2PriorityEvent) msg;
            frameWriter.writePriority(ctx, event.streamId(), event.streamDependency(), event.weight(),
                    event.exclusive(), promise);
        } else if (msg instanceof Http2RstEvent) {
            var event = (Http2RstEvent) msg;
            frameWriter.writeRstStream(ctx, event.streamId(), event.errorCode(), promise);
        } else if (msg instanceof Http2SettingEvent) {
            var event = (Http2SettingEvent) msg;
            frameWriter.writeSettings(ctx, event.http2Settings(), promise);
        } else if (msg instanceof Http2SettingAckEvent) {
            frameWriter.writeSettingsAck(ctx, promise);
        } else if (msg instanceof Http2PingEvent) {
            var event = (Http2PingEvent) msg;
            frameWriter.writePing(ctx, false, event.data(), promise);
        } else if (msg instanceof Http2PingAckEvent) {
            var event = (Http2PingAckEvent) msg;
            frameWriter.writePing(ctx, true, event.data(), promise);
        } else if (msg instanceof Http2PushPromiseEvent) {
            var event = (Http2PushPromiseEvent) msg;
            frameWriter.writePushPromise(ctx, event.streamId(), event.promisedStreamId(), event.headers(),
                    event.padding(), promise);
        } else if (msg instanceof Http2GoAwayEvent) {
            var event = (Http2GoAwayEvent) msg;
            frameWriter.writeGoAway(ctx, event.lastStreamId(), event.errorCode(), event.debugData(), promise);
        } else if (msg instanceof Http2WindowUpdateEvent) {
            var event = (Http2WindowUpdateEvent) msg;
            frameWriter.writeWindowUpdate(ctx, event.streamId(), event.windowSizeIncrement(), promise);
        } else if (msg instanceof Http2UnknownEvent) {
            var event = (Http2UnknownEvent) msg;
            frameWriter.writeFrame(ctx, event.frameType(), event.streamId(), event.flags(), event.payload(), promise);
        } else {
            logger.error("unknown http2 event: {}", msg.getClass());
            ctx.close();
        }
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Http2Exception {
        if (!(msg instanceof ByteBuf)) {
            ctx.fireChannelRead(msg);
            return;
        }
        var in = (ByteBuf) msg;
        logger.debug("read http2 data, size: {}, endpoint: {}", in.readableBytes(), ctx.channel().remoteAddress());
        if (expectPreface) {
            if (ByteBufUtil.equals(CONNECTION_PREFACE, CONNECTION_PREFACE.readerIndex(),
                    in, in.readerIndex(), Math.min(in.readableBytes(), CONNECTION_PREFACE.readableBytes()))) {
                if (prefaceBuffer != null) {
                    in = prefaceCumulator.cumulate(ctx.alloc(), prefaceBuffer, in);
                }
                if (in.readableBytes() >= 24) {
                    prefaceBuffer = null;
                    var prefaceBuf = in.retain().slice(in.readerIndex(), 24);
                    in.skipBytes(24);
                    logger.debug("preface read from {}", ctx.channel().remoteAddress());
                    ctx.fireChannelRead(prefaceBuf);
                    expectPreface = false;
                } else {
                    prefaceBuffer = in;
                }
            } else {
                expectPreface = false;
            }
        }

        if (frameDataBuffer != null) {
            in = frameDataCumulator.cumulate(ctx.alloc(), frameDataBuffer, in);
            frameDataBuffer = null;
        }

        frameReader.readFrame(ctx, in, new Http2FrameListener() {
            @Override
            public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding,
                                  boolean endOfStream) {
                int dataLen = data.readableBytes();
                var http2Event = new Http2DataEvent(streamId, data.retain(), padding, endOfStream);
                onEventRead(ctx, http2Event);
                return dataLen + padding;
            }

            @Override
            public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding,
                                      boolean endOfStream) {
                var http2Event = new Http2HeadersEvent(streamId, headers, padding, endOfStream);
                onEventRead(ctx, http2Event);
            }

            @Override
            public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers,
                                      int streamDependency, short weight, boolean exclusive, int padding,
                                      boolean endOfStream) {
                var http2Event = new Http2PriorityHeadersEvent(streamId, headers, padding, endOfStream,
                        streamDependency, weight, exclusive);
                onEventRead(ctx, http2Event);
            }

            @Override
            public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency,
                                       short weight, boolean exclusive) {
                var http2Event = new Http2PriorityEvent(streamId, streamDependency, weight, exclusive);
                onEventRead(ctx, http2Event);
            }

            @Override
            public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) {
                var http2Event = new Http2RstEvent(streamId, errorCode);
                onEventRead(ctx, http2Event);
            }

            @Override
            public void onSettingsAckRead(ChannelHandlerContext ctx) {
                var http2Event = new Http2SettingAckEvent();
                onEventRead(ctx, http2Event);
            }

            @Override
            public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) {
                var http2Event = new Http2SettingEvent(settings);
                onEventRead(ctx, http2Event);
            }

            @Override
            public void onPingRead(ChannelHandlerContext ctx, long data) {
                var http2Event = new Http2PingEvent(data);
                onEventRead(ctx, http2Event);
            }

            @Override
            public void onPingAckRead(ChannelHandlerContext ctx, long data) {
                var http2Event = new Http2PingAckEvent(data);
                onEventRead(ctx, http2Event);
            }

            @Override
            public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId,
                                          Http2Headers headers, int padding) {
                var http2Event = new Http2PushPromiseEvent(streamId, promisedStreamId, headers, padding);
                onEventRead(ctx, http2Event);
            }

            @Override
            public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData) {
                var http2Event = new Http2GoAwayEvent(lastStreamId, errorCode, debugData.retain());
                onEventRead(ctx, http2Event);
            }

            @Override
            public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement) {
                var http2Event = new Http2WindowUpdateEvent(streamId, windowSizeIncrement);
                onEventRead(ctx, http2Event);
            }

            @Override
            public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId, Http2Flags flags,
                                       ByteBuf payload) {
                var http2Event = new Http2UnknownEvent(frameType, streamId, flags, payload.retain());
                onEventRead(ctx, http2Event);
            }

            private void onEventRead(ChannelHandlerContext ctx, Http2Event http2Event) {
                logger.debug("http2 event read from {}, type: {}", ctx.channel().remoteAddress(),
                        http2Event.getClass().getSimpleName());
                ctx.fireChannelRead(http2Event);
                ctx.flush();
            }
        });
        if (in.readableBytes() > 0) {
            frameDataBuffer = in;
        } else {
            in.release();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (prefaceBuffer != null) {
            prefaceBuffer.release();
            prefaceBuffer = null;
        }
        if (frameDataBuffer != null) {
            frameDataBuffer.release();
            frameDataBuffer = null;
        }
    }
}
