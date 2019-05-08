package org.mbachran.server.custom.parser.api;

import javax.annotation.Nonnull;

/**
 * Factory to be injected and used for assisted inject to create {@link RequestParser}s that are stateful.
 */
public interface RequestParserFactory
{
    /**
     * @return Creates a stateful {@link RequestParser}
     */
    @Nonnull
    RequestParser create();
}
