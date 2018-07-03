package net.dongliu.proxy.store;

import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

/**
 * @author Liu Dong
 */
public class BodyTest {
    @Test
    @Ignore
    public void write() throws Exception {
        Body body = new Body(BodyType.binary, null, "");
        byte[] data = new byte[1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        for (int i = 0; i < 520; i++) {
            body.append(ByteBuffer.wrap(data));
        }
        body.finish();
        InputStream inputStream = body.getDecodedInputStream();
        byte[] bytes = inputStream.readAllBytes();
        assertEquals(520 * 1024, bytes.length);
    }

}