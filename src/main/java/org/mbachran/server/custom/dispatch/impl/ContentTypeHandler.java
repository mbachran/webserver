package org.mbachran.server.custom.dispatch.impl;

import org.mbachran.server.custom.dispatch.api.Dispatcher;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Interface for handlers that claim to support a certain set of content types.
 */
public interface ContentTypeHandler extends Dispatcher
{
    /**
     * Supporting multiple content types in one handler eases up handler reuse.
     *
     * @return The set of content types supported by the handler.
     */
    @Nonnull
    List<String> getSupportedContentTypes();
}
