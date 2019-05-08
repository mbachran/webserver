package org.mbachran.server.custom.accept.impl;

import org.mbachran.server.custom.accept.api.Acceptor;
import org.mbachran.server.custom.accept.api.AcceptorFactory;
import org.mbachran.server.custom.connection.api.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutorService;

import static org.mbachran.server.custom.accept.impl.AcceptorConfig.CONNECTION_POOL_QUALIFIER;

/**
 * Default assisted inject behavior creating an {@link Acceptor} as {@link DefaultAcceptor} combining injected parameters
 * with runtime parameters.
 */
@Component
public class DefaultAcceptorFactory implements AcceptorFactory
{
    private final ExecutorService connectionPool;

    private final ConnectionFactory connectionFactory;

    @Autowired
    public DefaultAcceptorFactory(@Qualifier(CONNECTION_POOL_QUALIFIER) @Nonnull final ExecutorService connectionPool,
                                  @Nonnull final ConnectionFactory connectionFactory)
    {
        this.connectionPool = connectionPool;
        this.connectionFactory = connectionFactory;
    }

    @Nonnull
    @Override
    public Acceptor create(final int acceptorNumber, @Nonnull final ServerSocketChannel serverSocketChannel)
    {
        return new DefaultAcceptor(connectionPool, connectionFactory, acceptorNumber, serverSocketChannel);
    }
}
