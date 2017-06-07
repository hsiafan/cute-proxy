package net.dongliu.byproxy.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream for websocket
 *
 * @author Liu Dong
 */
public class Http2InputStream extends RichInputStream implements NumberReader {
    private static Logger logger = LoggerFactory.getLogger(Http2InputStream.class);
    private CompressionContext compressionContext;

    public Http2InputStream(InputStream inputStream) {
        super(inputStream);
    }

    /**
     * Read an webSocket frame
     */
    @Nullable
    private Frame readFrameHeader() throws IOException {
        int payloadLen = readUInt24();
        int type = read();
        int flag = read();
        int streamIdentifier = ((read() & 0x7fffffff) << 24) + readUInt24();
        return new Frame(payloadLen, type, flag, streamIdentifier);
    }

    private class Frame {
        private final int payloadLen;
        private final int type;
        private final int flag;
        private final int streamIdentifier;

        public Frame(int payloadLen, int type, int flag, int streamIdentifier) {
            this.payloadLen = payloadLen;
            this.type = type;
            this.flag = flag;
            this.streamIdentifier = streamIdentifier;
        }

        public int getPayloadLen() {
            return payloadLen;
        }

        public int getType() {
            return type;
        }

        public int getFlag() {
            return flag;
        }

        public int getStreamIdentifier() {
            return streamIdentifier;
        }
    }

    private static class CompressionContext {

    }
}
