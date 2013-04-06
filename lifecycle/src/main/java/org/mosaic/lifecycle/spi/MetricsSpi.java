package org.mosaic.lifecycle.spi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.Module;
import org.mosaic.lifecycle.ModuleManager;

/**
 * @author arik
 */
public final class MetricsSpi
{
    private static MetricsSpi instance;

    @SuppressWarnings( "UnusedDeclaration" )
    @Nullable
    public static Module.MetricsTimer getTimer( @Nonnull Class<?> type, @Nonnull String method )
    {
        MetricsSpi spi = MetricsSpi.instance;
        if( spi == null )
        {
            throw new IllegalStateException( "Metrics SPI has not been created" );
        }

        Module module = spi.moduleManager.getModuleFor( type );
        if( module == null )
        {
            throw new IllegalArgumentException( "Type '" + type.getName() + "' was not loaded by Mosaic" );
        }

        Module.Metrics metrics = module.getMetrics();
        if( metrics == null )
        {
            return null;
        }
        else
        {
            return metrics.getTimer( type.getPackage().getName(), type.getSimpleName(), method );
        }
    }

    @Nonnull
    private final ModuleManager moduleManager;

    public MetricsSpi( @Nonnull ModuleManager moduleManager )
    {
        if( MetricsSpi.instance != null )
        {
            throw new IllegalStateException( "Metrics SPI has already been created!" );
        }

        this.moduleManager = moduleManager;
        MetricsSpi.instance = this;
    }
}
