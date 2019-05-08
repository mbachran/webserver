package org.mbachran.server.custom.dispatch.impl;

import org.mbachran.server.custom.HttpVersion;
import org.mbachran.server.custom.dispatch.api.Dispatcher;
import org.mbachran.server.custom.request.api.Request;
import org.mbachran.server.custom.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.List;

import static org.mbachran.server.custom.HttpCode.HTTP_VERSION_NOT_SUPPORTED;
import static org.mbachran.server.custom.dispatch.impl.DefaultDispatcher.NAME;

/**
 * This is the {@link Dispatcher} wired into the {@link org.mbachran.server.custom.connection.api.Connection}  by default.
 * It selects a handler for the given HTTP version and forwards.
 */
@Component(NAME)
public class DefaultDispatcher implements Dispatcher
{
    private static final Logger LOG = LoggerFactory.getLogger(DefaultDispatcher.class);

    /**
     * Next in chain: select {@link HttpVersionHandler}.
     */
    private final EnumMap<HttpVersion, HttpVersionHandler> versionHandlers = new EnumMap<>(HttpVersion.class);

    static final String NAME = "defaultDispatcher";

    public DefaultDispatcher(@Nonnull final List<HttpVersionHandler> versionHandlers)
    {
        versionHandlers.forEach(h -> this.versionHandlers.put(h.getHttpVersion(), h));
    }

    @Override
    @Nonnull
    public String getName()
    {
        return NAME;
    }

    @Override
    @Nonnull
    public Response handle(@Nonnull final Request request) throws Exception
    {
        LOG.debug("Handling request: " + request);
        final HttpVersion version = request.getRequestLine().getVersion();
        final HttpVersionHandler httpVersionHandler = versionHandlers.get(version);
        if (httpVersionHandler == null)
        {
            return Response.buildErrorResponse(HTTP_VERSION_NOT_SUPPORTED, "Unsupported HTTP version: " + version.getValue());
        }

        return httpVersionHandler.handle(request);
    }
}
