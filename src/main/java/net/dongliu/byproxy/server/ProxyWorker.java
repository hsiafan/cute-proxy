package net.dongliu.byproxy.server;

import net.dongliu.byproxy.exception.HttpParserException;
import net.dongliu.byproxy.parser.HttpInputStream;
import net.dongliu.byproxy.parser.HttpOutputStream;
import net.dongliu.byproxy.struct.RequestLine;
import net.dongliu.byproxy.parser.RichInputStream;
import net.dongliu.byproxy.utils.NetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * Proxy workers
 *
 * @author Liu Dong
 */
public class ProxyWorker implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(ProxyWorker.class);
    private final Socket serverSocket;
    private final SSLContextManager sslContextManager;
    private final MessageListener messageListener;
    private final ExecutorService executor;

    public ProxyWorker(Socket serverSocket, SSLContextManager sslContextManager, MessageListener messageListener,
                       ExecutorService executor)
            throws IOException {
        this.serverSocket = serverSocket;
        this.sslContextManager = sslContextManager;
        this.messageListener = Objects.requireNonNull(messageListener);
        this.executor = executor;
    }

    @Override
    public void run() {
        try {
            InputStream in = serverSocket.getInputStream();
            RichInputStream input = new RichInputStream(in);
            input.mark(4096);
            int b = input.read();
            if (b == -1) {
                //error
                logger.error("empty client request");
                return;
            }
            if (b == 4) {
                logger.error("socks4 proxy not supported");
                return;
            }
            if (b == 5) {
                // socks5
                handleSocks(input);
                return;
            }
            handleHttp(b, input);
        } catch (RejectedExecutionException e) {
            logger.debug("server thread pool shutdown", e);
        } catch (HttpParserException e) {
            logger.error("Illegal http data", e);
        } catch (SocketTimeoutException e) {
            logger.error("Socket Timeout", e);
        } catch (SocketException e) {
            logger.error("Socket exception", e);
        } catch (IOException | UncheckedIOException e) {
            logger.error("IO error", e);
        } catch (Exception e) {
            logger.error("Error while handle http traffic", e);
        } catch (Throwable e) {
            logger.error("", e);
        } finally {
            try {
                serverSocket.close();
            } catch (IOException ignore) {
            }
        }
    }

    //socks5 proxy
    private void handleSocks(RichInputStream in) throws IOException {
        in.enableBuffered();

        // handshake
        int methodNum = in.readUnsignedInt8();
        byte[] methods = in.readExact(methodNum);
        if (methods.length != methodNum) {
            logger.error("invalid sock5 handshake");
            return;
        }

        OutputStream out = serverSocket.getOutputStream();
        out.write(new byte[]{5, 0});

        // get socks request
        int version = in.readUnsignedInt8();
        int cmd = in.readUnsignedInt8();
        if (version != 5 || cmd != 1) {
            logger.error("socks not supported");
            return;
        }

        int reserved = in.read();
        int type = in.readUnsignedInt8();
        String host;
        if (type == 1) {//ipv4
            byte[] ipData = in.readExact(4);
            host = Inet4Address.getByAddress(ipData).getHostAddress();
        } else if (type == 3) {//domain
            int domainLen = in.readUnsignedInt8();
            byte[] domainData = in.readExact(domainLen);
            host = new String(domainData, StandardCharsets.US_ASCII);
        } else if (type == 4) {//ipv6
            byte[] ipData = in.readExact(16);
            host = Inet6Address.getByAddress(ipData).getHostAddress();
        } else {
            logger.error("unknown socks target type: " + type);
            return;
        }
        int port = in.readUnsignedInt16();
        logger.debug("socks proxy request to {}:{}", host, port);

        // just tell client all things going well...
        out.write(new byte[]{0x05, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x08, 0x43});
        out.flush();
        Handler handler = new ConnectProxyHandler(serverSocket, messageListener, host, port, sslContextManager,
                executor);
        handler.handle();
    }

    private void handleHttp(int b, RichInputStream input) throws IOException {
        String rawRequestLine = (char) b + input.readLine();
        Objects.requireNonNull(rawRequestLine);
        input.reset();

        RequestLine requestLine = RequestLine.parse(rawRequestLine);
        if (requestLine.isHttp10()) {
            //now just forbidden http 1.0
            logger.error("Http 1.0 not supported");
            return;
        }

        String method = requestLine.getMethod();
        String path = requestLine.getPath();
        if (method.equalsIgnoreCase("CONNECT")) {
            while (true) {
                String line = input.readLine();
                if (line == null) {
                    logger.error("unexpected EOF when read client connect request");
                    return;
                }
                if (line.isEmpty()) {
                    break;
                }
            }
            String target = requestLine.getPath();
            logger.debug("Receive connect request to {}", target);
            String host = NetUtils.getHost(target);
            int port = NetUtils.getPort(target);
            // just tell client ok..
            HttpOutputStream output = new HttpOutputStream(serverSocket.getOutputStream());
            output.writeLine("HTTP/1.1 200 OK\r\n");
            output.flush();
            Handler handler = new ConnectProxyHandler(serverSocket, messageListener, host, port, sslContextManager,
                    executor);
            handler.handle();
        } else if (path.startsWith("/")) {
            Handler handler = new HttpRequestHandler(serverSocket, new HttpInputStream(input), sslContextManager);
            handler.handle();
        } else {
            Handler handler = new CommonProxyHandler(serverSocket, new HttpInputStream(input), messageListener);
            handler.handle();
        }
    }
}
