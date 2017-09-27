package net.dongliu.byproxy.ssl;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.function.Supplier;

public class ClientSSLContextFactory implements Supplier<SSLContext> {

    private SSLContext context = createClientSSlContext();
    private static ClientSSLContextFactory instance = new ClientSSLContextFactory();

    public static ClientSSLContextFactory getInstance() {
        return instance;
    }

    @Override
    public SSLContext get() {
        return context;
    }

    private static SSLContext createClientSSlContext() {
        TrustManager[] trustAllManagers = new TrustManager[]{
                new X509TrustManager() {
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }
        };

        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllManagers, new SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
        return sslContext;
    }
}
