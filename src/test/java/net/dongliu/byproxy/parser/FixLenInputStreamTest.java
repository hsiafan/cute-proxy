package net.dongliu.byproxy.parser;

import net.dongliu.commons.io.InputStreams;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

/**
 * @author Liu Dong
 */
public class FixLenInputStreamTest {
    @Test
    public void read() throws Exception {
        String s = "require('net').createServer(function(sock) {\n" +
                "    sock.on('data', function(data) {\n" +
                "        sock.write('HTTP/1.1 200 OK\\r\\n');\n" +
                "        sock.write('\\r\\n');\n" +
                "        sock.write('hello world!');\n" +
                "        sock.destroy();\n" +
                "    });\n" +
                "}).listen(9090, '127.0.0.1');";
        ByteArrayInputStream bos = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
        FixLenInputStream input = new FixLenInputStream(bos, 64);
        byte[] bytes = InputStreams.readAll(input);
        assertEquals(64, bytes.length);
        assertEquals("require('net').createServer(function(sock) {\n    sock.on('data',", new String(bytes));
        assertEquals(s.length() - 64, InputStreams.readAll(bos).length);
    }


    @Test
    public void skip() throws Exception {

    }

    @Test
    public void available() throws Exception {

    }

    @Test
    public void close() throws Exception {

    }

}