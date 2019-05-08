package org.mbachran.server.custom.dispatch.impl;

import org.mbachran.server.custom.HttpCode;
import org.mbachran.server.custom.HttpVersion;
import org.mbachran.server.custom.request.api.Request;
import org.mbachran.server.custom.response.Response;
import org.mbachran.server.custom.util.Delimiters;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mbachran.server.custom.dispatch.impl.HttpVersion10Handler.NAME;

/**
 * Copy of the 1.1 handle to respect the backwards compatibility needs.
 * Should rather be changed for one handler to support multiple versions instead of the code copy.
 */
@Component(NAME)
public class HttpVersion10Handler implements HttpVersionHandler
{
    private final Map<String, ContentTypeHandler> contentTypeHandlers = new HashMap<>();

    static final String NAME = "httpVersion10Handler";

    public HttpVersion10Handler(@Nonnull final List<ContentTypeHandler> contentTypeHandlers)
    {
        for (final ContentTypeHandler contentTypeHandler : contentTypeHandlers)
        {
            for (final String supportedContentType : contentTypeHandler.getSupportedContentTypes())
            {
                this.contentTypeHandlers.put(supportedContentType, contentTypeHandler);
            }
        }
    }


    @Override
    @Nonnull
    public HttpVersion getHttpVersion()
    {
        return HttpVersion.HTTP_1_0;
    }

    @Override
    @Nonnull
    public String getName()
    {
        return NAME;
    }

    @Nonnull
    @Override
    public Response handle(@Nonnull final Request request) throws Exception
    {
        final String contentType = request.getRequestHeaders().getHeader("content-type");
        final String parameterFreeContentType = contentType == null ? "*" : contentType.split(Delimiters.SEMI_COLON)[0];
        final ContentTypeHandler contentTypeHandler = contentTypeHandlers.get(parameterFreeContentType);
        if (contentTypeHandler == null)
        {
            return Response.buildErrorResponse(HttpCode.UNSUPPORTED_MEDIA_TYPE, "Content type not supported: " + parameterFreeContentType);
        }

        return contentTypeHandler.handle(request);
    }
}
