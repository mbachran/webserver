package org.mbachran.server.custom.parser.impl;

import org.mbachran.server.custom.HttpCode;
import org.mbachran.server.custom.request.api.RequestBody;
import org.mbachran.server.custom.request.api.RequestHeaders;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;

/**
 * Interface for parsing request bodies in a stateful manner and per transfer encoding.
 * Its interface is similar as the {@link org.mbachran.server.custom.parser.api.RequestParser} and it directly plugs into the
 * {@link DefaultRequestParser}.
 *
 * BodyParsers can allow to chain those parsers for layered transformation (like gzip etc.).
 * Body parsing is extensible by allowing to implement different parsing per transfer encoding of even down stream for mime types.
 * This includes the way how the end of an body is detected and it must.
 *
 * Note that the {@link BodyParser} itself is a wired Spring instance and must not have a state. It acts like the factories used for assisted
 * injection. The stateful part is in the inner class {@link Parser} of the interface.
 */
public interface BodyParser
{
    TransferEncoding getTransferEncoding();

    Parser create(@Nonnull RequestHeaders headers);

    /**
     * Represents the stateful part of the {@link BodyParser}.
     * Behaves same contract wise as {@link org.mbachran.server.custom.parser.api.RequestParser} apart from the type to be retrieved.
     */
    interface Parser extends AutoCloseable
    {
        /**
         * @param buffer The {@link ByteBuffer} holding the next bytes from the body.
         * @return true is the {@link BodyParser} detected it has all data for the body.
         */
        boolean parse(@Nonnull ByteBuffer buffer);

        /**
         * @return A {@link HttpCode} if a failure was detected NULL otherwise. The code will not start with a 2..
         */
        @Nullable
        HttpCode getFailure();

        /**
         * @return The {@link RequestBody}. Might be an empty body.
         */
        @Nonnull
        RequestBody retrieve();
    }
}
