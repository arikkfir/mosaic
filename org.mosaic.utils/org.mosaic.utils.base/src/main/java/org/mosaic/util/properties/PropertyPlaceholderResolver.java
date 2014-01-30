package org.mosaic.util.properties;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.springframework.util.PropertyPlaceholderHelper;

/**
 * @author arik
 */
public final class PropertyPlaceholderResolver
{
    private static class MapResolver implements Resolver
    {
        @Nonnull
        private final Map<String, String> properties;

        private MapResolver( @Nonnull Properties properties )
        {
            this( Maps.fromProperties( properties ) );
        }

        private MapResolver( @Nonnull Map<String, String> properties )
        {
            this.properties = new ConcurrentHashMap<>( properties );
        }

        @Nullable
        @Override
        public final String resolve( @Nonnull String propertyName )
        {
            return this.properties.get( propertyName );
        }
    }

    private static final class BundleContextResolver implements Resolver
    {
        @Nullable
        @Override
        public String resolve( @Nonnull String propertyName )
        {
            Bundle bundle = FrameworkUtil.getBundle( getClass() );
            if( bundle != null )
            {
                BundleContext bundleContext = bundle.getBundleContext();
                if( bundleContext != null )
                {
                    return bundleContext.getProperty( propertyName );
                }
            }
            return null;
        }
    }

    @Nonnull
    private final ResolverAdapter resolver;

    @Nonnull
    private final PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper( "${", "}", ":", false );

    public PropertyPlaceholderResolver()
    {
        this( new BundleContextResolver() );
    }

    public PropertyPlaceholderResolver( @Nonnull Properties properties )
    {
        this( new MapResolver( properties ) );
    }

    public PropertyPlaceholderResolver( @Nonnull Map<String, String> properties )
    {
        this( new MapResolver( properties ) );
    }

    public PropertyPlaceholderResolver( @Nonnull Resolver resolver )
    {
        this.resolver = new ResolverAdapter( resolver );
    }

    @Nonnull
    public String resolve( @Nonnull String input )
    {
        return this.propertyPlaceholderHelper.replacePlaceholders( input, this.resolver );
    }

    public static interface Resolver
    {
        @Nullable
        String resolve( @Nonnull String propertyName );
    }

    private final class ResolverAdapter implements PropertyPlaceholderHelper.PlaceholderResolver
    {
        @Nonnull
        private final Resolver resolver;

        private ResolverAdapter( @Nonnull Resolver resolver )
        {
            this.resolver = resolver;
        }

        @Override
        public final String resolvePlaceholder( String placeholderName )
        {
            return this.resolver.resolve( placeholderName );
        }
    }
}
