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
 
/**
 * Configured in (application.config.custom-server.spi.handler.head) handler for HEAD requests.
 * Uses {@link FilePersistence} as simple storage.
 */
@Component
public class HeadFileHandler implements NamedHandler
{
    private static final Logger LOG = LoggerFactory.getLogger(HeadFileHandler.class);

    private final FilePersistence filePersistence;

    @Autowired
    public HeadFileHandler(@Nonnull final FilePersistence filePersistence)
    {
        this.filePersistence = filePersistence;
    }

    @Nonnull
    @Override
    public Response handle(@Nonnull final Request request)
    {
        try
        {
            // just to validate resource existence
            filePersistence.readBinary(request.getRequestLine().getUri().getPath());
        }
        catch (IOException e)
        {
            LOG.warn("Failed reading resource: {}", e.getMessage());
            return Response.buildErrorResponse(HttpCode.NOT_FOUND);
        }

        return new Response.Builder().code(HttpCode.NO_CONTENT).build();
    }

    @Nonnull
    @Override
    public String getName()
    {
        return "file-storage-head";
    }
}
