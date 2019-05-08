package org.mbachran.server.custom.dispatch.impl;

import org.mbachran.server.custom.HttpVersion;
import org.mbachran.server.custom.dispatch.api.Dispatcher;

import javax.annotation.Nonnull;

/**
 * Interface for handlers that claim to support a certain HTTP version.
 */
public interface HttpVersionHandler extends Dispatcher
{
    /**
     * Here I missed to support multiple which is why the handler has been duplicated.
     * Should be either switched to multi support or the implementations might reuse code by inheritance or delegation.
     *
     * @return The HTTP version supported by the handler.
     */
    @Nonnull
    HttpVersion getHttpVersion();
}
