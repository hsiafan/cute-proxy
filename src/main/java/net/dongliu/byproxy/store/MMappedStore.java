package net.dongliu.byproxy.store;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

class MMappedStore implements DataStore {
    private ByteBuffer buffer;

    public MMappedStore() {
        try {
            File file = File.createTempFile("byproxy_", ".tmp");
            RandomAccessFile raf = new RandomAccessFile(file, "rw");

            if (!file.delete()) {
                // fall back to deletion on exit on windows
                file.deleteOnExit();
            }
            buffer = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, Stores.REGION_SIZE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public int write(ByteBuffer buffer) {
        if (buffer.remaining() > this.buffer.remaining()) {
            return -1;
        }
        int pos = this.buffer.position();
        this.buffer.put(buffer);
        return pos;
    }

    @Override
    public ByteBuffer read(int offset, int size) {
        ByteBuffer buffer = this.buffer.duplicate();
        buffer.position(offset).limit(offset + size);
        return buffer.slice();
    }
}
