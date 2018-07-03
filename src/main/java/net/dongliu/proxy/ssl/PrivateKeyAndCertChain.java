package net.dongliu.proxy.ssl;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class PrivateKeyAndCertChain {

    private final PrivateKey privateKey;
    private final X509Certificate[] certificateChain;

    PrivateKeyAndCertChain(PrivateKey privateKey, X509Certificate[] certificateChain) {
        this.privateKey = privateKey;
        this.certificateChain = certificateChain;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public X509Certificate[] getCertificateChain() {
        return certificateChain;
    }
}
