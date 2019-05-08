package org.mbachran.server.custom.parser.impl;

import javax.annotation.Nullable;

/**
 * Enum for the HTTP transfer encodings.
 * Enum names directly match the expected trimmed lower case header values.
 *
 * Just because they are listed here does not mean handlers have been implemented for all!
 */
public enum TransferEncoding
{
    chunked,
    compress,
    deflate,
    gzip,
    identity;

    /**
     * Avoids the nasty Exception and returns null in case of absence.
     * Enum is short enough for the iteration not being a problem.
     *
     * @param value The header value of the transfer encoding. (null will always return null :-))
     * @return The {@link TransferEncoding} or null.
     */
    public static TransferEncoding from(@Nullable String value)
    {
        for (TransferEncoding transferEncoding : values())
        {
            if (transferEncoding.name().equals(value))
            {
                return transferEncoding;
            }
        }

        return null;
    }
}
