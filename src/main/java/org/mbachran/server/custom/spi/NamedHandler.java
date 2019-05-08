package org.mbachran.server.custom.spi;

import org.mbachran.server.custom.request.api.Request;
import org.mbachran.server.custom.response.Response;

import javax.annotation.Nonnull;

/**
 * Simple SPI interface that can be used to provide handlers per HTTP method.
 * A custom class must be on the classpath and implement this interface to be found via Spring list injection.
 * It is picked up by the corresponding internal handler based on its name which should be configured in the application.properties as:
 * 'application.config.custom-server.spi.handler.<method>=<name>' with <method> being the lower case representation of the method name.
 *
 * NOTE that in fact the {@link Response} and the {@link Request} are part of the SPI though the package structure does not tell so.
 */
public interface NamedHandler
{
    /**
     * @param request The request to handle.
     * @return The {@link Response} to be returned as a reaction to the request.
     * @throws Exception If handling the request fails.
     */
    @Nonnull
    Response handle(@Nonnull Request request) throws Exception;

    /**
     * @return The name of the handler that can be used to configure it as a handler for a certain operation. Within a classpath it should be
     * unique if configured. Otherwise there is a risk of random pickup or error (depending on the selection code).
     * The {@link org.mbachran.server.custom.handler.api.MethodHandler}s provided by default are failing in case of any name collision.
     */
    @Nonnull
    String getName();
}
