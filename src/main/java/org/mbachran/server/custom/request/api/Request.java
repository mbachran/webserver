package org.mbachran.server.custom.request.api;

import javax.annotation.Nonnull;

/**
 * The interface for the request to be constructed by parsers and to be given to handlers through the SPI.
 */
public interface Request
{
    /**
     * @return The {@link RequestLine} of the request.
     */
    @Nonnull
    RequestLine getRequestLine();

    /**
     * @return The {@link RequestHeaders} of the request.
     */
    @Nonnull
    RequestHeaders getRequestHeaders();

    /**
     * @return The {@link RequestBody} of the request.
     */
    @Nonnull
    RequestBody getRequestBody();
}
