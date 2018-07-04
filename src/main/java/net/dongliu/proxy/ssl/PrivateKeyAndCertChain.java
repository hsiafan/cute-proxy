package net.dongliu.proxy.ssl;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static java.util.Objects.requireNonNull;

public class PrivateKeyAndCertChain {

    private final PrivateKey privateKey;
    private final X509Certificate[] certificateChain;

    PrivateKeyAndCertChain(PrivateKey privateKey, X509Certificate[] certificateChain) {
        this.privateKey = requireNonNull(privateKey);
        this.certificateChain = requireNonNull(certificateChain);
    }

    public PrivateKey privateKey() {
        return privateKey;
    }

    public X509Certificate[] certificateChain() {
        return certificateChain;
    }
}
