package net.dongliu.byproxy.proxy;

import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author Liu Dong
 */
class Utils {
    private static Logger logger = LoggerFactory.getLogger(Utils.class);

    //Note: input1 and input2 should be tee input stream
    public static void tunnel(InputStream input1, OutputStream output1, Socket socket1,
                              InputStream input2, OutputStream output2, Socket socket2,
                              ExecutorService executor) throws IOException {
        Future<?> future = executor.submit(() -> {
            try {
                ByteStreams.copy(input1, output2);
            } catch (Throwable t) {
                logger.warn("tunnel traffic failed", t);
            } finally {
                shutdownOneWay(socket1, socket2, output2);
            }
        });
        try {
            ByteStreams.copy(input2, output1);
        } finally {
            shutdownOneWay(socket2, socket1, output1);
        }
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            Throwables.throwIfUnchecked(e.getCause());
            Throwables.throwIfInstanceOf(e.getCause(), IOException.class);
        }
    }

    static void shutdownOneWay(Socket srcSocket, Socket dstSocket, OutputStream dstOutput) {
        try {
            srcSocket.shutdownInput();
        } catch (Exception e) {
            logger.debug("", e);
        }
        try {
            dstOutput.flush();
        } catch (IOException e) {
            logger.error("", e);
        }
        try {
            dstSocket.shutdownOutput();
        } catch (Exception e) {
            logger.debug("", e);
        }
    }

}
