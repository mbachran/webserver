package org.mbachran.server.custom.response;

import org.mbachran.server.custom.HttpCode;
import org.mbachran.server.custom.HttpVersion;
import org.mbachran.server.custom.util.ByteStreamWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.mbachran.server.custom.util.Delimiters.*;

/**
 * The response object to return to the client. Very simple version with a default serialization to a {@link ByteBuffer}.
 * Originally designed as immutable this contracted has been removed to allow header manipulation after the {@link Response} has been build by
 * its {@link Builder}.
 *
 * The response does not support varying the transfer encoding or anything on the body but plainly returns the byte array.
 *
 * The Builder defaults to {@link HttpVersion#HTTP_1_1}, utf-8 and {@link HttpCode#OK} if not explicitly set.
 */
public class Response
{
    private final HttpVersion version;

    private final Charset encoding;

    private final HttpCode code;

    private final byte[] body;

    private final Map<String, String> headers;

    private Response(@Nonnull final HttpVersion version,
                     @Nonnull final Charset encoding,
                     @Nonnull final HttpCode code,
                     @Nonnull final Map<String, String> headers,
                     @Nonnull final byte[] body)
    {
        this.version = Objects.requireNonNull(version);
        this.encoding = Objects.requireNonNull(encoding);
        this.code = Objects.requireNonNull(code);
        this.body = Objects.requireNonNull(body);
        this.headers = Objects.requireNonNull(headers);
    }

    /**
     * Default "serialization" concatenating status line, headers and body by CRLFs with an empty line in front of the body (or as terminator).
     * Both headers and body are optional.
     *
     * @return The full response serialized into a {@link ByteBuffer}.
     * @throws IOException If writing to the stream used for concatenation fails.
     */
    @Nonnull
    public ByteBuffer toByteBuffer() throws IOException
    {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream(1024);
        final ByteStreamWriter writer = new ByteStreamWriter(stream, encoding);
        writer.write(version.getValue()).write(SP).write(code.getCode()).write(SP).write(code.getReasonPhrase()).write(CR_LF);
        for (final Map.Entry<String, String> header : headers.entrySet())
        {
            writer.write(header.getKey()).write(COLON).write(SP).write(header.getValue()).write(CR_LF);
        }

        writer.write(CR_LF);
        if (body.length > 0)
        {
            writer.write(body);
        }

        return ByteBuffer.wrap(stream.toByteArray());
    }

    /**
     * Break the immutable pattern here to allow handlers in the dispatch chain to play there role regarding behavior (like connection handling).
     *
     * @param name  name of the header to modify
     * @param value header value (replaces aby existing value)
     */
    public void setHeader(@Nonnull final String name, @Nullable final String value)
    {
        Objects.requireNonNull(name);
        headers.put(name, value);
    }

    /**
     * Break the immutable pattern here to allow handlers in the dispatch chain to play there role regarding behavior (like connection handling).
     * @param name name of the header to remove
     */
    public void removeHeader(@Nonnull final String name)
    {
        Objects.requireNonNull(name);
        headers.remove(name);
    }

    /**
     * Convenience method to the convenience method {@link #buildErrorResponse(HttpCode, String)} if there is no message to be send.
     *
     * @param code The {@link HttpCode} to overwrite the {@link Builder}s default with.
     * @return The built {@link Response}.
     */
    public static Response buildErrorResponse(@Nonnull final HttpCode code)
    {
        return buildErrorResponse(code, null);
    }

    /**
     * Convenience method to build a common error {@link Response}.
     *
     * @param code    The {@link HttpCode} to overwrite the {@link Builder}s default with.
     * @param message The message interpreted as utf-8 to write to the body. The content length header will be set to the resulting bytes' length.
     * @return The built {@link Response}.
     */
    public static Response buildErrorResponse(@Nonnull final HttpCode code, @Nullable final String message)
    {
        final byte[] body = null == message ? new byte[0] : message.getBytes(StandardCharsets.UTF_8);
        return new Builder()
                .code(code)
                .addHeader("Content-Length", String.valueOf(body.length))
                .body(body)
                .build();
    }

    /**
     * Use to build a {@link Response}. NOTE that the build {@link Response} is still mutable regarding the headers afterwards.
     */
    public static class Builder
    {
        private HttpVersion version = HttpVersion.HTTP_1_1;

        private Charset encoding = StandardCharsets.UTF_8;

        private HttpCode code = HttpCode.OK;

        private byte[] body = null;

        private final Map<String, String> headers = new HashMap<>();

        /**
         * @param version The {@link HttpVersion} to use.
         * @return This {@link Builder}.
         */
        @Nonnull
        public Builder version(@Nonnull final HttpVersion version)
        {
            this.version = Objects.requireNonNull(version);
            return this;
        }

        /**
         * @param encoding The String representation of the {@link Charset} to use.
         * @return This {@link Builder}.
         */
        @Nonnull
        public Builder encoding(@Nonnull final String encoding)
        {
            this.encoding = Charset.forName(Objects.requireNonNull(encoding));
            return this;
        }

        /**
         * @param code The {@link HttpCode} to use.
         * @return This {@link Builder}.
         */
        @Nonnull
        public Builder code(@Nonnull final HttpCode code)
        {
            this.code = Objects.requireNonNull(code);
            return this;
        }

        /**
         * @param body The body content represented as plain byte array.
         * @return This {@link Builder}.
         */
        @Nonnull
        public Builder body(@Nullable final byte[] body)
        {
            this.body = body;
            return this;
        }

        /**
         * @param name The name of the header to add.
         * @param value The value of the header (simple replacement, no concatenation).
         * @return This {@link Builder}.
         */
        @Nonnull
        public Builder addHeader(@Nonnull final String name, @Nullable final String value)
        {
            Objects.requireNonNull(name);
            this.headers.put(name, value);
            return this;
        }

        /**
         * @return The built {@link Response}.
         */
        @Nonnull
        public Response build()
        {
            return new Response(version, encoding, code, headers, body == null ? new byte[0] : body);
        }
    }

    public HttpVersion getVersion()
    {
        return version;
    }

    public Charset getEncoding()
    {
        return encoding;
    }

    public HttpCode getCode()
    {
        return code;
    }

    public byte[] getBody()
    {
        return body;
    }

    /**
     * @return unmodifiable map
     */
    public Map<String, String> getHeaders()
    {
        return headers;
    }
}
