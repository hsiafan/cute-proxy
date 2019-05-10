package net.dongliu.proxy.store;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * DataStore backend by MMappedByteBuffer
 */
class MMappedStore extends DataStore {

    public MMappedStore(int size) {
        super(createBuffer(size));
    }

    private static ByteBuffer createBuffer(int size) {
        try {
            File file = File.createTempFile("cute_proxy_", ".tmp");
            RandomAccessFile raf = new RandomAccessFile(file, "rw");

            if (!file.delete()) {
                // fall back to deletion on exit on windows
                file.deleteOnExit();
            }
            return raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, size);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
