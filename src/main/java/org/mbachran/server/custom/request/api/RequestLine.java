package org.mbachran.server.custom.request.api;

import org.mbachran.server.custom.HttpVersion;

import javax.annotation.Nonnull;
import java.net.URI;

/**
 * The request line. This is the first line of a request stream up to the \r\n delimiter.
 */
public interface RequestLine
{
    /**
     * @return The HTTP {@link Method} as given by the request line.
     */
    @Nonnull
    Method getMethod();

    /**
     * @return The {@link URI} as given by the request line.
     */
    @Nonnull
    URI getUri();

    /**
     * @return The {@link HttpVersion} derived via its value from what was given by the request line.
     */
    @Nonnull
    HttpVersion getVersion();
}
