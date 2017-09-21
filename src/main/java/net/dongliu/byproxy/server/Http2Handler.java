package net.dongliu.byproxy.server;

import net.dongliu.byproxy.parser.HttpInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

/**
 * Handle http2 traffic
 *
 * @author Liu Dong
 */
public class Http2Handler {
    private static Logger logger = LoggerFactory.getLogger(Http2Handler.class);

    private final ExecutorService executor;

    public Http2Handler(ExecutorService executor) {
        this.executor = executor;
    }

    public void handle(HttpInputStream clientIn, OutputStream clientOut, Socket clientSocket,
                       HttpInputStream serverIn, OutputStream serverOut, Socket serverSocket,
                       boolean ssl, String target,
                       @Nullable MessageListener messageListener) throws IOException {

        // header compression http://httpwg.org/specs/rfc7541.html
        // http2 http://httpwg.org/specs/rfc7540.html

        // read client connection preface
        // read server connection preface
        Utils.tunnel(clientIn, clientOut, clientSocket, serverIn, serverOut, serverSocket, executor);
    }


}
