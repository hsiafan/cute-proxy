package net.dongliu.byproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Liu Dong
 */
public class ShutdownHooks {
    private static final Logger logger = LoggerFactory.getLogger(ShutdownHooks.class);
    private static final List<Runnable> tasks = new ArrayList<>();
    private static boolean shutdown = false;

    public synchronized static void registerTask(Runnable runnable) {
        if (shutdown) {
            throw new IllegalStateException("Already shutdown");
        }
        tasks.add(runnable);
    }

    public synchronized static void shutdownAll() {
        if (shutdown) {
            throw new IllegalStateException("Already shutdown");
        }
        shutdown = true;
        for (Runnable task : tasks) {
            try {
                task.run();
            } catch (Throwable e) {
                logger.error("run shutdown hooks error", e);
            }
        }
    }
}
