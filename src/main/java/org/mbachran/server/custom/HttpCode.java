package org.mbachran.server.custom;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Enum for the HTTP status codes.
 * Could have reused the HttpStatus from spring-web
 *
 * @see org.springframework.http.HttpStatus for documentation including rfc links.
 */
public enum HttpCode
{
    OK(200, "OK"),
    CREATED(201, "Created"),
    NO_CONTENT(204, "No Content"),
    BAD_REQUEST(400, "Bad Request"),
    NOT_FOUND(404, "Not Found"),
    LENGTH_REQUIRED(411, "Length Required"),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version not supported");

    private final int code;

    private final String reasonPhrase;

    HttpCode(final int code, @Nonnull final String reasonPhrase)
    {
        this.code = code;
        this.reasonPhrase = Objects.requireNonNull(reasonPhrase);
    }

    /**
     * @return The HTTP status code.
     */
    public int getCode()
    {
        return code;
    }

    /**
     * @return The reason phrase as to be used within the response status line.
     */
    @Nonnull
    public String getReasonPhrase()
    {
        return reasonPhrase;
    }
}
