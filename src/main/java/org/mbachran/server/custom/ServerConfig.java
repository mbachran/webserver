package org.mbachran.server.custom;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Java Bean providing the Server related application properties after they have been injected via the
 * value annotations at one place only.
 */
class ServerConfig
{
    static final String ACCEPT_POOL_QUALIFIER = "acceptPool";

    private final int port;

    private final int acceptorCount;

    private final int acceptBacklog;

    private final String networkInterface;

    ServerConfig(final int port, final int acceptorCount, final int acceptBacklog, @Nonnull final String networkInterface)
    {
        this.port = port;
        this.acceptorCount = acceptorCount;
        this.acceptBacklog = acceptBacklog;
        this.networkInterface = Objects.requireNonNull(networkInterface).trim();
    }

    /**
     * @return The port the web server should listen to.
     */
    int getPort()
    {
        return port;
    }

    /**
     * @return The number of acceptor thread to accept connections on the server socket.
     */
    int getAcceptorCount()
    {
        return acceptorCount;
    }

    /**
     * @return The accept backlog on the server socket.
     */
    int getAcceptBacklog()
    {
        return acceptBacklog;
    }

    /**
     * @return The interface to bind the server socket to.
     */
    @Nonnull
    String getInterface()
    {
        return networkInterface;
    }
}
