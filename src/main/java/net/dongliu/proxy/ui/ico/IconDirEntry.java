package net.dongliu.proxy.ui.ico;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IconDirEntry {
    private byte width;
    private byte height;
    private byte colorNum;
    private byte reserved;
    private short colorPlane;
    private short bitsPerPixel;
    private int dataSize;
    private int dataOffset;

    public IconDirEntry(byte width, byte height, byte colorNum, byte reserved, short colorPlane, short bitsPerPixel,
                        int dataSize, int dataOffset) {
        this.width = width;
        this.height = height;
        this.colorNum = colorNum;
        this.reserved = reserved;
        this.colorPlane = colorPlane;
        this.bitsPerPixel = bitsPerPixel;
        this.dataSize = dataSize;
        this.dataOffset = dataOffset;
    }

    public static IconDirEntry decode(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return new IconDirEntry(
                buffer.get(),
                buffer.get(),
                buffer.get(),
                buffer.get(),
                buffer.getShort(),
                buffer.getShort(),
                buffer.getInt(),
                buffer.getInt()
        );
    }

    public int width() {
        if (width == 0) {
            return 256;
        }
        return Byte.toUnsignedInt(width);
    }

    public int height() {
        if (height == 0) {
            return 256;
        }
        return Byte.toUnsignedInt(height);
    }

    public int colorNum() {
        return Short.toUnsignedInt(colorNum);
    }

    public byte reserved() {
        return reserved;
    }

    public int colorPlane() {
        return Short.toUnsignedInt(colorPlane);
    }

    public int bitsPerPixel() {
        return Short.toUnsignedInt(bitsPerPixel);
    }

    public int dataSize() {
        return Unsigns.ensure(dataSize);
    }

    public int dataOffset() {
        return Unsigns.ensure(dataOffset);
    }
}
