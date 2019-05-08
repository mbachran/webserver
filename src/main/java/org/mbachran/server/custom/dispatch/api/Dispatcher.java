package org.mbachran.server.custom.dispatch.api;

import org.mbachran.server.custom.request.api.Request;
import org.mbachran.server.custom.response.Response;

import javax.annotation.Nonnull;

/**
 * Interface to support a command chain of handlers that can dispatch further after handling the aspect they are responsible for.
 */
public interface Dispatcher
{
    @Nonnull
    String getName();

    /**
     * The method to build the command chain of Dispatchers.
     *
     * @param request The {@link Request} to dispatch.
     * @return The {@link Response} produced.
     * @throws Exception Any exception allowed to be thrown up through the chain as implementations of handlers cannot be foreseen.
     */
    @Nonnull
    Response handle(@Nonnull Request request) throws Exception;
}
