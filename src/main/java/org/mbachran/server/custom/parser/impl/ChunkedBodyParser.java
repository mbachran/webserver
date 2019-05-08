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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.mbachran.server.custom.util.Delimiters.CR_LF;
import static org.mbachran.server.custom.util.Delimiters.SEMI_COLON;

/**
 * Parses transfer encoding chunked.
 *
 * Ignores (only logs) extensions and trailers.
 * Invalid request (format) behavior must be investigated more deeply. Therefore currently never a failure is returned.
 */
@Component
public class ChunkedBodyParser implements BodyParser
{
    private static final Logger LOG = LoggerFactory.getLogger(ChunkedBodyParser.class);

    @Override
    public TransferEncoding getTransferEncoding()
    {
        return TransferEncoding.chunked;
    }

    @Override
    public Parser create(@Nonnull RequestHeaders headers)
    {
        return new ChunkedParser(headers);
    }

    /**
     * Switches between chunk-count, chunk-data and chunk-trailer parsing.
     * For count and trailer a StringBuilder is used (unfortunately as this makes it too complex).
     * For the data one stream is used to collect the chunk data which is then concatenated to the full body.
     * Read positioning it too complex especially as binary data should not be damaged by char conversion and String and byte array length might vary.
     */
    class ChunkedParser implements Parser
    {
        /**
         * The char based parsing for count and trailer.
         * Note that this can contain besides the hexadecimal count the extension and is also used for the trailer.
         */
        private final StringBuilder hexBuffer = new StringBuilder();

        /**
         * The stream collecting the full body.
         */
        private final ByteArrayOutputStream bodyCollectionBuffer = new ByteArrayOutputStream();

        /**
         * The stream collecting the actual chunk.
         */
        private final ByteArrayOutputStream chunkCollectionBuffer = new ByteArrayOutputStream();

        /**
         * Helper class for writing to {@link #chunkCollectionBuffer}.
         */
        private final ByteStreamWriter chunkCollectionBufferWriter = new ByteStreamWriter(chunkCollectionBuffer, StandardCharsets.UTF_8);

        /**
         * The current chunk size. Used for switching the three states:
         * -1 = count
         *  0 = trailer
         *  positive integer = body
         */
        private int currentChunkSize = -1;

        /**
         * We still have to see how we can detect failures during parsing (instead of hanging or similar).
         */
        @SuppressWarnings("unused")
        private HttpCode failure;

        /**
         * The final bytes representing the full body as collected by bodyCollectionBuffer.
         */
        private byte[] bodyBytes;

        /**
         * The offset of the StringBuilder used for char based collection ({@link #hexBuffer}).
         */
        private int dataOffset;

        ChunkedParser(@Nonnull final RequestHeaders headers)
        {
            // headers are not used yet
        }

        @Override
        public boolean parse(@Nonnull final ByteBuffer buffer)
        {
            boolean parsingDone = false;
            boolean reading = true;
            final int previousBufferLength = hexBuffer.length();
            hexBuffer.append(StandardCharsets.UTF_8.decode(buffer).toString());
            buffer.rewind();
            while (reading)
            {
                if (currentChunkSize == -1)
                {
                    // next to read is a chunk size
                    final int hexEnd = hexBuffer.indexOf(CR_LF, dataOffset);
                    if (hexEnd != -1)
                    {
                        final String hexString = hexBuffer.substring(dataOffset, hexEnd);

                        // let's simply ignore the extension for now:
                        final String[] hexAndExtension = hexString.split(SEMI_COLON);
                        if (hexAndExtension.length > 1)
                        {
                            LOG.debug("Ignoring extension: {}", hexAndExtension[1]);
                        }

                        currentChunkSize = Integer.parseInt(hexAndExtension[0], 16);
                        final int readBefore = dataOffset < previousBufferLength ? previousBufferLength - dataOffset : 0;
                        dataOffset += hexString.length() + CR_LF.length();
                        if (currentChunkSize == 0)
                        {
                            LOG.debug("Last chunk finished by zero: {}.", currentChunkSize);
                        }
                        else
                        {
                            LOG.debug("Next chunk size is {}. Data offset is {}.", currentChunkSize, dataOffset);
                            final byte[] hexBytes = hexString.getBytes(StandardCharsets.UTF_8);
                            buffer.position(buffer.position() + hexBytes.length + CR_LF.length() - readBefore);
                            if (!buffer.hasRemaining())
                            {
                                reading = false;
                            }
                        }
                    }
                    else
                    {
                        // need for data to have the complete hex value
                        reading = false;
                    }
                }
                else if (currentChunkSize == 0)
                {
                    // read the trailer if given after we found the zero after the last chunk or read just the final CR LF
                    final int end = hexBuffer.indexOf(CR_LF, dataOffset);
                    if (end != -1)
                    {
                        final String trailer = hexBuffer.substring(dataOffset, end);
                        if (!trailer.isEmpty())
                        {
                            LOG.debug("Ignoring trailer: {}", trailer);
                        }

                        if (hexBuffer.length() > end + CR_LF.length())
                        {
                            LOG.warn("Received data after trailer of chunked transfer: {}", hexBuffer.substring(end, hexBuffer.length()));
                        }

                        bodyBytes = bodyCollectionBuffer.toByteArray();
                        reading = false;
                        parsingDone = true;
                    }
                    else
                    {
                        // wait for more data
                        reading = false;
                    }
                }
                else
                {
                    final int lengthToRead = Math.min(buffer.remaining(), currentChunkSize - chunkCollectionBuffer.size() + CR_LF.length());
                    final int positionBeforeRead = buffer.position();
                    chunkCollectionBufferWriter.write(buffer, positionBeforeRead, lengthToRead);
                    LOG.debug("Added {} bytes to chunk.", lengthToRead);

                    if (chunkCollectionBuffer.size() >= currentChunkSize + CR_LF.length())
                    {
                        // ... but do not include the CR LF into the body:
                        final byte[] chunkBytes = chunkCollectionBuffer.toByteArray();
                        bodyCollectionBuffer.write(chunkBytes, 0, currentChunkSize);
                        LOG.debug("Added {} chunk bytes to body now of size {}.", currentChunkSize, bodyCollectionBuffer.size());
                        chunkCollectionBuffer.reset();
                        currentChunkSize = -1;
                    }

                    //use the char not hte byte length for data offset update:
                    final byte[] readBytes = new byte[lengthToRead];
                    buffer.asReadOnlyBuffer().position(positionBeforeRead).get(readBytes);
                    dataOffset += new String(readBytes, StandardCharsets.UTF_8).length();

                    if (!buffer.hasRemaining())
                    {
                        reading = false;
                    }
                }
            }

            return parsingDone;
        }

        @Nullable
        @Override
        public HttpCode getFailure()
        {
            return null;
        }

        @Nonnull
        @Override
        public RequestBody retrieve()
        {
            return bodyBytes == null ? new DefaultRequestBody() : new DefaultRequestBody(bodyBytes);
        }

        @Override
        public void close() throws IOException
        {
            chunkCollectionBuffer.close();
            bodyCollectionBuffer.close();
        }
    }
}
