package net.dongliu.byproxy.server;

import net.dongliu.byproxy.setting.MainSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Proxy server
 *
 * @author Liu Dong
 */
@ThreadSafe
public class ProxyServer {
    private static Logger logger = LoggerFactory.getLogger(ProxyServer.class);
    private final MainSetting mainSetting;
    private final SSLContextManager sslContextManager;
    private volatile ServerSocket serverSocket;
    private volatile MessageListener messageListener;

    private volatile ExecutorService executor;
    private volatile Thread masterThread;
    private final AtomicInteger threadCounter = new AtomicInteger();

    public ProxyServer(MainSetting config, SSLContextManager sslContextManager) {
        this.mainSetting = Objects.requireNonNull(config);
        this.sslContextManager = sslContextManager;
    }

    /**
     * Start proxy server
     */
    public void start() {
        executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setName("proxy-server-worker-" + threadCounter.getAndIncrement());
            t.setDaemon(true);
            return t;
        });

        masterThread = new Thread(() -> {
            try {
                run();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        masterThread.setName("proxy-server-master");
        masterThread.setDaemon(true);
        masterThread.start();
    }

    /**
     * Wait proxy server to stop
     */
    public void join() throws InterruptedException {
        masterThread.join();
    }

    /**
     * Start proxy
     */
    private void run() throws IOException {
        if (mainSetting.getHost().isEmpty()) {
            serverSocket = new ServerSocket(mainSetting.getPort(), 128);
        } else {
            serverSocket = new ServerSocket(mainSetting.getPort(), 128, InetAddress.getByName(mainSetting.getHost()));
        }
        logger.info("proxy server run at {}:{}", mainSetting.getHost(), mainSetting.getPort());
        while (true) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (SocketException e) {
                if (Thread.currentThread().isInterrupted()) {
                    // server be stopped
                    break;
                } else {
                    logger.error("", e);
                }
                continue;
            }
            ProxyWorker worker;
            try {
                socket.setSoTimeout(mainSetting.getTimeout() * 1000);
                worker = new ProxyWorker(socket, sslContextManager, messageListener, executor);
            } catch (Exception e) {
                try {
                    socket.close();
                } catch (IOException ignore) {
                }
                logger.error("Create new proxy worker failed.", e);
                continue;
            }
            logger.debug("Accept new connection, from: {}", socket.getInetAddress());
            executor.submit(worker);
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
        }
    }

    /**
     * Stop proxy
     */
    public void stop() {
        if (!masterThread.isInterrupted()) {
            logger.info("Stopping proxy server...");
            masterThread.interrupt();
            try {
                serverSocket.close();
            } catch (IOException ignore) {
            }
            executor.shutdownNow();
        }
    }

    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }
}
