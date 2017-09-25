package net.dongliu.byproxy.netty.web;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * For serving web content from InputStream
 */
public class ResourceLoader {
    private static ResourceLoader instance = new ResourceLoader();

    private ExecutorService resourceLoaderExecutor = Executors.newSingleThreadExecutor(
            new ThreadFactory() {
                private AtomicLong seq = new AtomicLong();

                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setDaemon(true);
                    thread.setName("Resource-Loader-" + seq.getAndIncrement());
                    return thread;
                }
            }
    );

    public static ResourceLoader getInstance() {
        return instance;
    }

    public CompletableFuture<byte[]> loadResource(InputStream input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ByteStreams.toByteArray(input);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                Closeables.closeQuietly(input);
            }
        }, resourceLoaderExecutor);
    }

    public CompletableFuture<byte[]> loadClassPathResource(String path) {
        return loadResource(this.getClass().getResourceAsStream(path));
    }
}
