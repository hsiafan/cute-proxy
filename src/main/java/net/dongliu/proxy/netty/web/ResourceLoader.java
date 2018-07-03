package net.dongliu.proxy.netty.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * For serving web content from InputStream
 */
public class ResourceLoader {
    private static ResourceLoader instance = new ResourceLoader();

    private ExecutorService resourceLoaderExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("Resource-Loader");
                return thread;
            }
    );

    public static ResourceLoader getInstance() {
        return instance;
    }

    public CompletableFuture<byte[]> loadResource(InputStream input) {
        return CompletableFuture.supplyAsync(() -> {
            try (input) {
                return input.readAllBytes();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, resourceLoaderExecutor);
    }

    public CompletableFuture<byte[]> loadClassPathResource(String path) {
        return loadResource(this.getClass().getResourceAsStream(path));
    }
}
