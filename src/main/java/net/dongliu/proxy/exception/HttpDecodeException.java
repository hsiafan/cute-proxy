package net.dongliu.proxy.exception;

/**
 * Thrown when decode http data error
 *
 * @author Liu Dong
 */
public class HttpDecodeException extends RuntimeException {
    public HttpDecodeException() {
    }

    public HttpDecodeException(String message) {
        super(message);
    }

    public HttpDecodeException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpDecodeException(Throwable cause) {
        super(cause);
    }
}
