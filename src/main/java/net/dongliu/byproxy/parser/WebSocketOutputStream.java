package net.dongliu.byproxy.parser;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * @author Liu Dong
 */
public class WebSocketOutputStream extends FilterOutputStream {
    public WebSocketOutputStream(OutputStream out) {
        super(out);
    }

    public void writeFrame(WebSocketFrame frame) throws IOException {
        int first = 0;
        if (frame.isFin()) {
            first |= 1 << 7;
        }
        first |= frame.getOpcode() & 0xf;
        write(first);

        int second = 0;
        if (frame.isMask()) {
            second |= 1 << 7;
        }
        long len = frame.getPayloadLen();
        if (len <= 125) {
            second |= len;
            write(second);
        } else if (len <= 0xffff) {
            second |= 126;
            write(second);
            writeUInt16((int) len);
        } else {
            second |= 127;
            write(second);
            writeUInt64(len);
        }

        if (frame.isMask()) {
            write(Objects.requireNonNull(frame.getMaskData()));
        }
        if (len > 0) {
            write(frame.getData());
        }
    }

    private void writeUInt16(int len) throws IOException {
        write((len >> 8) & 0xff);
        write(len & 0xff);
    }

    private void writeUInt64(long len) throws IOException {
        write((int) ((len >> 56) & 0xff));
        write((int) ((len >> 48) & 0xff));
        write((int) ((len >> 40) & 0xff));
        write((int) ((len >> 32) & 0xff));
        write((int) ((len >> 24) & 0xff));
        write((int) ((len >> 16) & 0xff));
        write((int) ((len >> 8) & 0xff));
        write((int) (len & 0xff));
    }

}
