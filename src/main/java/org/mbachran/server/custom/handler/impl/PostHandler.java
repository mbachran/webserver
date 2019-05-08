package org.mbachran.server.custom.handler.impl;

import org.mbachran.server.custom.handler.api.MethodHandler;
import org.mbachran.server.custom.request.api.Method;
import org.mbachran.server.custom.request.api.Request;
import org.mbachran.server.custom.response.Response;
import org.mbachran.server.custom.spi.NamedHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Delegates to a configurable handler.
 *
 * See application.properties -  application.config.custom-server.spi.handler.<method> regarding handler selection.
 */
@Component
public class PostHandler implements MethodHandler
{
    private static final Logger LOG = LoggerFactory.getLogger(PostHandler.class);

    private final NamedHandler namedHandler;

    @Autowired
    public PostHandler(@Nonnull final List<NamedHandler> namedHandlers,
                       @Value("${application.config.custom-server.spi.handler.post}") @Nonnull final String handlerName)
    {
        final List<NamedHandler> handlers = namedHandlers.stream().filter(h -> h.getName().equals(handlerName)).collect(Collectors.toList());
        if (handlers.size() == 0)
        {
            final String msg = getMethod() + " handler not found for name: " + handlerName;
            LOG.error(msg);
            throw new IllegalStateException(msg);
        }

        if (handlers.size() > 1)
        {
            String msg = getMethod() + " handler configured more than once for name: " + handlerName;
            LOG.error(msg);
            throw new IllegalStateException(msg);
        }

        namedHandler = handlers.get(0);
        LOG.info(getMethod() + " method uses handler {} of type {}", handlerName, namedHandler.getClass().getName());
    }

    @Nonnull
    @Override
    public Method getMethod()
    {
        return Method.POST;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return getClass().getSimpleName();
    }

    @Nonnull
    @Override
    public Response handle(@Nonnull Request request) throws Exception
    {
        LOG.info("POST uri=" + request.getRequestLine().getUri());
        return namedHandler.handle(request);
    }
}
