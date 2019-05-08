package org.mbachran.server.custom.accept.api;

import org.mbachran.server.custom.connection.api.Connection;

/**
 * This role is meant to accept on the server socket and dispatch {@link Connection}s for handling
 * data sockets.
 *
 * Acceptor is a {@link java.util.concurrent.Callable} that obeys to the contract of the {@link org.mbachran.server.custom.util.SurvivingRunnable}.
 * The interface allows implementations to throw any Exception.
 */
@FunctionalInterface
public interface Acceptor
{
    /**
     *
     * @return The indication on whether to keep the thread running and for how long to delay the next call to this callable.
     * @throws Exception If any Exception occurred like I/O or interruption.
     */
    Long accept() throws Exception;
}
