package net.dongliu.proxy.ui.ico;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IconDirHeader {
    private short Reserved;
    private short imageType;
    private short imageNum;

    private static final int SIZE = 6;
    public static final int ICO = 1;
    public static final int CUR = 2;

    public IconDirHeader(short reserved, short imageType, short imageNum) {
        Reserved = reserved;
        this.imageType = imageType;
        this.imageNum = imageNum;
    }

    public static IconDirHeader decode(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return new IconDirHeader(buffer.getShort(), buffer.getShort(), buffer.getShort());
    }

    public short reserved() {
        return Reserved;
    }

    public short imageType() {
        return imageType;
    }

    public int imageNum() {
        return Short.toUnsignedInt(imageNum);
    }
}
