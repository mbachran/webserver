package org.mbachran.server.custom.request.impl;

import org.mbachran.server.custom.request.api.Request;
import org.mbachran.server.custom.request.api.RequestBody;
import org.mbachran.server.custom.request.api.RequestHeaders;
import org.mbachran.server.custom.request.api.RequestLine;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Default implementation of {@link Request}.
 * Immutable apart from the byte array impediment on the {@link RequestBody} as enforced by the API contract right now.
 */
public class DefaultRequest implements Request
{
    private final RequestLine requestLine;

    private final RequestHeaders requestHeaders;

    private final RequestBody requestBody;

    public DefaultRequest(@Nonnull final RequestLine requestLine,
                          @Nonnull final RequestHeaders requestHeaders,
                          @Nonnull final RequestBody requestBody)
    {

        this.requestLine = Objects.requireNonNull(requestLine);
        this.requestHeaders = Objects.requireNonNull(requestHeaders);
        this.requestBody = Objects.requireNonNull(requestBody);
    }

    @Override
    @Nonnull
    public RequestLine getRequestLine()
    {
        return requestLine;
    }

    @Override
    @Nonnull
    public RequestHeaders getRequestHeaders()
    {
        return requestHeaders;
    }

    @Override
    @Nonnull
    public RequestBody getRequestBody()
    {
        return requestBody;
    }

    @Override
    public String toString()
    {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("requestLine=" + requestLine)
                .add("requestHeaders=" + requestHeaders)
                .toString();
    }
}
