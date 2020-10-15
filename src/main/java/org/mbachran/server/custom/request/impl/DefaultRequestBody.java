package org.mbachran.server.custom.request.impl;

import org.mbachran.server.custom.request.api.RequestBody;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Default implementation of the {@link RequestBody}.
 * Not immutable due to byte array exposure.
 */
public class DefaultRequestBody implements RequestBody
{
    private final byte[] body;

    /**
     * Convenience constructor for empty body backing up the body with an empty array.
     */
    public DefaultRequestBody()
    {
        this(new byte[0]);
    }

    public DefaultRequestBody(@Nonnull final byte[] body)
    {
        this.body = Objects.requireNonNull(body);
    }

    @Override
    @Nonnull
    public byte[] getContent()
    {
        return body;
    }
}
