package org.mbachran.server.custom.parser.impl;

import org.mbachran.server.custom.HttpCode;
import org.mbachran.server.custom.HttpVersion;
import org.mbachran.server.custom.parser.api.RequestParser;
import org.mbachran.server.custom.request.api.Method;
import org.mbachran.server.custom.request.api.Request;
import org.mbachran.server.custom.request.api.RequestBody;
import org.mbachran.server.custom.request.api.RequestHeaders;
import org.mbachran.server.custom.request.impl.DefaultRequest;
import org.mbachran.server.custom.request.impl.DefaultRequestBody;
import org.mbachran.server.custom.request.impl.DefaultRequestHeaders;
import org.mbachran.server.custom.request.impl.DefaultRequestLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;

import static org.mbachran.server.custom.util.Delimiters.*;


/**
 * Default {@link RequestParser} implementation.
 * Request line and header parsing is implemented using a {@link StringBuilder} based on utf-8 encoding.
 * Body parsing is based on bytes not to damage any binaries and is delegated to a {@link BodyParser} based on transfer encoding.
 * Content-Types (especially types like multipart/form-data) would need to be handled downstream by the {@link BodyParser}s.
 * <p>
 * The parser is called
 * - multiple times for subsequent data chunks being read by the server
 * - expected to be called once per request (also for subsequent requests on the same connection) as it is stateful
 * <p>
 * Header parsing supports
 * - the same header name appearing multiple times (concatenating values by comma)
 * - multi line header values (keeping the leading SP/HT within the concatenation)
 */
public class DefaultRequestParser implements RequestParser
{
    private static final Logger LOG = LoggerFactory.getLogger(DefaultRequestParser.class);

    /**
     * The injected {@link BodyParser} per {@link TransferEncoding}s. Support depends on list injection and therefore on the classpath.
     *
     * Only identity and chunked are supported so far.
     */
    private final EnumMap<TransferEncoding, BodyParser> bodyParsers;

    /**
     * Used for simple parsing of request line and headers. Is never used to damage part of the body.
     */
    private final StringBuilder requestBuffer = new StringBuilder();

    /**
     * The {@link BodyParser} to delegate body parsing to selected based on the transfer encoding header which defaults to identity.
     */
    private BodyParser.Parser bodyParser;

    /**
     * Position within the {@link #requestBuffer} while going ahead with parsing.
     */
    private int parseOffset = 0;

    /**
     * {@link Mode} for the state machine switching between request line, headers and body.
     */
    private Mode mode = Mode.RequestLine;

    /**
     * The {@link org.mbachran.server.custom.request.api.RequestLine} build during parsing to factor the {@link Request} from on retrieval.
     * Cannot be null if the parser ever ends.
     */
    private DefaultRequestLine requestLine;

    /**
     * The {@link org.mbachran.server.custom.request.api.RequestHeaders} build during parsing to factor the {@link Request} from on retrieval.
     * Cannot be null if the parser ever ends but might be empty as no headers might be given.
     */
    private RequestHeaders headers;

    /**
     * The {@link org.mbachran.server.custom.request.api.RequestBody} build during parsing to factor the {@link Request} from on retrieval.
     * If null on retrieval an empty body will be added to the {@link Request}.
     */
    private RequestBody body;

    /**
     * Is set during parsing is the request is not OK, the parser does not support a certain aspect yet etc.
     * If set clients must use this to create a corresponding Response (namely the Connection).
     */
    private HttpCode failure;

    DefaultRequestParser(@Nonnull final EnumMap<TransferEncoding, BodyParser> bodyParsers)
    {
        this.bodyParsers = bodyParsers;
    }

    private enum Mode
    {
        RequestLine, Headers, Body
    }

    @Override
    public HttpCode getFailure()
    {
        return failure;
    }

    @Nonnull
    @Override
    public Request retrieve()
    {
        final RequestBody body = this.body == null ? new DefaultRequestBody() : this.body;
        return new DefaultRequest(this.requestLine, this.headers, body);
    }

    @Override
    public void close() throws Exception
    {
        if (bodyParser != null) bodyParser.close();
    }

    @Override
    public boolean parse(@Nonnull final ByteBuffer buffer)
    {
        boolean parsingDone = false;
        boolean remaining = true;
        while (remaining)
        {
            switch (mode)
            {
                case RequestLine:
                    remaining = parseRequestLine(buffer);
                    break;
                case Headers:
                    remaining = parseHeaders(buffer);
                    break;
                case Body:
                    parsingDone = parseBody(buffer);

                    // body concatenation will not need the loop on the buffer anymore
                    remaining = false;
                    break;
            }

            if (failure != null)
            {
                remaining = false;
                parsingDone = true;
            }
        }

        return parsingDone;
    }

    private boolean parseRequestLine(@Nonnull final ByteBuffer buffer)
    {
        requestBuffer.append(StandardCharsets.UTF_8.decode(buffer).toString());

        boolean remaining = false;
        final int firstLineEnd = requestBuffer.indexOf(CR_LF);
        if (firstLineEnd != -1)
        {
            final String firstLine = requestBuffer.substring(parseOffset, firstLineEnd);
            final String[] firstLineSegments = firstLine.split(SP);
            if (firstLineSegments.length != 3)
            {
                LOG.info("Retrieved invalid request line: {}", firstLine);
                failure = HttpCode.BAD_REQUEST;
            }
            else
            {
                final HttpVersion httpVersion = HttpVersion.from(firstLineSegments[2]);
                if (httpVersion == null)
                {
                    LOG.info("Retrieved invalid HTTP version: {}", firstLineSegments[2]);
                    failure = HttpCode.BAD_REQUEST;
                }
                else
                {
                    final String uriStr = firstLineSegments[1].trim();
                    try
                    {
                        final URI uri = new URI(uriStr);
                        requestLine = new DefaultRequestLine(Method.valueOf(firstLineSegments[0]), uri, httpVersion);

                        parseOffset = firstLineEnd + CR_LF.length();
                        // handling already read second CR_LF indicating there are no headers
                        if (requestBuffer.length() >= parseOffset + CR_LF.length() && requestBuffer.substring(parseOffset,
                                parseOffset + CR_LF.length()).equals(CR_LF))
                        {
                            parseOffset = parseOffset + CR_LF.length();
                            mode = Mode.Body;
                            headers = new DefaultRequestHeaders.DefaultBuilder().build();
                        }
                        else
                        {
                            mode = Mode.Headers;
                        }

                        remaining = true;
                    }
                    catch (URISyntaxException e)
                    {
                        LOG.info("Invalid uri {} in request line: {}", uriStr, e.getReason());
                        failure = HttpCode.BAD_REQUEST;
                    }
                }
            }
        }

        return remaining;
    }

    private boolean parseHeaders(@Nonnull ByteBuffer buffer)
    {
        requestBuffer.append(StandardCharsets.UTF_8.decode(buffer).toString());

        boolean remaining = false;

        // lazy parsing: do not consume headers before all are retrieved
        final int headersEnd = requestBuffer.indexOf(EMPTY_LINE);
        if (headersEnd != -1)
        {
            // handling second CR_LF was read after we switched to headers
            if (requestBuffer.length() == parseOffset + CR_LF.length() && requestBuffer.substring(parseOffset, parseOffset + CR_LF.length()).equals(
                    CR_LF))
            {
                parseOffset = parseOffset + CR_LF.length();
                mode = Mode.Body;
                headers = new DefaultRequestHeaders.DefaultBuilder().build();
            }
            else
            {
                final String headersStr = requestBuffer.substring(parseOffset, headersEnd);
                final String[] headerLines = headersStr.split(CR_LF);
                final RequestHeaders.Builder builder = new DefaultRequestHeaders.DefaultBuilder();

                // look ahead for multi line header values
                for (int i = 0; i < headerLines.length; i++)
                {
                    final String headerLine = headerLines[i];
                    final int nameEnd = headerLine.indexOf(COLON);
                    final String name = headerLine.substring(0, nameEnd);
                    final StringBuilder value = new StringBuilder(headerLine.substring(nameEnd + COLON.length()));

                    // collect the full value
                    boolean valueFullyCollected = false;
                    while (i + 1 < headerLines.length && !valueFullyCollected)
                    {
                        final String nextLine = headerLines[i + 1];
                        if (nextLine.startsWith(SP) || nextLine.startsWith(HT))
                        {
                            value.append(nextLine);
                            i++;// fast forward
                        }
                        else
                        {
                            valueFullyCollected = true;
                        }
                    }

                    builder.addHeader(name, value.toString());
                }

                headers = builder.build();
                parseOffset = headersEnd + EMPTY_LINE.length();
                mode = Mode.Body;
            }

            remaining = true;
        }

        return remaining;
    }

    private boolean parseBody(@Nonnull final ByteBuffer buffer)
    {
        boolean parsingDone = false;
        if (bodyParser == null)
        {
            final TransferEncoding transferEncoding = determineTransferEncoding();
            if (transferEncoding == TransferEncoding.identity)
            {
                final String contentLength = headers.getHeader("content-length");
                if (contentLength == null || Integer.valueOf(contentLength) == 0)
                {
                    // we could validate here whether there is no disallowed trailing body
                    // e.g. GET should come here
                    return true;
                }
            }

            this.bodyParser = selectBodyParser(transferEncoding);
            if (bodyParser == null)
            {
                return true;
            }

            // we just switched to body parsing and might have a left over from the last read buffer
            final int carryForwardLength = requestBuffer.substring(parseOffset, requestBuffer.length()).getBytes(StandardCharsets.UTF_8).length;
            if (carryForwardLength > 0)
            {
                final int carryForwardStart = buffer.limit() - carryForwardLength;
                final byte[] carryForwardBytes = new byte[carryForwardLength];
                buffer.position(carryForwardStart);
                buffer.get(carryForwardBytes);
                parsingDone = bodyParser.parse(ByteBuffer.wrap(carryForwardBytes));
            }
        }
        else
        {
            parsingDone = bodyParser.parse(buffer);
        }

        if (parsingDone)
        {
            failure = bodyParser.getFailure();
            body = bodyParser.retrieve();
        }

        return parsingDone;
    }

    private BodyParser.Parser selectBodyParser(final TransferEncoding transferEncoding)
    {
        final BodyParser bodyParser = bodyParsers.get(transferEncoding);
        if (bodyParser == null)
        {
            LOG.error("Unsupported transfer encoding: " + headers.getHeader("transfer-encoding"));
            failure = HttpCode.BAD_REQUEST;
            return null;
        }
        else
        {
            return bodyParser.create(headers);
        }
    }

    private TransferEncoding determineTransferEncoding()
    {
        final String transferEncodingHeader = headers.getHeader("transfer-encoding");
        final TransferEncoding transferEncoding;
        if (transferEncodingHeader == null)
        {
            transferEncoding = TransferEncoding.identity;
        }
        else
        {
            transferEncoding = TransferEncoding.from(transferEncodingHeader.trim().toLowerCase());
        }
        return transferEncoding;
    }
}
