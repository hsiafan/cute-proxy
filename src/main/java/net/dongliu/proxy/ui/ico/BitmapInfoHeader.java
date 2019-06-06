package net.dongliu.proxy.ui.ico;

import java.nio.ByteBuffer;

public class BitmapInfoHeader {
    private int headerSize;
    private int width;
    private int height;
    private short planeNum;
    private short bitsPerPixel;
    private int compression;
    private int imageSize;
    private int xpixelsPerM;
    private int ypixelsPerM;
    private int colorsUsed;
    private int colorsImportant;

    public static final int SIZE = 40;

    public BitmapInfoHeader(int headerSize, int width, int height, short planeNum, short bitsPerPixel, int compression,
                            int imageSize, int xpixelsPerM, int ypixelsPerM, int colorsUsed, int colorsImportant) {
        this.headerSize = headerSize;
        this.width = width;
        this.height = height;
        this.planeNum = planeNum;
        this.bitsPerPixel = bitsPerPixel;
        this.compression = compression;
        this.imageSize = imageSize;
        this.xpixelsPerM = xpixelsPerM;
        this.ypixelsPerM = ypixelsPerM;
        this.colorsUsed = colorsUsed;
        this.colorsImportant = colorsImportant;
    }

    public static BitmapInfoHeader decode(ByteBuffer buffer) {
        return new BitmapInfoHeader(
                buffer.getInt(),
                buffer.getInt(),
                buffer.getInt(),
                buffer.getShort(),
                buffer.getShort(),
                buffer.getInt(),
                buffer.getInt(),
                buffer.getInt(),
                buffer.getInt(),
                buffer.getInt(),
                buffer.getInt()
        );
    }

    public int headerSize() {
        return Unsigns.ensure(headerSize);
    }

    public int width() {
        return Unsigns.ensure(width);
    }

    public int height() {
        return Unsigns.ensure(height);
    }

    public int planeNum() {
        return Short.toUnsignedInt(planeNum);
    }

    public int bitsPerPixel() {
        return Short.toUnsignedInt(bitsPerPixel);
    }

    public int compression() {
        return compression;
    }

    public int imageSize() {
        return Unsigns.ensure(imageSize);
    }

    public int xpixelsPerM() {
        return Unsigns.ensure(xpixelsPerM);
    }

    public int ypixelsPerM() {
        return Unsigns.ensure(ypixelsPerM);
    }

    public int colorsUsed() {
        return Unsigns.ensure(colorsUsed);
    }

    public int colorsImportant() {
        return Unsigns.ensure(colorsImportant);
    }
}
