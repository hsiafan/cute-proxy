package net.dongliu.proxy.exception;

/**
 * Thrown when load or create ssl context error
 */
public class SSLContextException extends RuntimeException {
    public SSLContextException(Throwable cause) {
        super(cause);
    }
}
