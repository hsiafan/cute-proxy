package net.dongliu.byproxy.parser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Utils to read ssl handshake messages
 *
 * @author Liu Dong
 */
public class TLSInputStream extends RichInputStream implements NumberReader {

    public TLSInputStream(InputStream in) {
        super(in);
    }

    /**
     * Read TLSPlaintext Header
     */
    public TLSPlaintextHeader readPlaintextHeader() throws IOException {
        int contentType = read();
        int majorVersion = read();
        int minorVersion = read();
        int length = readUInt16();
        return new TLSPlaintextHeader(contentType, majorVersion, minorVersion, length);
    }


    /**
     * Read one TLS handshake message. Now only support helloRequest, clientHello, serverHello
     *
     * @throws IOException
     */
    public HandShakeMessage<?> readHandShakeMessage() throws IOException {
        int messageType = read();
        int length = readUInt24();
        if (messageType == HandShakeMessage.HELLO_REQUEST) {
            return new HandShakeMessage<>(messageType, length, readHelloRequest(length));
        } else if (messageType == HandShakeMessage.CLIENT_HELLO) {
            return new HandShakeMessage<>(messageType, length, readClientHello(length));
        } else if (messageType == HandShakeMessage.SERVER_HELLO) {
            return new HandShakeMessage<>(messageType, length, readServerHello(length));
        }
        throw new UnsupportedOperationException();
    }

    private HelloRequest readHelloRequest(int length) throws IOException {
        return new HelloRequest();
    }

    private ServerHello readServerHello(int length) throws IOException {
        List<String> alpnNames = readHello(length);
        return new ServerHello(alpnNames);
    }

    private ClientHello readClientHello(int length) throws IOException {
        List<String> alpnNames = readHello(length);
        return new ClientHello(alpnNames);
    }

    private List<String> readHello(int length) throws IOException {
        int majorVersion = read();
        int minorVersion = read();
        byte[] random = readExact(32);
        int sessionIdLen = read();
        byte[] sessionId = readExact(sessionIdLen);
        int cipherSuiteLen = readUInt16();
        byte[] cipherSuite = readExact(cipherSuiteLen);
        int compressionMethodsLen = read();
        byte[] compressionMethods = readExact(compressionMethodsLen);

        List<String> alpnNames = new ArrayList<>();
        int readed = 2 + 32 + 1 + sessionIdLen + 2 + cipherSuiteLen + 1 + compressionMethodsLen;
        if (readed < length) {
            //read extensions
            int extensionsLen = readUInt16();
            int count = 0;
            while (true) {
                Extension extension = readExtension();
                if (extension.isALPN()) {
                    alpnNames = extension.ALPNNames();
                }
                int size = extension.size();
                count += size;
                if (count >= extensionsLen) {
                    break;
                }
            }
        }
        return alpnNames;
    }

    private Extension readExtension() throws IOException {
        int extensionType = readUInt16();
        int extensionDataLen = readUInt16();
        byte[] data = readExact(extensionDataLen);
        return new Extension(extensionType, data);
    }

    public static class TLSPlaintextHeader {
        private static final int CHANGE_CIPHER_SPEC = 20;
        private static final int ALERT = 21;
        private static final int HANDSHAKE = 22;
        private static final int APPLICATION_DATA = 23;

        private final int contentType;
        private final int majorVersion;
        private final int minorVersion;
        private final int length;

        TLSPlaintextHeader(int contentType, int majorVersion, int minorVersion, int length) {
            this.contentType = contentType;
            this.majorVersion = majorVersion;
            this.minorVersion = minorVersion;
            this.length = length;
        }

        public boolean isValidHandShake() {
            return contentType == HANDSHAKE && majorVersion <= 3 && minorVersion <= 3;
        }

        public int getContentType() {
            return contentType;
        }

        public int getMajorVersion() {
            return majorVersion;
        }

        public int getMinorVersion() {
            return minorVersion;
        }

        public int getLength() {
            return length;
        }
    }

    public static class HelloRequest {

    }

    public static class HandShakeMessage<T> {
        private static final int HELLO_REQUEST = 0;
        private static final int CLIENT_HELLO = 1;
        private static final int SERVER_HELLO = 2;
        private static final int CERTIFICATE = 11;
        private static final int SERVER_KEY_EXCHANGE = 12;
        private static final int CERTIFICATE_REQUEST = 13;
        private static final int SERVER_HELLO_DONE = 14;
        private static final int CERTIFICATE_VERIFY = 15;
        private static final int CLIENT_KEY_EXCHANGE = 16;
        private static final int FINISHED = 20;

        private final int type;
        private final int length;
        private final T message;

        public HandShakeMessage(int type, int length, T message) {
            this.type = type;
            this.length = length;
            this.message = message;
        }

        public int getType() {
            return type;
        }

        public int getLength() {
            return length;
        }

        public T getMessage() {
            return message;
        }
    }

    public static class Hello {
        // we just want to get alpn protocol names
        private final List<String> alpnNames;

        public Hello(List<String> alpnNames) {
            this.alpnNames = alpnNames;
        }

        public boolean alpnHas(String protocol) {
            return alpnNames.contains(protocol);
        }

        public List<String> getAlpnNames() {
            return alpnNames;
        }
    }

    public static class ClientHello extends Hello {

        public ClientHello(List<String> alpnNames) {
            super(alpnNames);
        }
    }

    public static class ServerHello extends Hello {

        public ServerHello(List<String> alpnNames) {
            super(alpnNames);
        }
    }

    public static class Extension {
        private static final int application_layer_protocol_negotiation = 16;

        private final int type;
        private final byte[] data;

        public Extension(int type, byte[] data) {
            this.type = type;
            this.data = data;
        }

        public int size() {
            return 4 + data.length;
        }

        public boolean isALPN() {
            return type == application_layer_protocol_negotiation;
        }

        public List<String> ALPNNames() throws IOException {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            int protocolNamesLen = Short.toUnsignedInt(buffer.getShort());
            List<String> protocolNameList = new ArrayList<>(protocolNamesLen);
            int total = 0;
            while (true) {
                int protocolNameLen = Byte.toUnsignedInt(buffer.get());
                byte[] protocolName = new byte[protocolNameLen];
                buffer.get(protocolName);
                String protocol = new String(protocolName);
                protocolNameList.add(protocol);
                total += 2 + protocolNameLen;
                if (total > protocolNamesLen) {
                    break;
                }
            }
            return protocolNameList;
        }
    }
}
