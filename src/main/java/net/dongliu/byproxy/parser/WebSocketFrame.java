package net.dongliu.byproxy.parser;

import javax.annotation.Nullable;

/**
 * @author Liu Dong
 */
public class WebSocketFrame {
    private static final int FRAME_CONTINUATION = 0;
    private static final int FRAME_TEXT = 1;
    private static final int FRAME_BINARY = 2;
    private static final int FRAME_CONNECTION_CLOSE = 8;
    private static final int FRAME_PING = 9;
    private static final int FRAME_PONG = 10;

    private final boolean fin;
    private final int opcode;
    private final long payloadLen;
    private final boolean mask;
    @Nullable
    private final byte[] maskData;

    private byte[] data;

    public WebSocketFrame(boolean fin, int opcode, long payloadLen, boolean mask, @Nullable byte[] maskData,
                          byte[] data) {
        this.fin = fin;
        this.opcode = opcode;
        this.payloadLen = payloadLen;
        this.mask = mask;
        this.maskData = maskData;
        this.data = data;
    }

    public boolean isControlFrame() {
        return Bits.bitSet(opcode, 3);
    }

    // unmask data if need
    public byte[] getFinalData() {
        if (maskData == null) {
            return data;
        }
        byte[] finalData = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            finalData[i] = (byte) (data[i] ^ maskData[i % maskData.length]);
        }
        return finalData;
    }

    public boolean isFin() {
        return fin;
    }

    public int getOpcode() {
        return opcode;
    }

    public long getPayloadLen() {
        return payloadLen;
    }

    public boolean isMask() {
        return mask;
    }

    @Nullable
    public byte[] getMaskData() {
        return maskData;
    }

    public byte[] getData() {
        return data;
    }
}