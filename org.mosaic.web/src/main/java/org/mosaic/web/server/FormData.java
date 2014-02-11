package org.mosaic.web.server;

import com.google.common.collect.Multimap;
import javax.annotation.Nonnull;
import org.mosaic.util.collections.MapEx;

/**
 * @author arik
 */
public interface FormData
{
    @Nonnull
    MapEx<String, String> asMap();

    @Nonnull
    Multimap<String, String> asMultimap();
}
