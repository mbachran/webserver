package org.mbachran.server.custom.request.impl;

import org.mbachran.server.custom.request.api.RequestHeaders;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Default implementation of the {@link RequestHeaders}.
 */
public class DefaultRequestHeaders implements RequestHeaders
{
    private final Map<String, String> headers;

    private DefaultRequestHeaders(@Nonnull final Map<String, String> headers)
    {
        this.headers = Objects.requireNonNull(headers);
    }

    /**
     * Default implementation of the {@link org.mbachran.server.custom.request.api.RequestHeaders.Builder}.
     */
    public static class DefaultBuilder implements Builder
    {
        private final Map<String, String> headers = new HashMap<>();

        /**
         * Converts the header name to lower case and trims it. Value is trimmed as well.
         * If a header already exists the values are concatenated separated by comma.
         * Null values will not lead to ,, concatenations.
         *
         * @param headerName  The header name
         * @param headerValue The full header value (un-parsed for now)
         * @return This builder for fluent chaining.
         */
        @Nonnull
        @SuppressWarnings("UnusedReturnValue")
        public Builder addHeader(@Nonnull final String headerName, @Nullable final String headerValue)
        {
            final String effectiveName = headerName.toLowerCase().trim();
            final String existingValue = headers.get(effectiveName);
            final String effectiveValue;
            if (existingValue == null)
            {
                effectiveValue = headerValue == null ? null : headerValue.trim();
            }
            else
            {
                effectiveValue = existingValue + (headerValue == null ? "" : ',' + headerValue.trim());
            }

            headers.put(effectiveName, effectiveValue);
            return this;
        }

        @Nonnull
        public RequestHeaders build()
        {
            return new DefaultRequestHeaders(headers);
        }
    }

    @Nullable
    @Override
    public String getHeader(@Nonnull final String name)
    {
        return headers.get(name);
    }

    @Override
    @Nonnull
    public String toString()
    {
        return new StringJoiner(", ", DefaultRequestHeaders.class.getSimpleName() + "[", "]")
                .add("headers=" + headers)
                .toString();
    }
}
