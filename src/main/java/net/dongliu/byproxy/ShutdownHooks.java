package net.dongliu.byproxy;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Liu Dong
 */
public class ShutdownHooks {

    static final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    public static void registerTask(Runnable runnable) {
        tasks.offer(runnable);
    }
}
