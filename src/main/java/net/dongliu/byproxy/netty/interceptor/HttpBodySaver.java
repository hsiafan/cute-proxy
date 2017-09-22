package net.dongliu.byproxy.netty.interceptor;

import net.dongliu.byproxy.store.HttpBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpBodySaver {
    private static final Logger logger = LoggerFactory.getLogger(HttpBodySaver.class);
    private ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("BodySaver-worker-1");
        return thread;
    });

    private static HttpBodySaver instance = new HttpBodySaver();

    public static HttpBodySaver getInstance() {
        return instance;
    }

    public CompletableFuture<Void> save(HttpBody body, ByteBuffer buffer) {
        return runAsync(() -> body.append(buffer));
    }

    public CompletableFuture<Void> finish(HttpBody body) {
        return runAsync(body::finish);
    }

    private CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, executor);
    }
}
