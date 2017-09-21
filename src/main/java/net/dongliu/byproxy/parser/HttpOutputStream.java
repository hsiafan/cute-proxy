package net.dongliu.byproxy.parser;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Output stream for http
 *
 * @author Liu Dong
 */
@ThreadSafe
public class HttpOutputStream extends OutputStream {
    private final OutputStream output;
    private volatile boolean closed;

    public HttpOutputStream(OutputStream output) {
        this.output = output;
    }

    /**
     * Output http response headers
     */
    public synchronized void writeResponseHeaders(HttpResponseHeader headers) throws IOException {
        writeLine(headers.getStatusLine().raw());
        writeHeaders(headers.getHeaders());
    }

    /**
     * Output http request headers
     */
    public synchronized void writeRequestHeaders(HttpRequestHeader headers) throws IOException {
        writeLine(headers.getRequestLine().raw());
        writeHeaders(headers.getHeaders());
    }

    /**
     * Write one http header
     */
    public synchronized void writeHeaders(List<Header> headers) throws IOException {
        for (Header header : headers) {
            writeLine(header.raw());
        }
        output.write('\r');
        output.write('\n');
    }

    public synchronized void writeRawHeaders(List<String> lines) throws IOException {
        for (String header : lines) {
            writeLine(header);
        }
        output.write('\r');
        output.write('\n');
    }

    public synchronized void writeLine(String line) throws IOException {
        output.write(line.getBytes(US_ASCII));
        output.write('\r');
        output.write('\n');
    }

    @Override
    public synchronized void write(int b) throws IOException {
        output.write(b);
    }

    @Override
    public synchronized void write(byte[] b) throws IOException {
        output.write(b);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        output.write(b, off, len);
    }

    @Override
    public synchronized void flush() throws IOException {
        output.flush();
    }

    @Override
    public synchronized void close() throws IOException {
        output.close();
        closed = true;
    }

    public synchronized boolean isClosed() {
        return closed;
    }

}
