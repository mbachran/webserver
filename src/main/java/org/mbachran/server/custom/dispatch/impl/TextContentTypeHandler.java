package org.mbachran.server.custom.dispatch.impl;

import org.mbachran.server.custom.HttpCode;
import org.mbachran.server.custom.handler.api.MethodHandler;
import org.mbachran.server.custom.request.api.Method;
import org.mbachran.server.custom.request.api.Request;
import org.mbachran.server.custom.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.List;

import static org.mbachran.server.custom.dispatch.impl.TextContentTypeHandler.NAME;

/**
 * Supports the content types as configured in application.properties under
 * - application.config.custom-server.supported.content.type.text
 * Configuration defaults to 'text/plain' if the property is not given.
 *
 * Dispatched to a {@link MethodHandler} selecting based on the requests {@link Method}.
 */

@Component(NAME)
public class TextContentTypeHandler implements ContentTypeHandler
{
    private final List<String> supportedContentTypes;

    private final EnumMap<Method, MethodHandler> methodHandlers = new EnumMap<>(Method.class);

    static final String NAME = "textContentTypeHandler";

    @Autowired
    public TextContentTypeHandler(@Value("${application.config.custom-server.supported.content.type.text:text/plain}")
                                  @Nonnull final String supportedContentTypes,
                                  @Nonnull final List<MethodHandler> methodHandlers)
    {
        this.supportedContentTypes = List.of(supportedContentTypes.split(","));
        methodHandlers.forEach(h -> this.methodHandlers.put(h.getMethod(), h));
    }

    @Nonnull
    @Override
    public String getName()
    {
        return NAME;
    }

    @Nonnull
    @Override
    public Response handle(@Nonnull final Request request) throws Exception
    {
        final Method method = request.getRequestLine().getMethod();
        final MethodHandler methodHandler = methodHandlers.get(method);
        final Response response;
        if (methodHandler == null)
        {
            response = Response.buildErrorResponse(HttpCode.BAD_REQUEST, "Unsupported method: " + method);
        }
        else
        {
            response = methodHandler.handle(request);
        }

        return response;
    }

    @Nonnull
    @Override
    public List<String> getSupportedContentTypes()
    {
        return supportedContentTypes;
    }
}
