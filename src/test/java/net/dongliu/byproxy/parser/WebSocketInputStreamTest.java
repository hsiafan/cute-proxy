package net.dongliu.byproxy.parser;

import org.junit.Test;

/**
 * @author Liu Dong
 */
public class WebSocketInputStreamTest {
    @Test
    public void readMessage() throws Exception {
//        WebSocketInputStream input = new WebSocketInputStream(getClass().getResourceAsStream("/websocket.1"));
//        WebSocketInputStream.Frame frame = input.readMessage();
//        assertNotNull(frame);
//        assertEquals(1, frame.getOpcode());
//        assertTrue(frame.isFin());
//        assertTrue(frame.isMask());
//        String body = bodyToString(frame);
//        assertTrue(body.contains("channel"));
    }


    @Test
    public void readMessage2() throws Exception {
//        WebSocketInputStream input = new WebSocketInputStream(getClass().getResourceAsStream("/websocket.2"));
//        WebSocketInputStream.Frame frame = input.readMessage();
//        assertNotNull(frame);
//        assertEquals(1, frame.getOpcode());
//        assertTrue(frame.isFin());
//        assertFalse(frame.isMask());
//        String body = bodyToString(frame);
//        assertTrue(body.startsWith("[{\"ext\":{\"timesync\":"));
    }


//    private String bodyToString(WebSocketFrame frame) throws IOException {
//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//        frame.copyTo(bos);
//        return bos.toString("UTF-8");
//    }

}