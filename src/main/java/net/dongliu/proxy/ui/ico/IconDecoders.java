package net.dongliu.proxy.ui.ico;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import net.dongliu.commons.io.ByteBufferInputStream;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

public class IconDecoders {

    public static List<Image> decode(byte[] data) {
        var buf = ByteBuffer.wrap(data);
        var buffer = buf.duplicate().order(LITTLE_ENDIAN);
        IconDirHeader header = IconDirHeader.decode(buffer);
        var images = new ArrayList<Image>();
        for (int i = 0; i < header.imageNum(); i++) {
            var entry = IconDirEntry.decode(buffer);

            var entryBuffer = buf.duplicate()
                    .position(entry.dataOffset())
                    .limit(entry.dataOffset() + entry.dataSize())
                    .slice()
                    .order(LITTLE_ENDIAN);
            var firstLong = entryBuffer.getLong(0);
            if (firstLong == 0x89504E470D0A1A0AL) {
                //png
                var image = new Image(new ByteBufferInputStream(entryBuffer));
                images.add(image);
                continue;
            }

            // should be bmp
            var infoHeaderBuffer = entryBuffer.duplicate().limit(BitmapInfoHeader.SIZE).order(LITTLE_ENDIAN);
            var infoHeader = BitmapInfoHeader.decode(infoHeaderBuffer);
            if (infoHeader.headerSize() != BitmapInfoHeader.SIZE) {
                throw new RuntimeException("invalid bmp icon image");
            }

            int width = entry.width();
            int height = entry.width();
            //color table is only present for 1, 4 or 8 bit (indexed) images
            int colorsBufferSize;
            if (infoHeader.bitsPerPixel() <= 8) {
                colorsBufferSize = entry.colorNum() * 4;
            } else {
                colorsBufferSize = 0;
            }
            int xorBitmapSize = entry.bitsPerPixel() * entry.width() * entry.height() / 8;
            int andBitmapLineLen = infoHeader.width();
            if (andBitmapLineLen % 32 != 0) {
                andBitmapLineLen = (andBitmapLineLen / 32 + 1) * 32;
            }

            int andBitmapSize;
            if (infoHeader.height() == infoHeader.width() * 2) {
                andBitmapSize = andBitmapLineLen * entry.height() / 8;
                infoHeaderBuffer.putInt(8, height);
            } else {
                andBitmapSize = 0;
            }

            int bitMapDataSize = entry.dataSize();
            int bitMapFileSize = BitmapFileHeader.SIZE + bitMapDataSize;
            int bitMapDataOffset = BitmapFileHeader.SIZE + BitmapInfoHeader.SIZE + colorsBufferSize;
            var bmpBuffer = ByteBuffer.allocate(bitMapFileSize).order(LITTLE_ENDIAN);
            var bitMapFileHeader = BitmapFileHeader.newHeader(bitMapFileSize, bitMapDataOffset);
            bitMapFileHeader.encode(bmpBuffer);
            bmpBuffer.put(entryBuffer.position(0).limit(bitMapDataSize));

            Image image;
            if (entry.bitsPerPixel() == 32) {
                var xorBitmapSizeBuffer = entryBuffer.duplicate()
                        .position(BitmapInfoHeader.SIZE + colorsBufferSize)
                        .limit(BitmapInfoHeader.SIZE + colorsBufferSize + xorBitmapSize)
                        .slice()
                        .order(LITTLE_ENDIAN);
                // jfx bpm loader do not deal with alpha channel
                WritableImage wi = new WritableImage(width, height);
                for (int y = height - 1; y >= 0; y--) {
                    for (int x = 0; x < width; x++) {
                        int rgba = xorBitmapSizeBuffer.getInt();
                        wi.getPixelWriter().setArgb(x, y, rgba);
                    }
                }
                image = wi;
                images.add(image);
                continue;
            }
            image = new Image(new ByteBufferInputStream(bmpBuffer.position(0)));
            if (image.isError()) {
                continue;
            }
            if (andBitmapSize > 0) {
                PixelReader pixelReader = image.getPixelReader();
                WritableImage wImage = new WritableImage(width, height);
                PixelWriter pixelWriter = wImage.getPixelWriter();
                int andOffset = BitmapInfoHeader.SIZE + colorsBufferSize + xorBitmapSize;
                ByteBuffer andBitmapBuffer = entryBuffer.duplicate()
                        .position(andOffset)
                        .limit(andOffset + andBitmapSize)
                        .slice();
                byte[] row = new byte[andBitmapLineLen / 8];
                for (int y = height - 1; y >= 0; y--) {
                    andBitmapBuffer.get(row);
                    for (int x = 0; x < width; x++) {
                        boolean mask = (row[x / 8] & (1 << (7 - x % 8))) != 0;
                        int argb = pixelReader.getArgb(x, y);
//                        System.out.print(mask ? 1 : 0);
                        byte alpha = (byte) (mask ? 0 : 255);
                        int newArgb = (alpha << 24) | (argb & 0xffffff);
                        pixelWriter.setArgb(x, y, newArgb);
//                        Prints.print("argb:", argb, "alpha:", alpha, "newArgb:", newArgb);
                    }
//                    System.out.println();
                }
                image = wImage;
            }
            images.add(image);
        }
        return images;
    }
}
