package org.mbachran.server.custom.request.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The headers of a {@link Request}.
 */
public interface RequestHeaders
{
    /**
     * @param name The name of the header to retrieve.
     * @return The plain full value of the header (multi values or parameters need to be parsed from it by the client if given)
     */
    @Nullable
    String getHeader(@Nonnull String name);

    /**
     * The Builder to construct a {@link Request} that is immutable apart from the byte array from the body (which should
     * be used read only only though).
     */
    interface Builder
    {
        /**
         * Adds a header to the request. If a header is added multiple times the contract is a concatenation by comma.
         * Added null values will not lead to a ,, sequence but will be a no-op in case the header is already there.
         *
         * @param headerName   The name of the header to add a value for.
         * @param headerValue The value to be added to the header.
         * @return This {@link Builder}.
         */
        @SuppressWarnings("UnusedReturnValue")
        @Nonnull
        RequestHeaders.Builder addHeader(@Nonnull String headerName, @Nullable String headerValue);

        /**
         * @return The immutable {@link RequestHeaders} constructed by this {@link Builder}.
         */
        @Nonnull
        RequestHeaders build();
    }
}
