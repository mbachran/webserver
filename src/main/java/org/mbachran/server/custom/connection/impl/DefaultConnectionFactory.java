package org.mbachran.server.custom.connection.impl;

import org.mbachran.server.custom.connection.api.Connection;
import org.mbachran.server.custom.connection.api.ConnectionFactory;
import org.mbachran.server.custom.dispatch.api.Dispatcher;
import org.mbachran.server.custom.parser.api.RequestParserFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.nio.channels.SocketChannel;
import java.util.List;

@Component
public class DefaultConnectionFactory implements ConnectionFactory
{
    private final int socketTimeout;

    private final int readBufferSize;

    private final RequestParserFactory requestParserFactory;

    private final Dispatcher dispatcher;

    /**
     * @param socketTimeout        The default timeout to be used for the data sockets.
     * @param requestParserFactory The factory to use for factoring request parsers.
     * @param dispatchers           The start of the dispatch chain.
     */
    @Autowired
    public DefaultConnectionFactory(@Value("${application.config.custom-server.connection.socket-timeout:60}") final int socketTimeout,
                                    @Value("${application.config.custom-server.connection.read-buffer-size:8192}") final int readBufferSize,
                                    @Value("${application.config.custom-server.dispatch-chain.start:defaultDispatcher}") final String dispatcherName,
                                    @Nonnull final RequestParserFactory requestParserFactory,
                                    @Nonnull final List<Dispatcher> dispatchers)
    {
        this.socketTimeout = socketTimeout;
        this.readBufferSize = readBufferSize;
        this.requestParserFactory = requestParserFactory;
        this.dispatcher = dispatchers.stream()
                .filter(d->d.getName().equals(dispatcherName))
                .findFirst()
                .orElseThrow(()->new IllegalArgumentException("No such dispatcher " + dispatcherName + ". Make sure to fix the server config."));
    }

    @Nonnull
    @Override
    public Connection create(final long creationTime, @Nonnull final SocketChannel dataSocketChannel)
    {
        return new DefaultConnection(socketTimeout, readBufferSize, requestParserFactory, creationTime, dataSocketChannel, dispatcher);
    }
}
