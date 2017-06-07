package net.dongliu.byproxy.exception;

/**
 * @author Liu Dong
 */
public class HttpParserException extends RuntimeException {
    public HttpParserException() {
    }

    public HttpParserException(String message) {
        super(message);
    }

    public HttpParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpParserException(Throwable cause) {
        super(cause);
    }
}
