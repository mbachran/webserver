package org.mbachran.server.custom.accept.api;

import javax.annotation.Nonnull;
import java.nio.channels.ServerSocketChannel;

/**
 * Factory/provider that helps with assisted injection.
 * Service provide approach is probably over sized here. But Spring can overwrite implementations via @Primary which can be handy for testing.
 */
public interface AcceptorFactory
{
    /**
     * Constructs an {@link Acceptor} based on a number that tells the Acceptor its identity and the server socket to accept on.
     *
     * @param acceptorNumber      The identity of the {@link Acceptor}.
     * @param serverSocketChannel The server socket channel to accept on.
     * @return The factored {@link Acceptor}.
     */
    @Nonnull
    Acceptor create(int acceptorNumber, @Nonnull ServerSocketChannel serverSocketChannel);
}
