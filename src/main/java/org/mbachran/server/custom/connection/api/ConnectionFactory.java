package org.mbachran.server.custom.connection.api;

import javax.annotation.Nonnull;
import java.nio.channels.SocketChannel;

/**
 * Factory for assisted inject for {@link Connection} which are stateful as factored from data sockets at runtime.
 */
public interface ConnectionFactory
{
    @Nonnull
    Connection create(long creationTime, @Nonnull SocketChannel dataSocketChannel);
}
