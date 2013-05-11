package org.mosaic.filewatch.impl.manager;

import javax.annotation.Nonnull;
import org.mosaic.util.collect.HashMapEx;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;

/**
 * @author arik
 */
public class ScanContext
{
    @Nonnull
    private final MapEx<String, Object> attributes;

    public ScanContext( @Nonnull ConversionService conversionService )
    {
        this.attributes = new HashMapEx<>( conversionService );
    }

    @Nonnull
    public MapEx<String, Object> getAttributes()
    {
        return this.attributes;
    }
}
