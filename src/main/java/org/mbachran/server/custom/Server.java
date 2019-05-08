package org.mbachran.server.custom;

import org.mbachran.server.custom.accept.api.Acceptor;
import org.mbachran.server.custom.accept.api.AcceptorFactory;
import org.mbachran.server.custom.util.SurvivingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.mbachran.server.custom.ServerConfig.ACCEPT_POOL_QUALIFIER;

/**
 * Initializes/starts the custom web server opening the server socket and spawning the acceptor threads.
 */
@Component
public class Server
{
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private final ServerConfig serverConfig;

    private final ExecutorService acceptPool;

    private final AcceptorFactory acceptorFactory;

    private ServerSocketChannel serverSocketChannel;

    @Autowired
    public Server(@Nonnull final ServerConfig serverConfig,
                  @Qualifier(ACCEPT_POOL_QUALIFIER) @Nonnull final ExecutorService acceptPool,
                  @Nonnull AcceptorFactory acceptorFactory)
    {
        this.serverConfig = serverConfig;
        this.acceptPool = acceptPool;
        this.acceptorFactory = acceptorFactory;
    }

    /**
     * Preferably do not do something that might fail like I/O within the constructor.
     *
     * @throws IOException If the port cannot be bound.
     */
    @PostConstruct
    void init() throws IOException
    {
        LOG.info("Starting custom server on port {} for interfaces {} with an accept backlog of {} ...", serverConfig.getPort(),
                serverConfig.getInterface(), serverConfig.getAcceptBacklog());

        // We must not use AutoClosable here as we need to keep the socket open and continue wiring the Spring context. PreDestroy takes care.
        serverSocketChannel = ServerSocketChannel.open();
        LOG.info("Listening to " + serverConfig.getPort());
        serverSocketChannel.bind(new InetSocketAddress(serverConfig.getInterface(), serverConfig.getPort()), serverConfig.getAcceptBacklog());

        for (int acceptorNumber = 0; acceptorNumber < serverConfig.getAcceptorCount(); acceptorNumber++)
        {
            final Acceptor acceptor = acceptorFactory.create(acceptorNumber, serverSocketChannel);
            acceptPool.submit(new SurvivingRunnable(acceptor::accept));
        }
    }

    @PreDestroy
    void shutdown() throws IOException
    {
        serverSocketChannel.close();
    }
}
