package net.dongliu.proxy.netty.handler;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import net.dongliu.proxy.exception.SSLContextException;
import net.dongliu.proxy.setting.Settings;
import net.dongliu.proxy.ssl.KeyStoreGenerator;
import net.dongliu.proxy.ssl.PrivateKeyAndCertChain;
import net.dongliu.proxy.utils.Networks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Hold current root cert and cert generator
 *
 * @author Liu Dong
 */
public class ServerSSLContextManager {

    private static Logger logger = LoggerFactory.getLogger(ServerSSLContextManager.class);

    private Path rootKeyStorePath;
    private KeyStoreGenerator keyStoreGenerator;
    private BigInteger lastRootCertSN;
    // ssl context cache
    private final ConcurrentHashMap<String, SslContext> sslContextCache = new ConcurrentHashMap<>();
    // guard for set new root cert
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public ServerSSLContextManager(Path rootKeyStorePath, char[] keyStorePassword) {
        this.rootKeyStorePath = rootKeyStorePath;
        long start = System.currentTimeMillis();
        KeyStoreGenerator keyStoreGenerator;
        try {
            keyStoreGenerator = new KeyStoreGenerator(rootKeyStorePath, keyStorePassword);
        } catch (Exception e) {
            throw new SSLContextException(e);
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
    public SslContext createSSlContext(String host, boolean useH2) {
        String finalHost = Networks.wildcardHost(host);
        lock.readLock().lock();
        try {
            return sslContextCache.computeIfAbsent(host + ":" + useH2, key -> {
                try {
                    return getNettySslContextInner(finalHost, useH2);
                } catch (Exception e) {
                    throw new SSLContextException(e);
                }
            });
        } finally {
            lock.readLock().unlock();
        }
    }

    private SslContext getNettySslContextInner(String host, boolean useH2) throws Exception {
        long start = System.currentTimeMillis();
        PrivateKeyAndCertChain keyAndCertChain = keyStoreGenerator.generateCertChain(host, Settings.certValidityDays);
        logger.debug("Create certificate for {}, cost {} ms", host, System.currentTimeMillis() - start);
        SslContextBuilder builder = SslContextBuilder
                .forServer(keyAndCertChain.privateKey(), keyAndCertChain.certificateChain());
        if (useH2) {
//                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
            builder.applicationProtocolConfig(new ApplicationProtocolConfig(
                    ApplicationProtocolConfig.Protocol.ALPN,
                    SelectorFailureBehavior.NO_ADVERTISE,
                    SelectedListenerFailureBehavior.ACCEPT,
                    ApplicationProtocolNames.HTTP_2,
                    ApplicationProtocolNames.HTTP_1_1));
        }
        return builder.build();
    }

    public Path getRootKeyStorePath() {
        return rootKeyStorePath;
    }

    public KeyStoreGenerator getKeyStoreGenerator() {
        return keyStoreGenerator;
    }
}
