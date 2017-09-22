package net.dongliu.byproxy.ssl;

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
 * @author Liu Dong
 */
public class SSLContextManager {

    private static Logger logger = LoggerFactory.getLogger(SSLContextManager.class);

    private String keyStorePath;
    private AppKeyStoreGenerator appKeyStoreGenerator;
    private BigInteger lastCaCertSerialNumber;
    // ssl context cache
    private final ConcurrentHashMap<String, SSLContext> sslContextCache = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public SSLContextManager(String keyStorePath, char[] keyStorePassword) {
        this.keyStorePath = keyStorePath;
        long start = System.currentTimeMillis();
        AppKeyStoreGenerator appKeyStoreGenerator;
        try {
            appKeyStoreGenerator = new AppKeyStoreGenerator(keyStorePath, keyStorePassword);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        logger.info("Initialize AppKeyStoreGenerator cost {} ms", System.currentTimeMillis() - start);
        BigInteger caCertSerialNumber = appKeyStoreGenerator.getCACertSerialNumber();

        lock.writeLock().lock();
        try {
            if (caCertSerialNumber.equals(lastCaCertSerialNumber)) {
                // do nothing
                return;
            }
            this.appKeyStoreGenerator = appKeyStoreGenerator;
            this.lastCaCertSerialNumber = caCertSerialNumber;
            this.sslContextCache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public SSLContext createSSlContext(String host) {
        lock.readLock().lock();
        try {
            return sslContextCache.computeIfAbsent(host, host1 -> {
                try {
                    return getSslContextInner(host1);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } finally {
            lock.readLock().unlock();
        }
    }

    private SSLContext getSslContextInner(String host) throws Exception {
        char[] appKeyStorePassword = "123456".toCharArray();
        long start = System.currentTimeMillis();
        KeyStore keyStore = appKeyStoreGenerator.generateKeyStore(host, 365, appKeyStorePassword);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, appKeyStorePassword);
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(keyManagers, null, new SecureRandom());
        logger.info("Create ssh context for {}, cost {} ms", host, System.currentTimeMillis() - start);
        return sslContext;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public AppKeyStoreGenerator getAppKeyStoreGenerator() {
        return appKeyStoreGenerator;
    }
}
