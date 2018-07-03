package net.dongliu.proxy;

import net.dongliu.proxy.data.Message;

/**
 * Listener to receive request data. The operation in the call back method must not block
 *
 * @author Liu Dong
 */
@FunctionalInterface
public interface MessageListener {
    void onMessage(Message message);
}
