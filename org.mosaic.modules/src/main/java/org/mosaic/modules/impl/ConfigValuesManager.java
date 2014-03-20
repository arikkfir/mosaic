package org.mosaic.modules.impl;

import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.ConfigValue;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.resource.support.PathWatcherAdapter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;
import static org.mosaic.modules.impl.Activator.getConversionService;

/**
 * @author arik
 */
final class ConfigValuesManager extends PathWatcherAdapter
{
    private static final Logger LOG = LoggerFactory.getLogger( ConfigValuesManager.class );

    @Nonnull
    private final Map<Path, ConfigScopeImpl> scopes = new ConcurrentHashMap<>();

    @Override
    public synchronized void pathCreated( @Nonnull Path path, @Nonnull MapEx<String, Object> context )
    {
        ConfigScopeImpl scope = this.scopes.get( path );
        if( scope == null )
        {
            scope = new ConfigScopeImpl( path );
            this.scopes.put( path, scope );
        }
        scope.refresh();
    }

    @Override
    public void pathModified( @Nonnull Path path, @Nonnull MapEx<String, Object> context )
    {
        ConfigScopeImpl scope = this.scopes.get( path );
        if( scope == null )
        {
            scope = new ConfigScopeImpl( path );
            this.scopes.put( path, scope );
        }
        scope.refresh();
    }

    @Override
    public void pathDeleted( @Nonnull Path path, @Nonnull MapEx<String, Object> context )
    {
        ConfigScopeImpl scope = this.scopes.remove( path );
        if( scope != null )
        {
            scope.unregister();
        }
    }

    private class ConfigScopeImpl
    {
        @Nonnull
        private final Path path;

        @Nonnull
        private final String scope;

        @Nonnull
        private Map<String, ConfigValueImpl> values = Collections.emptyMap();

        private ConfigScopeImpl( @Nonnull Path path )
        {
            this.path = path;

            String fileName = this.path.getFileName().toString();
            this.scope = fileName.substring( 0, fileName.lastIndexOf( '.' ) );
        }

        private synchronized void refresh()
        {
            Properties properties;
            try( Reader reader = Files.newBufferedReader( this.path, Charset.forName( "UTF-8" ) ) )
            {
                properties = new Properties();
                properties.load( reader );
            }
            catch( IOException e )
            {
                LOG.warn( "Could not read new/updated configuration file at '{}': {}", this.path, e.getMessage(), e );
                return;
            }

            Map<String, ConfigValueImpl> oldValues = this.values;

            // first register new values as alternatives
            Map<String, ConfigValueImpl> newValues = new HashMap<>();
            for( String key : properties.stringPropertyNames() )
            {
                ConfigValueImpl configValue = new ConfigValueImpl( key, properties.getProperty( key ) );
                newValues.put( key, configValue );
                configValue.register();
            }
            this.values = newValues;

            // and only then remove the old ones
            for( ConfigValueImpl value : oldValues.values() )
            {
                value.unregister();
            }
        }

        private synchronized void unregister()
        {
            for( ConfigValueImpl value : this.values.values() )
            {
                value.unregister();
            }
            this.values = Collections.emptyMap();

        }

        private class ConfigValueImpl implements ConfigValue
        {
            @Nonnull
            private final String key;

            @Nonnull
            private final String value;

            @Nullable
            private ServiceRegistration<? extends ConfigValue> registration;

            private ConfigValueImpl( @Nonnull String key, @Nonnull String value )
            {
                this.key = key;
                this.value = value;
            }

            @Nonnull
            @Override
            public String getScope()
            {
                return scope;
            }

            @Nonnull
            @Override
            public String getKey()
            {
                return this.key;
            }

            @Nonnull
            @Override
            public String getValue()
            {
                return this.value;
            }

            @Nonnull
            @Override
            public <T> T getValue( @Nonnull TypeToken<T> typeToken )
            {
                return requireNonNull( getConversionService().convert( this.value, typeToken ) );
            }

            private void register()
            {
                Bundle bundle = FrameworkUtil.getBundle( getClass() );
                if( bundle != null )
                {
                    BundleContext bundleContext = bundle.getBundleContext();
                    if( bundleContext != null )
                    {
                        Dictionary<String, Object> dict = new Hashtable<>();
                        dict.put( "path", path );
                        dict.put( "scope", scope );
                        dict.put( "key", this.key );
                        this.registration = bundleContext.registerService( ConfigValue.class, this, null );
                    }
                }
            }

            private void unregister()
            {
                ServiceRegistration<? extends ConfigValue> registration = this.registration;
                if( registration != null )
                {
                    try
                    {
                        registration.unregister();
                    }
                    catch( Exception ignore )
                    {
                    }
                    this.registration = null;
                }
            }
        }
    }
}
