package org.mbachran.server.custom.parser.impl;

import org.mbachran.server.custom.parser.api.RequestParser;
import org.mbachran.server.custom.parser.api.RequestParserFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.List;

/**
 * The default factory not providing any dynamic mechanism so far to select various parser implementations but always delivering
 * {@link DefaultRequestParser}.
 *
 * {@link BodyParser}s are injected mapped by {@link TransferEncoding}.
 */
@Component
public class DefaultRequestParserFactory implements RequestParserFactory
{
    private final EnumMap<TransferEncoding, BodyParser> bodyParsers = new EnumMap<>(TransferEncoding.class);

    @Autowired
    public DefaultRequestParserFactory(@Nonnull final List<BodyParser> bodyParsers)
    {
        bodyParsers.forEach(p -> this.bodyParsers.put(p.getTransferEncoding(), p));
    }

    @Nonnull
    @Override
    public RequestParser create()
    {
        return new DefaultRequestParser(bodyParsers);
    }
}
