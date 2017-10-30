package net.dongliu.byproxy.exception;

/**
 * Thrown when load or create ssl context error
 */
public class SSLContextException extends RuntimeException {
    public SSLContextException(Throwable cause) {
        super(cause);
    }
}
