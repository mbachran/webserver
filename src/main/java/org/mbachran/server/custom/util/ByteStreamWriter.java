package org.mbachran.server.custom.util;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Wrapper around a {@link ByteArrayOutputStream} to ease up some writing to it.
 */
@SuppressWarnings("UnusedReturnValue")
public class ByteStreamWriter
{
    private final ByteArrayOutputStream stream;

    private final Charset encoding;

    public ByteStreamWriter(@Nonnull final ByteArrayOutputStream stream, @Nonnull final Charset encoding)
    {
        this.stream = Objects.requireNonNull(stream);
        this.encoding = Objects.requireNonNull(encoding);
    }

    /**
     * @param content The String to write to the stream as bytes based on the encoding given on construction.
     * @return This writer for fluent continuation.
     * @throws IOException If writing fails.
     */
    @Nonnull
    public ByteStreamWriter write(@Nonnull final String content) throws IOException
    {
        stream.write(content.getBytes(encoding));
        return this;
    }

    /**
     * @param content The int to write to the stream as bytes of its String representation based on the encoding given on construction.
     * @return This writer for fluent continuation.
     * @throws IOException If writing fails.
     */
    @Nonnull
    public ByteStreamWriter write(final int content) throws IOException
    {
        stream.write(String.valueOf(content).getBytes(encoding));
        return this;
    }

    /**
     * @param content The byte array to write to the stream. Encoding is ignored.
     * @return This writer for fluent continuation.
     * @throws IOException If writing fails.
     */
    @Nonnull
    public ByteStreamWriter write(@Nonnull final byte[] content) throws IOException
    {
        stream.write(content);
        return this;
    }

    /**
     * NOTE that this method has a side effect on the {@link ByteBuffer} through potential explicit position modification and through read via get.
     *
     * @see #write(ByteBuffer, int, int) which is called with zero and the limit of the buffer for your convenience.
     *
     * @param buffer The {@link ByteBuffer} to read from and to write its bytes to the stream.
     * @return This writer for fluent continuation.
     */
    @Nonnull
    public ByteStreamWriter write(@Nonnull final ByteBuffer buffer)
    {
        write(buffer, 0, buffer.limit());
        return this;
    }

    /**
     /**
     * NOTE that this method has a side effect on the {@link ByteBuffer} through potential explicit position modification and through read via get.
     *
     * @param buffer The {@link ByteBuffer} to read from and to write its bytes to the stream.
     * @param offset The position to start reading the contained bytes from the buffer.
     * @param length The number of bytes to read up from the offset.
     * @return This writer for fluent continuation.
     * @throws IndexOutOfBoundsException If the offset and limit settings do not respect the bounds of the {@link ByteBuffer}.
     */
    @Nonnull
    public ByteStreamWriter write(@Nonnull final ByteBuffer buffer, final int offset, final int length)
    {
        final byte[] bytes = new byte[length];
        buffer.position(offset);
        buffer.get(bytes);
        stream.writeBytes(bytes);
        return this;
    }
}
