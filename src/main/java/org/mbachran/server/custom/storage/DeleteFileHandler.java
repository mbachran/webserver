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

/**
 * Configured in (application.config.custom-server.spi.handler.delete) handler for DELETE requests.
 * Uses {@link FilePersistence} as simple storage.
 */
@Component
public class DeleteFileHandler implements NamedHandler
{
    private static final Logger LOG = LoggerFactory.getLogger(DeleteFileHandler.class);

    private final FilePersistence filePersistence;

    @Autowired
    public DeleteFileHandler(@Nonnull final FilePersistence filePersistence)
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
            filePersistence.delete(uri.getPath());
            LOG.info("Deleted resource: " + uri);
        }
        catch (IOException e)
        {
            LOG.warn("Failed deleting resource {}: {}", uri, e.getMessage());
            return Response.buildErrorResponse(HttpCode.NOT_FOUND);
        }

        return new Response.Builder().code(HttpCode.NO_CONTENT).build();
    }

    @Nonnull
    @Override
    public String getName()
    {
        return "file-storage-delete";
    }
}
