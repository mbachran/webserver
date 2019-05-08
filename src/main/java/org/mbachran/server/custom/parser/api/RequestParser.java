package org.mbachran.server.custom.parser.api;

import org.mbachran.server.custom.HttpCode;
import org.mbachran.server.custom.request.api.Request;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;

/**
 * Parser that is {@link AutoCloseable} so it can close any resources as soon as it is done.
 * Contract is:
 * - a {@link ByteBuffer} that holds a sub sequence of bytes read for the incoming request.
 * - the {@link Request} is constructed from the concatenation of the sub sequences in order along with an applied parsing logic.
 *
 * Usage:
 * - Call {@link #parse(ByteBuffer)} until it indicates it received all data for a request
 * - Check via {@link #getFailure()} whether a failure has been detected as soon as parsing is done
 * - {@link #retrieve()} the request if there was no failure.
 * - If there was a failure it is the responsibility of the client what to do with it.
 */
public interface RequestParser extends AutoCloseable
{
    /**
     * @param buffer The {@link ByteBuffer} holding the next bytes from the request.
     * @return true is the {@link RequestParser} detected it has all data for the Request it tries to construct from the "stream".
     */
    boolean parse(@Nonnull ByteBuffer buffer);

    /**
     * @return A {@link HttpCode} if a failure was detected NULL otherwise. The code will not start with a 2..
     */
    @Nullable
    HttpCode getFailure();

    @Nonnull
    Request retrieve();
}
