package net.dongliu.byproxy.netty.interceptor;

import io.netty.buffer.ByteBuf;
import net.dongliu.byproxy.store.Body;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * For netty interceptor, async save http body.
 * Methods of this class should be call in netty eventLoop thread.
 */
public class BodySaver {
    private static final Logger logger = LoggerFactory.getLogger(BodySaver.class);
    private ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("BodySaver-worker-1");
        return thread;
    });

    private static BodySaver instance = new BodySaver();

    public static BodySaver getInstance() {
        return instance;
    }

    /**
     * Append http body async
     *
     * @param executor the netty eventLoop thread executor
     */
    public CompletableFuture<Void> append(Body body, ByteBuf buf, Executor executor) {
        ByteBuffer buffer = buf.retain().nioBuffer();
        return runAsync(() -> body.append(buffer)).thenRunAsync(buf::release, executor);
    }

    public CompletableFuture<Void> finish(Body body) {
        return runAsync(body::finish);
    }

    private CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, executor);
    }
}
