package org.mbachran.server.custom.handler.api;

import org.mbachran.server.custom.dispatch.api.Dispatcher;
import org.mbachran.server.custom.request.api.Method;
import org.mbachran.server.custom.request.api.Request;
import org.mbachran.server.custom.response.Response;

import javax.annotation.Nonnull;

/**
 * Interface for dispatchers that are responsible for a certain HTTP {@link Method}.
 *
 * The wiring code of the implementations is redundant and might be reused. Must be a shared constructor via inheritance though
 * as we want final members after wiring.
 */
public interface MethodHandler extends Dispatcher
{
    /**
     * @return The {@link Method} this handler is capable of handling.
     */
    @Nonnull
    Method getMethod();

    @Nonnull
    Response handle(@Nonnull Request request) throws Exception;
}
