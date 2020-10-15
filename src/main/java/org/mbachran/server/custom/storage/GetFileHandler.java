package org.mbachran.server.custom.storage;

import org.mbachran.server.custom.HttpCode;
import org.mbachran.server.custom.request.api.Request;
import org.mbachran.server.custom.response.Response;
import org.mbachran.server.custom.spi.NamedHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.io.IOException;
 
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Configured in (application.config.custom-server.spi.handler.get) handler for GET requests.
 * Uses {@link FilePersistence} as simple storage.
 */
@Component
public class GetFileHandler implements NamedHandler
{
    private static final Logger LOG = LoggerFactory.getLogger(GetFileHandler.class);

    private final FilePersistence filePersistence;

    @Autowired
    public GetFileHandler(@Nonnull final FilePersistence filePersistence)
    {
        this.filePersistence = filePersistence;
    }

    @Nonnull
    @Override
    public Response handle(@Nonnull final Request request)
    {
        final byte[] body;
        try
        {
            body = filePersistence.readBinary(request.getRequestLine().getUri().getPath());
            logContentInfo(body);
        }
        catch (IOException e)
        {
            LOG.warn("Failed reading resource: {}", e.getMessage());
            return Response.buildErrorResponse(HttpCode.NOT_FOUND);
        }

        return new Response.Builder().addHeader("Content-Length", String.valueOf(body.length)).body(body).build();
    }

    private static void logContentInfo(@Nonnull final byte[] body)
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug("Read body content (up to 100 bytes in utf-8): " + new String(body, 0, Math.min(body.length, 100), UTF_8));
        }
        else
        {
            LOG.info("Read body content of length: " + body.length);
        }
    }

    @Nonnull
    @Override
    public String getName()
    {
        return "file-storage-get";
    }
}
