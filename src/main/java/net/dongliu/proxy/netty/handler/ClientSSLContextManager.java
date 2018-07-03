package net.dongliu.proxy.netty.handler;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import net.dongliu.proxy.exception.SSLContextException;

import javax.net.ssl.SSLException;
import java.util.function.Supplier;

public class ClientSSLContextManager implements Supplier<SslContext> {

    private SslContext context = createNettyClientSSlContext();
    private static ClientSSLContextManager instance = new ClientSSLContextManager();

    public static ClientSSLContextManager getInstance() {
        return instance;
    }

    @Override
    public SslContext get() {
        return context;
    }

    private static SslContext createNettyClientSSlContext() {
        try {
            return SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                            ApplicationProtocolConfig.Protocol.ALPN,
                            SelectorFailureBehavior.NO_ADVERTISE,
                            SelectedListenerFailureBehavior.ACCEPT,
                            ApplicationProtocolNames.HTTP_2,
                            ApplicationProtocolNames.HTTP_1_1))
                    .build();
        } catch (SSLException e) {
            throw new SSLContextException(e);
        }
    }

}
