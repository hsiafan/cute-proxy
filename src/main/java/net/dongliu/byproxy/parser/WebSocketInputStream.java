package net.dongliu.byproxy.parser;

import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * InputStream for websocket
 * //TODO extensions support
 *
 * @author Liu Dong
 */
public class WebSocketInputStream extends RichInputStream implements NumberReader {
    private static Logger logger = LoggerFactory.getLogger(WebSocketInputStream.class);

    public WebSocketInputStream(InputStream inputStream) {
        super(inputStream);
    }

    /**
     * Read an entire webSocket data message
     *
     * @return the websocket message. return null if reach the end of stream
     */
    @Nullable
    public List<WebSocketFrame> readMessage() throws IOException {
        List<WebSocketFrame> frameList = new ArrayList<>();
        WebSocketFrame frame = readDataFrame();
        if (frame == null) {
            return null;
        }
        frameList.add(frame);
        while (!frame.isFin()) {
            frame = readDataFrame();
            if (frame == null) {
                throw new IOException("WebSocket Message not terminated");
            }
            frameList.add(frame);
        }
        return frameList;
    }

    /**
     * Read next data frame
     */
    private WebSocketFrame readDataFrame() throws IOException {
        while (true) {
            WebSocketFrame frame = readFrame();
            if (frame == null) {
                return null;
            }
            if (!frame.isControlFrame()) {
                return frame;
            }
        }
    }

    /**
     * Read one webSocket frame.
     *
     * @return the webSocket frame. return null if reach end of stream
     */
    @Nullable
    public WebSocketFrame readFrame() throws IOException {
        int first = read();
        if (first == -1) {
            return null;
        }
        int second = read();
        boolean fin = Bits.bitSet(first, 7);
        boolean rsv1 = Bits.bitSet(first, 6);
        boolean rsv2 = Bits.bitSet(first, 5);
        boolean rsv3 = Bits.bitSet(first, 4);
        int opcode = first & 0xf;
        boolean mask = Bits.bitSet(second, 7);
        long payloadLen = second & 0x7f;
        if (payloadLen <= 125) {

        } else if (payloadLen == 126) {
            payloadLen = readUInt16();
        } else if (payloadLen == 127) {
            payloadLen = readUInt64();
        }
        byte[] maskData = null;
        if (mask) {
            maskData = readExact(4);
        }
        logger.debug("fin: {}, opcode: {}, mask: {}, payloadLen: {}", fin, opcode, mask, payloadLen);
        byte[] data = new byte[(int) payloadLen];
        ByteStreams.readFully(this, data);
        return new WebSocketFrame(fin, opcode, payloadLen, mask, maskData, data);
    }

}
