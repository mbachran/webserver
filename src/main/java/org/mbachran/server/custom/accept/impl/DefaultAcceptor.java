package org.mbachran.server.custom.accept.impl;

import org.mbachran.server.custom.accept.api.Acceptor;
import org.mbachran.server.custom.connection.api.Connection;
import org.mbachran.server.custom.connection.api.ConnectionFactory;
import org.mbachran.server.custom.util.SurvivingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Accepts connections and uses the injected {@link ConnectionFactory} to create/dispatch {@link Connection}s based on a SocketChannel
 * and a timestamp directly taken after accept. Dispatch wraps the {@link Connection} into a {@link SurvivingRunnable}.
 * Directly returns into the accept state after dispatching keeping the thread always up.
 */
public class DefaultAcceptor implements Acceptor
{
    private static final Logger LOG = LoggerFactory.getLogger(DefaultAcceptor.class);

    private static final long KEEP_RUNNING = 0L;

    private static final long STOP_RUNNING = -1L;

    private final ExecutorService connectionPool;

    private final ConnectionFactory connectionFactory;

    private final int acceptorNumber;

    private final ServerSocketChannel serverSocketChannel;

    DefaultAcceptor(@Nonnull final ExecutorService connectionPool,
                    @Nonnull final ConnectionFactory connectionFactory,
                    final int acceptorNumber,
                    @Nonnull final ServerSocketChannel serverSocketChannel)
    {
        this.connectionPool = connectionPool;
        this.connectionFactory = connectionFactory;
        this.acceptorNumber = acceptorNumber;
        this.serverSocketChannel = Objects.requireNonNull(serverSocketChannel);
    }

    @Override
    public Long accept() throws IOException
    {
        LOG.info("Acceptor number {} accepting on port {}", acceptorNumber, serverSocketChannel.socket().getLocalPort());
        try
        {
            // Do not use AutoClosable here. The connection is responsible for closing the socket.
            final SocketChannel socketChannel = serverSocketChannel.accept();
            final long creationTime = System.currentTimeMillis();
            final Connection connection = connectionFactory.create(creationTime, socketChannel);
            LOG.debug("Created data connection at {}", creationTime);
            connectionPool.submit(new SurvivingRunnable(connection::serve));

            LOG.debug("Acceptor number {} accepted data connection on port {}", acceptorNumber, serverSocketChannel.socket().getLocalPort());
            return KEEP_RUNNING;
        }
        catch (AsynchronousCloseException e)
        {
            LOG.warn("Shutting down acceptor as the socket is closed!");
            return STOP_RUNNING;
        }
    }
}
