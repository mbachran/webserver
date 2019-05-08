package org.mbachran.server.custom.request.impl;

import org.mbachran.server.custom.HttpVersion;
import org.mbachran.server.custom.request.api.Method;
import org.mbachran.server.custom.request.api.RequestLine;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Default implementation of {@link RequestLine}.
 */
public class DefaultRequestLine implements RequestLine
{
    private final Method method;

    private final URI uri;

    private final HttpVersion version;

    public DefaultRequestLine(@Nonnull final Method method, @Nonnull final URI uri, @Nonnull final HttpVersion version)
    {
        this.method = Objects.requireNonNull(method);
        this.uri = Objects.requireNonNull(uri);
        this.version = Objects.requireNonNull(version);
    }

    @Override
    @Nonnull
    public Method getMethod()
    {
        return method;
    }

    @Override
    @Nonnull
    public URI getUri()
    {
        return uri;
    }

    @Override
    @Nonnull
    public HttpVersion getVersion()
    {
        return version;
    }

    @Override
    @Nonnull
    public String toString()
    {
        return new StringJoiner(", ", DefaultRequestLine.class.getSimpleName() + "[", "]")
                .add("method=" + method)
                .add("uri='" + uri + "'")
                .add("version=" + version)
                .toString();
    }
}
