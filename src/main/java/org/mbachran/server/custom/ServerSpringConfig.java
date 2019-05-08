package org.mbachran.server.custom;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Nonnull;
import java.util.concurrent.*;

import static org.mbachran.server.custom.ServerConfig.ACCEPT_POOL_QUALIFIER;

/**
 * Provides the Server related beans and the injected configurations
 * avoiding the value annotation literals to be used in more than one place.
 * All injected application properties hav default values making the properties optional.
 */
@Configuration
public class ServerSpringConfig
{
    private final ServerConfig serverConfig;

    @Autowired
    public ServerSpringConfig(@Value("${application.config.custom-server.port:7070}") final int port,
                              @Value("${application.config.custom-server.acceptor.count:1}") final int acceptorCount,
                              @Value("${application.config.custom-server.acceptor.backlog:100}") final int acceptBacklog,
                              @Value("${application.config.custom-server.bind.networkInterface:localhost}") @Nonnull final String networkInterface)
    {
        serverConfig = new ServerConfig(port, acceptorCount, acceptBacklog, networkInterface);
    }

    /**
     * @return The bean to retrieve the acceptor threads from. The acceptor threads will never return to the pool
     * unless they fail and will not survive or until the server is shutdown.
     */
    @Bean(ACCEPT_POOL_QUALIFIER)
    ExecutorService executorService()
    {
        final int acceptorCount = serverConfig.getAcceptorCount();
        final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(acceptorCount);
        final BasicThreadFactory factory = new BasicThreadFactory.Builder().namingPattern("acceptor-%d").build();
        return new ThreadPoolExecutor(acceptorCount, acceptorCount, 10L, TimeUnit.SECONDS, queue, factory);
    }

    /**
     * @return The Java Bean holding the Server related application properties.
     */
    @Bean
    ServerConfig serverConfig()
    {
        return serverConfig;
    }
}
