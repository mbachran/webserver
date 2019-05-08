package org.mbachran.server.custom.connection.impl;

import org.apache.commons.lang3.StringUtils;
import org.mbachran.server.custom.HttpCode;
import org.mbachran.server.custom.connection.api.Connection;
import org.mbachran.server.custom.dispatch.api.Dispatcher;
import org.mbachran.server.custom.parser.api.RequestParser;
import org.mbachran.server.custom.parser.api.RequestParserFactory;
import org.mbachran.server.custom.request.api.Request;
import org.mbachran.server.custom.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * Factors a {@link RequestParser} per call to {@link #serve()}.
 * Is responsible for closing the socket it was factored upon.
 * Dispatches to a {@link Dispatcher} to handle requests after they have been parsed and build.
 * Responds to its {@link SocketChannel}.
 * Handles connections including keep-alive.
 */
public class DefaultConnection implements Connection
{
    private static final Logger LOG = LoggerFactory.getLogger(DefaultConnection.class);

    /**
     * Avoids garbage caused by boxing otherwise on each return. (or is the JVM by now smart enough?)
     */
    private static final long KEEP_RUNNING = 0L;

    /**
     * Avoids garbage caused by boxing otherwise on each return. (or is the JVM by now smart enough?)
     */
    private static final long STOP_RUNNING = -1L;

    /**
     * Nice to have.
     */
    private final long creationTime;

    /**
     * The reason for this instance to exist. Reads, writes and closes it.
     */
    private final SocketChannel dataSocketChannel;

    /**
     * Default that might be temporarily overwritten on the {@link #dataSocketChannel} by the keep-alive timeout.
     */
    private final int socketTimeout;

    /**
     * The (Spring) configured size of the ByteBuffer to use for reading. Static at runtime.
     */
    private final int readBufferSize;

    /**
     * Teh factory to create new {@link RequestParser}s per request.
     */
    private final RequestParserFactory requestParserFactory;

    /**
     * The {@link Dispatcher} to handle requests and produce responses.
     */
    private final Dispatcher dispatcher;

    /**
     * Counter to check keep-alive max values against.
     */
    private int numberOfRequestsReceived;

    DefaultConnection(final int socketTimeout,
                      final int readBufferSize,
                      @Nonnull final RequestParserFactory requestParserFactory,
                      final long creationTime,
                      @Nonnull final SocketChannel dataSocketChannel,
                      @Nonnull final Dispatcher dispatcher)
    {
        this.socketTimeout = socketTimeout;
        this.readBufferSize = readBufferSize;
        this.requestParserFactory = requestParserFactory;
        this.dispatcher = dispatcher;
        this.creationTime = creationTime;
        this.dataSocketChannel = dataSocketChannel;
    }

    @Override
    public Long serve()
    {
        boolean keepAlive = true;
        final String connectionName = Thread.currentThread().getName();
        LOG.info("Connection {} created at {} awaiting data ...", connectionName, creationTime);
        try
        {
            try (final RequestParser requestParser = requestParserFactory.create())
            {
                final ByteBuffer readBuffer = ByteBuffer.allocateDirect(readBufferSize);
                dataSocketChannel.socket().setSoTimeout(socketTimeout);
                boolean reading = true;
                while (reading)
                {
                    LOG.debug("Connection {} created at {} reading data ...", connectionName, creationTime);
                    readBuffer.clear();
                    final int numBytesRead = dataSocketChannel.read(readBuffer);
                    if (numBytesRead == -1)
                    {
                        dataSocketChannel.close();
                        reading = false;
                        LOG.info("Connection {} created at {} closed. No more data retrieved.", connectionName, creationTime);
                    }
                    else
                    {
                        readBuffer.flip();
                        LOG.debug("Parsing read buffer: '{}'", StandardCharsets.UTF_8.decode(readBuffer).toString().replaceAll("\\\\", "X"));
                        readBuffer.rewind();

                        final boolean done = requestParser.parse(readBuffer);
                        if (done)
                        {
                            final HttpCode failure = requestParser.getFailure();
                            final Response response;
                            if (failure != null)
                            {
                                response = new Response.Builder().code(failure).build();
                            }
                            else
                            {
                                final Request request = requestParser.retrieve();
                                response = dispatcher.handle(request);

                                final int timeoutSeconds = handleConnectionLiveTime(request, response);

                                // go back to the default read timeout as soon as there is no keep alive header info with timeout anymore
                                if (timeoutSeconds == -1)
                                {
                                    dataSocketChannel.socket().setSoTimeout(socketTimeout);
                                }
                            }

                            dataSocketChannel.write(response.toByteBuffer());
                            final boolean connection = "close".equals(response.getHeaders().get("Connection"));
                            if (connection)
                            {
                                dataSocketChannel.close();
                                keepAlive = false;
                            }

                            reading = false;
                        }
                    }
                }
            }
        }
        catch (AsynchronousCloseException e)
        {
            LOG.info("Connection was closed by client!");
            keepAlive = false;
        }
        catch (Throwable t)
        {
            attemptUnintentionalCloseResponse();
            close();
            LOG.error("Connection failed!", t);
            keepAlive = false;
        }

        return keepAlive ? KEEP_RUNNING : STOP_RUNNING;
    }

    private int handleConnectionLiveTime(@Nonnull final Request request, @Nonnull final Response response) throws SocketException
    {
        final String connection = request.getRequestHeaders().getHeader("connection");
        int timeoutSeconds = -1;
        if ("close".equalsIgnoreCase(connection))
        {
            response.setHeader("Connection", connection);
        }
        else
        {
            if (connection == null)
            {
                response.removeHeader("Connection");
            }
            else if ("keep-alive".equalsIgnoreCase(connection))
            {
                final String keepAliveHeader = request.getRequestHeaders().getHeader("keep-alive");
                if (keepAliveHeader != null)
                {
                    final String[] parameters = StringUtils.split(keepAliveHeader, ',');
                    for (final String parameter : parameters)
                    {
                        final String[] nameAndValue = StringUtils.split(parameter, '=');
                        if (nameAndValue.length == 2)
                        {
                            final String name = nameAndValue[0].trim();
                            final String value = nameAndValue[1].trim();
                            if ("timeout".equalsIgnoreCase(name))
                            {
                                timeoutSeconds = Integer.valueOf(value);
                                dataSocketChannel.socket().setSoTimeout(timeoutSeconds * 1000);
                            }
                            else if ("max".equalsIgnoreCase(name))
                            {
                                final int maxNumberOfRequests = Integer.valueOf(value);
                                if (maxNumberOfRequests <= 0)
                                {
                                    // reset counting as soon as the max is missing once
                                    numberOfRequestsReceived = 0;
                                }
                                else
                                {
                                    numberOfRequestsReceived++;
                                    if (maxNumberOfRequests <= numberOfRequestsReceived)
                                    {
                                        // this will trigger the close and even inform the client
                                        response.setHeader("Connection", "close");
                                    }
                                }
                            }
                        }
                    }
                }

                response.setHeader("Connection", connection);
            }
        }

        return timeoutSeconds;
    }

    private void attemptUnintentionalCloseResponse()
    {
        try
        {
            final Response response = Response.buildErrorResponse(HttpCode.INTERNAL_SERVER_ERROR);
            dataSocketChannel.write(response.toByteBuffer());
        }
        catch (IOException e)
        {
            LOG.warn("Failed to notify client upon unintended connection close: " + e.getMessage());
        }
    }

    private void close()
    {
        try
        {
            dataSocketChannel.close();
        }
        catch (IOException e)
        {
            LOG.warn("Failed closing socket!", e);
        }
    }
}
