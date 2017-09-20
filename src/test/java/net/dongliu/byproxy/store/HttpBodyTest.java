package net.dongliu.byproxy.store;

import com.google.common.io.ByteStreams;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

/**
 * @author Liu Dong
 */
public class HttpBodyTest {
    @Test
    @Ignore
    public void write() throws Exception {
        HttpBody httpBody = new HttpBody(null, null, null);
        byte[] data = new byte[1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        for (int i = 0; i < 520; i++) {
            httpBody.append(ByteBuffer.wrap(data));
        }
        httpBody.finish();
        InputStream inputStream = httpBody.getDecodedInputStream();
        byte[] bytes = ByteStreams.toByteArray(inputStream);
        assertEquals(520 * 1024, bytes.length);
    }

}