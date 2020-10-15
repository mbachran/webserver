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
import java.net.URI;
 
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Configured in (application.config.custom-server.spi.handler.put) handler for PUT requests.
 * Uses {@link FilePersistence} as simple storage.
 */
@Component
public class PutFileHandler implements NamedHandler
{
    private static final Logger LOG = LoggerFactory.getLogger(PutFileHandler.class);

    private final FilePersistence filePersistence;

    @Autowired
    public PutFileHandler(@Nonnull final FilePersistence filePersistence)
    {
        this.filePersistence = filePersistence;
    }

    @Nonnull
    @Override
    public Response handle(@Nonnull final Request request)
    {
        final URI uri = request.getRequestLine().getUri();
        try
        {
            final byte[] body = request.getRequestBody().getContent();
            filePersistence.writeBinary(uri.getPath(), body);
            logContentInfo(body);
        }
        catch (IOException e)
        {
            LOG.error("Failed writing to resource: " + uri, e);
            return Response.buildErrorResponse(HttpCode.NOT_FOUND);
        }

        return new Response.Builder().code(HttpCode.CREATED).addHeader("Content-Length", String.valueOf(0)).build();
    }

    private static void logContentInfo(@Nonnull final byte[] body)
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug("Wrote body content (up to 100 bytes in utf-8): " + new String(body, 0, Math.min(body.length, 100), UTF_8));
        }
        else
        {
            LOG.info("Wrote body content of length: " + body.length);
        }
    }

    @Nonnull
    @Override
    public String getName()
    {
        return "file-storage-put";
    }
}
