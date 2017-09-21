package net.dongliu.byproxy.server;

import com.google.common.base.Throwables;
import net.dongliu.byproxy.parser.WebSocketFrame;
import net.dongliu.byproxy.parser.WebSocketInputStream;
import net.dongliu.byproxy.parser.WebSocketOutputStream;
import net.dongliu.byproxy.store.HttpBody;
import net.dongliu.byproxy.store.HttpBodyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Handle web socket traffic
 *
 * @author Liu Dong
 */
public class WebSocketHandler {
    private static Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private final ExecutorService executor;

    public WebSocketHandler(ExecutorService executor) {
        this.executor = executor;
    }

    public void handle(InputStream fromIn, OutputStream fromOut, Socket fromSocket,
                       InputStream toIn, OutputStream toOut, Socket toSocket,
                       String host, String url, MessageListener messageListener) throws IOException {
        Future<?> future = executor.submit(() -> {
            try {
                readWebSocket(toIn, fromOut, host, url, messageListener, false);
            } catch (Throwable t) {
                logger.warn("handle webSocket {} failed", url, t);
            } finally {
                Utils.shutdownOneWay(toSocket, fromSocket, fromOut);
            }
        });

        try {
            readWebSocket(fromIn, toOut, host, url, messageListener, true);
        } catch (SocketException e) {
            logger.error("handle webSocket {} failed", url, e);
        } finally {
            Utils.shutdownOneWay(fromSocket, toSocket, toOut);
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

    private void readWebSocket(InputStream input, OutputStream out, String host, String url,
                               MessageListener messageListener, boolean isRequest) throws IOException {
        WebSocketInputStream srcWSInput = new WebSocketInputStream(input);
        WebSocketOutputStream dstWSOutput = new WebSocketOutputStream(out);
        List<WebSocketFrame> dataFrameBuffer = new ArrayList<>();
        while (true) {
            WebSocketFrame frame = srcWSInput.readFrame();
            if (frame == null) {
                break;
            }
            dstWSOutput.writeFrame(frame);

            if (frame.isControlFrame()) {
                continue;
            }
            dataFrameBuffer.add(frame);
            if (frame.isFin()) {
                String messageId = MessageIdGenerator.getInstance().nextId();
                WebSocketFrame firstFrame = dataFrameBuffer.get(0);
                int type = firstFrame.getOpcode();
                HttpBodyType httpBodyType = type == 1 ? HttpBodyType.text : HttpBodyType.binary;
                HttpBody httpBody = new HttpBody(httpBodyType, UTF_8, null);
                for (WebSocketFrame webSocketFrame : dataFrameBuffer) {
                    httpBody.append(ByteBuffer.wrap(webSocketFrame.getFinalData()));
                }
                httpBody.finish();
                messageListener.onWebSocket(messageId, host, url, type, isRequest, httpBody);
                dataFrameBuffer.clear();
            }
        }
    }
}
