package net.dongliu.byproxy;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generate uniq id for each message
 *
 * @author Liu Dong
 */
public class MessageIdGenerator {
    private final AtomicLong seq;
    private final String uuid = UUID.randomUUID().toString();

    private static MessageIdGenerator instance = new MessageIdGenerator();

    public static MessageIdGenerator getInstance() {
        return instance;
    }

    public MessageIdGenerator() {
        seq = new AtomicLong();
    }

    public String nextId() {
        return uuid + "_" + seq.incrementAndGet();
    }
}
