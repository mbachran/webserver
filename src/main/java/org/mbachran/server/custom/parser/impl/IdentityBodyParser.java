package org.mbachran.server.custom.parser.impl;

import org.mbachran.server.custom.HttpCode;
import org.mbachran.server.custom.request.api.RequestBody;
import org.mbachran.server.custom.request.api.RequestHeaders;
import org.mbachran.server.custom.request.impl.DefaultRequestBody;
import org.mbachran.server.custom.util.ByteStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Parses transfer encoding identity (and therefore is the default pick).
 *
 * It requires content length header to be present if called.
 */
@Component
public class IdentityBodyParser implements BodyParser
{
    private static final Logger LOG = LoggerFactory.getLogger(IdentityBodyParser.class);

    @Override
    public TransferEncoding getTransferEncoding()
    {
        return TransferEncoding.identity;
    }

    @Override
    public Parser create(@Nonnull final RequestHeaders headers)
    {
        return new IdentityParser(headers);
    }

    /**
     * The stateful part of the parser.
     */
    class IdentityParser implements Parser
    {
        /**
         * The stream to collect bytes in.
         */
        private final ByteArrayOutputStream bodyCollectionBuffer = new ByteArrayOutputStream();

        /**
         * Helper class to write to {@link #bodyCollectionBuffer}.
         */
        private final ByteStreamWriter bodyCollectionBufferWriter = new ByteStreamWriter(bodyCollectionBuffer, StandardCharsets.UTF_8);

        /**
         * Currently this parser only uses the content length header.
         */
        private final RequestHeaders headers;

        /**
         * Set if the content length header is missing or the body size exceeds the content length (only if the garbage data is received
         * within the same buffer).
         */
        private HttpCode failure;

        /**
         * The collected bytes for the body. If null on retrieval an empty {@link RequestBody} is created for factoring the
         * {@link org.mbachran.server.custom.request.api.Request}
         */
        private byte[] bodyBytes;

        IdentityParser(@Nonnull final RequestHeaders headers)
        {
            this.headers = headers;
        }

        @Override
        public boolean parse(@Nonnull ByteBuffer buffer)
        {
            boolean parsingDone = false;
            final int contentLength = getRequireContentLength();
            if (contentLength != -1)
            {
                bodyCollectionBufferWriter.write(buffer);
                parsingDone = validateBodyLength(contentLength);
            }

            return parsingDone;
        }

        @Nullable
        @Override
        public HttpCode getFailure()
        {
            return failure;
        }

        @Nonnull
        @Override
        public RequestBody retrieve()
        {
            return bodyBytes == null ? new DefaultRequestBody() : new DefaultRequestBody(bodyBytes);
        }

        /**
         * @param contentLength The content length from the header.
         * @return True if the length was reached by the body, false if either trailing bytes wre detected or parsing is not yet done.
         */
        private boolean validateBodyLength(final int contentLength)
        {
            boolean parsingDone = false;
            if (bodyCollectionBuffer.size() == contentLength)
            {
                bodyBytes = bodyCollectionBuffer.toByteArray();
                parsingDone = true;
            }
            else if (bodyCollectionBuffer.size() > contentLength)
            {
                LOG.info("Body is longer than given content length. Rejecting as bad request.");
                failure = HttpCode.BAD_REQUEST; // for now the behavior if we receive more data than specified by the header
            }

            return parsingDone;
        }

        /**
         * @return The length or -1 if there was a failure.
         */
        private int getRequireContentLength()
        {
            final int contentLength;
            final String contentLengthStr = headers.getHeader("content-length");
            if (contentLengthStr == null)
            {
                LOG.info("Length header is missing but required by this server.");
                failure = HttpCode.LENGTH_REQUIRED;
                contentLength = -1;
            }
            else
            {
                contentLength = Integer.valueOf(contentLengthStr);
            }

            return contentLength;
        }

        @Override
        public void close() throws Exception
        {
            bodyCollectionBuffer.close();
        }
    }
}
