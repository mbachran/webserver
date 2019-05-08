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
import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Configured in (application.config.custom-server.spi.handler.post) handler for POST requests.
 * Uses {@link FilePersistence} as simple storage.
 */
@Component
public class PostFileHandler implements NamedHandler
{
    private static final Logger LOG = LoggerFactory.getLogger(PostFileHandler.class);

    private final FilePersistence filePersistence;

    @Autowired
    public PostFileHandler(@Nonnull final FilePersistence filePersistence)
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
            final String resourcePath = uri.getPath();

            // interpret POST as update in case of json resource
            if (resourcePath.endsWith(".json"))
            {
                final boolean created = filePersistence.createOrUpdateJson(resourcePath, new String(body, UTF_8));
                if (created)
                {
                    logContentInfo(body, "Created JSON with");
                    return new Response.Builder().code(HttpCode.CREATED).addHeader("Content-Length", String.valueOf(0)).build();
                }
                else
                {
                    logContentInfo(body, "Updated JSON with");
                    final byte[] updated = filePersistence.readBinary(resourcePath);
                    return new Response.Builder()
                            .code(HttpCode.OK)
                            .addHeader("Content-Length", String.valueOf(updated.length))
                            .body(updated)
                            .build();
                }
            }
            else
            {
                filePersistence.writeBinary(resourcePath, body);
                logContentInfo(body, "Wrote");
                return new Response.Builder().code(HttpCode.CREATED).addHeader("Content-Length", String.valueOf(0)).build();
            }
        }
        catch (IOException e)
        {
            LOG.error("Failed writing to resource: " + uri, e);
            return Response.buildErrorResponse(HttpCode.NOT_FOUND);
        }
    }

    private static void logContentInfo(@Nonnull final byte[] body, @Nonnull final String operation)
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug(operation + " body content (up to 100 bytes in utf-8): " + new String(body, 0, Math.min(body.length, 100), UTF_8));
        }
        else
        {
            LOG.info(operation + " body content of length: " + body.length);
        }
    }

    @Nonnull
    @Override
    public String getName()
    {
        return "file-storage-post";
    }
}
