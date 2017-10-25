package net.dongliu.byproxy.ssl;

import net.dongliu.byproxy.setting.Settings;
import net.dongliu.byproxy.utils.Networks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Hold current root cert and cert generator
 *
 * @author Liu Dong
 */
public class SSLContextManager {

    private static Logger logger = LoggerFactory.getLogger(SSLContextManager.class);

    private String keyStorePath;
    private KeyStoreGenerator keyStoreGenerator;
    private BigInteger lastRootCertSN;
    // ssl context cache
    private final ConcurrentHashMap<String, SSLContext> sslContextCache = new ConcurrentHashMap<>();
    // guard for set new root cert
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public SSLContextManager(String keyStorePath, char[] keyStorePassword) {
        this.keyStorePath = keyStorePath;
        long start = System.currentTimeMillis();
        KeyStoreGenerator keyStoreGenerator;
        try {
            keyStoreGenerator = new KeyStoreGenerator(keyStorePath, keyStorePassword);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        logger.info("Initialize KeyStoreGenerator cost {} ms", System.currentTimeMillis() - start);
        BigInteger rootCertSN = keyStoreGenerator.getRootCertSN();

        lock.writeLock().lock();
        try {
            if (rootCertSN.equals(lastRootCertSN)) {
                // do nothing
                return;
            }
            this.keyStoreGenerator = keyStoreGenerator;
            this.lastRootCertSN = rootCertSN;
            this.sslContextCache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Create ssl context for the host
     */
    public SSLContext createSSlContext(String host) {
        host = Networks.wildcardHost(host);
        lock.readLock().lock();
        try {
            return sslContextCache.computeIfAbsent(host, h -> {
                try {
                    return getSslContextInner(h);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } finally {
            lock.readLock().unlock();
        }
    }

    private SSLContext getSslContextInner(String host) throws Exception {
        char[] keyStorePassword = Settings.keyStorePassword;
        long start = System.currentTimeMillis();
        KeyStore keyStore = keyStoreGenerator.generateKeyStore(host, Settings.certValidityDays, keyStorePassword);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keyStorePassword);
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(keyManagers, null, new SecureRandom());
        logger.info("Create ssh context for {}, cost {} ms", host, System.currentTimeMillis() - start);
        return sslContext;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public KeyStoreGenerator getKeyStoreGenerator() {
        return keyStoreGenerator;
    }
}
