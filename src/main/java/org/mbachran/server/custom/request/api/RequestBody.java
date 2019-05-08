package org.mbachran.server.custom.request.api;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

/**
 * The body a {@link Request}. Empty bodies are represented by an empty byte array contained.
 * NOTE that giving out the byte array of the {@link RequestBody} directly by reference violates the immutable contract of the whoel
 * {@link Request} in principal. Manipulating the array is not allowed. Maybe this contract can be changed to
 * {@link ByteBuffer#asReadOnlyBuffer()}?
 */
public interface RequestBody
{
    /**
     * @return The body content. Never null. Empty array for empty body.
     */
    @Nonnull
    byte[] getContent();
}
