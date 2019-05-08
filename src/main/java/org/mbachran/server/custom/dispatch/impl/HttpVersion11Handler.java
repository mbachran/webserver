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

import static org.mbachran.server.custom.dispatch.impl.HttpVersion11Handler.NAME;

/**
 * Handles HTTP 1.1 requests and forwards to a {@link ContentTypeHandler} based on the requests content type.
 * Any content type parameters are ignored for selection.
 * Missing content type uses '*' for selection.
 * application.properties can include and according value for 'application.config.custom-server.supported.content.type.text'.
 */

@Component(NAME)
public class HttpVersion11Handler implements HttpVersionHandler
{
    private final Map<String, ContentTypeHandler> contentTypeHandlers = new HashMap<>();

    static final String NAME = "httpVersion11Handler";

    public HttpVersion11Handler(@Nonnull final List<ContentTypeHandler> contentTypeHandlers)
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
        return HttpVersion.HTTP_1_1;
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
        final ContentTypeHandler contentTypeHandler = contentTypeHandlers.get(parameterFreeContentType.trim());
        if (contentTypeHandler == null)
        {
            return Response.buildErrorResponse(HttpCode.UNSUPPORTED_MEDIA_TYPE, "Content type not supported: " + parameterFreeContentType);
        }

        return contentTypeHandler.handle(request);
    }
}
