package net.dongliu.proxy.ui.ico;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BitmapFileHeader {
    public static final int SIZE = 14;
    private short magic;
    private int size;
    private short reserved1;
    private short reserved2;
    private int dataOffset;

    public BitmapFileHeader(short magic, int size, short reserved1, short reserved2, int dataOffset) {
        this.magic = magic;
        this.size = size;
        this.reserved1 = reserved1;
        this.reserved2 = reserved2;
        this.dataOffset = dataOffset;
    }

    public static BitmapFileHeader newHeader(int size, int dataOffset) {
        return new BitmapFileHeader((short) 0x4d42, size, (short) 0, (short) 0, dataOffset);
    }

    public void encode(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN)
                .putShort(magic)
                .putInt(size)
                .putShort(reserved1)
                .putShort(reserved2)
                .putInt(dataOffset);
    }
}
