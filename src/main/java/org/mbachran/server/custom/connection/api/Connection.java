package org.mbachran.server.custom.connection.api;

/**
 * This role is meant to handle a data socket within its thread and keep the connection alive as long as the HTTP communication demands.
 *
 * Connection represents a {@link java.util.concurrent.Callable} obeying to the contract of {@link org.mbachran.server.custom.util.SurvivingRunnable}.
 *
 * Connection is not allowed to throw an Exception. It is responsible for closing the socket in case or errors.
 */
@FunctionalInterface
public interface Connection
{
    /**
     * Runs a connection/session on a data socket.
     * {@link org.mbachran.server.custom.util.SurvivingRunnable} keeps the connection alive as controlled by the return value of this
     * {@link Connection} as described in the contract of {@link org.mbachran.server.custom.util.SurvivingRunnable}.
     *
     * Each invocation of serve() represents a new request happening on the connection.
     * For one request all data is to be read, handled and responded to on the socket within this one method call.
     *
     * @return Contract as described in the {@link org.mbachran.server.custom.util.SurvivingRunnable}.
     */
    Long serve();
}
