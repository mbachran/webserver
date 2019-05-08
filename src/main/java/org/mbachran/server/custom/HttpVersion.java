package org.mbachran.server.custom;

import javax.annotation.Nullable;

/**
 * Enum of HTTP versions.
 */
public enum HttpVersion
{
    HTTP_1_0("HTTP/1.0"),
    HTTP_1_1("HTTP/1.1");
    private final String value;

    HttpVersion(String value)
    {
        this.value = value;
    }

    /**
     * @param httpVersion The version to lookup the Enum for.
     * @return The Enum instance for the version or null if the passed in version is not supported.
     */
    @Nullable
    public static HttpVersion from(final String httpVersion)
    {
        for (HttpVersion version : values())
        {
            if (version.value.equals(httpVersion))
            {
                return version;
            }
        }

        return null;
    }

    /**
     * @return The value as used in the HTTP message lines.
     */
    public String getValue()
    {
        return value;
    }
}
