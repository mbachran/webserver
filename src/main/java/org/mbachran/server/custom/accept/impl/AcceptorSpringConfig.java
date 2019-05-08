package org.mbachran.server.custom.accept.impl;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

import static org.mbachran.server.custom.accept.impl.AcceptorConfig.CONNECTION_POOL_QUALIFIER;

/**
 * Provides the Acceptor related beans and the injected configurations
 * avoiding the value annotation literals to be used in more than one place.
 * All injected application properties hav default values making the properties optional.
 */
@Configuration
public class AcceptorSpringConfig
{
    private final AcceptorConfig acceptorConfig;

    @Autowired
    public AcceptorSpringConfig(@Value("${application.config.custom-server.connection.min-count:5}") final int minConnectionCount,
                                @Value("${application.config.custom-server.connection.max-count:100}") final int maxConnectionCount,
                                @Value("${application.config.custom-server.connection.keep-alive-time-seconds:10}") final int keepAliveTime)
    {
        acceptorConfig = new AcceptorConfig(minConnectionCount,maxConnectionCount, keepAliveTime);
    }

    /**
     * @return The bean to retrieve the connection threads from. Threads will return as soon as the connection is
     * not kept alive.
     */
    @Bean(CONNECTION_POOL_QUALIFIER)
    ExecutorService executorService()
    {
        final int minConnectionCount = acceptorConfig.getMinConnectionCount();
        final int maxConnectionCount = acceptorConfig.getMaxConnectionCount();
        final int keepAliveTime = acceptorConfig.getKeepAliveTime();
        final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(maxConnectionCount);
        final BasicThreadFactory factory = new BasicThreadFactory.Builder().namingPattern("connection-%d").build();
        return new ThreadPoolExecutor(minConnectionCount, maxConnectionCount, keepAliveTime, TimeUnit.SECONDS, queue, factory);
    }

    /**
     * @return The Java Bean holding the Acceptor related application properties.
     */
    @Bean
    AcceptorConfig acceptConfig()
    {
        return acceptorConfig;
    }
}
