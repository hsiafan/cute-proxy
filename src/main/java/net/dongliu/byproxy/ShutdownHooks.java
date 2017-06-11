package net.dongliu.byproxy;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Liu Dong
 */
@Slf4j
public class ShutdownHooks {

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
