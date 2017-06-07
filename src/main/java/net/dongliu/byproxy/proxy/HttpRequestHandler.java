package net.dongliu.byproxy.proxy;

import net.dongliu.byproxy.parser.*;
import net.dongliu.commons.collection.Lists;
import net.dongliu.commons.exception.Throwables;
import net.dongliu.commons.io.InputStreams;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.cert.CertificateEncodingException;

/**
 * Handler to server http request
 *
 * @author Liu Dong
 */
public class HttpRequestHandler implements Handler {
    private final Socket serverSocket;
    private final HttpInputStream input;
    private final SSLContextManager sslContextManager;

    public HttpRequestHandler(Socket serverSocket, HttpInputStream input, SSLContextManager sslContextManager) {
        this.serverSocket = serverSocket;
        this.input = input;
        this.sslContextManager = sslContextManager;
    }

    @Override
    public void handle() throws IOException {
        input.enableBuffered();
        HttpOutputStream out = new HttpOutputStream(serverSocket.getOutputStream());
        RequestHeaders headers = input.readRequestHeaders();
        if (headers == null) {
            return;
        }
        RequestLine requestLine = headers.getRequestLine();
        String method = requestLine.getMethod();
        String path = requestLine.getPath();
        if (!method.equals("GET")) {
            return;
        }
        switch (path) {
            case "/":
                sendIndexHtml(out);
                break;
            case "/ByProxy.pem":
                sendPem(out);
                break;
            case "/ByProxy.crt":
                sendCrt(out);
                break;
            default:
        }
    }

    private void sendCrt(HttpOutputStream out) throws IOException {
        AppKeyStoreGenerator appKeyStoreGenerator = sslContextManager.getAppKeyStoreGenerator();
        byte[] data;
        try {
            data = appKeyStoreGenerator.exportCACertificate(false);
        } catch (CertificateEncodingException e) {
            throw Throwables.throwAny(e);
        }
        sendResponse(out, "application/x-x509-ca-cert", data);
    }

    private void sendPem(HttpOutputStream out) throws IOException {
        AppKeyStoreGenerator appKeyStoreGenerator = sslContextManager.getAppKeyStoreGenerator();
        byte[] data;
        try {
            data = appKeyStoreGenerator.exportCACertificate(true);
        } catch (CertificateEncodingException e) {
            throw Throwables.throwAny(e);
        }
        sendResponse(out, "application/x-pem-file", data);
    }

    private void sendIndexHtml(HttpOutputStream out) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/www/html/index.html")) {
            byte[] data = InputStreams.readAll(in);
            sendResponse(out, "text/html; charset=utf-8", data);
        }
    }

    private void sendResponse(HttpOutputStream out, String contentType, byte[] body) throws IOException {
        out.writeLine("HTTP/1.1 200 OK");
        out.writeHeaders(Lists.of(
                new Header("Content-Type", contentType),
                new Header("Content-Length", String.valueOf(body.length)),
                new Header("Connection", "close")
        ));
        out.write(body);
        out.flush();
    }
}
