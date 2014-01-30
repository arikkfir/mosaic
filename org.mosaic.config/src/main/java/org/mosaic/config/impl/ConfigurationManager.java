package org.mosaic.config.impl;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.DateTime;
import org.mosaic.config.Configurable;
import org.mosaic.modules.*;
import org.mosaic.pathwatchers.OnPathCreated;
import org.mosaic.pathwatchers.OnPathDeleted;
import org.mosaic.pathwatchers.OnPathModified;
import org.mosaic.server.Server;
import org.mosaic.util.collections.EmptyMapEx;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.method.MethodParameter;
import org.mosaic.util.method.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.walkFileTree;

/**
 * @author arik
 */
@Component
final class ConfigurationManager
{
    private static final Logger LOG = LoggerFactory.getLogger( ConfigurationManager.class );

    private static final String CFGS_PATTERN = "${mosaic.home.etc}/**/*.properties";

    private static final TypeToken<MapEx<String, String>> MAP_EX_TYPE_TOKEN = new TypeToken<MapEx<String, String>>()
    {
    };

    @Service
    @Nonnull
    private Server server;

    @Nonnull
    private Map<Long, ConfigurableAdapter> configurationAdapters = new ConcurrentHashMap<>();

    @OnServiceAdded
    synchronized void addConfigurableEndpoint( @Nonnull ServiceReference<MethodEndpoint<Configurable>> reference )
            throws IOException
    {
        Optional<MethodEndpoint<Configurable>> endpoint = reference.service();
        if( !endpoint.isPresent() )
        {
            return;
        }

        final ConfigurableAdapter adapter = new ConfigurableAdapter( endpoint.get() );
        walkFileTree( this.server.getEtcPath(), new SimpleFileVisitor<Path>()
        {
            @Nonnull
            @Override
            public FileVisitResult visitFile( @Nonnull Path file, @Nonnull BasicFileAttributes attrs )
                    throws IOException
            {
                String fileName = file.getFileName().toString();
                if( fileName.toLowerCase().endsWith( ".properties" ) )
                {
                    DateTime modificationTime = new DateTime( getLastModifiedTime( file ).toMillis() );
                    adapter.handle( fileName.substring( 0, fileName.lastIndexOf( '.' ) ), readConfiguration( file ), modificationTime );
                }
                return FileVisitResult.CONTINUE;
            }
        } );
        this.configurationAdapters.put( reference.getId(), adapter );
    }

    @OnServiceRemoved
    synchronized void removeConfigurableEndpoint( @Nonnull ServiceReference<MethodEndpoint<Configurable>> reference )
    {
        this.configurationAdapters.remove( reference.getId() );
    }

    @OnPathCreated( CFGS_PATTERN )
    @OnPathModified(CFGS_PATTERN)
    void configurationAddedOrModified( @Nonnull Path file ) throws IOException
    {
        String fileName = file.getFileName().toString();
        DateTime modificationTime = new DateTime( getLastModifiedTime( file ).toMillis() );
        notify( fileName.substring( 0, fileName.lastIndexOf( '.' ) ), readConfiguration( file ), modificationTime );
    }

    @OnPathDeleted(CFGS_PATTERN)
    void configurationDeleted( @Nonnull Path file )
    {
        String fileName = file.getFileName().toString();
        String configurationName = fileName.substring( 0, fileName.lastIndexOf( '.' ) );
        notify( configurationName, EmptyMapEx.<String, String>emptyMapEx(), DateTime.now() );
    }

    @Nonnull
    private MapEx<String, String> readConfiguration( @Nonnull Path file )
    {
        try( Reader reader = Files.newBufferedReader( file, Charset.forName( "UTF-8" ) ) )
        {
            Properties properties = new Properties();
            properties.load( reader );

            MapEx<String, String> cfg = new HashMapEx<>( 100 );
            for( String propertyName : properties.stringPropertyNames() )
            {
                cfg.put( propertyName, properties.getProperty( propertyName ) );
            }
            return cfg;
        }
        catch( IOException e )
        {
            LOG.warn( "Could not read new/updated configuration file at '{}': {}", file, e.getMessage(), e );
            return EmptyMapEx.emptyMapEx();
        }
    }

    private synchronized void notify( @Nonnull String configurationName,
                                      @Nonnull MapEx<String, String> cfg,
                                      @Nonnull DateTime modificationTime )
    {
        for( ConfigurableAdapter adapter : this.configurationAdapters.values() )
        {
            adapter.handle( configurationName, cfg, modificationTime );
        }
    }

    private class ConfigurableAdapter
    {
        @Nonnull
        private final MethodEndpoint<Configurable> endpoint;

        private final MethodEndpoint.Invoker invoker;

        @Nonnull
        private final String configurationName;

        @Nonnull
        private DateTime lastChange = new DateTime( 0 );

        private ConfigurableAdapter( @Nonnull MethodEndpoint<Configurable> endpoint )
        {
            this.endpoint = endpoint;
            this.configurationName = this.endpoint.getType().value();
            this.invoker = this.endpoint.createInvoker(
                    new ParameterResolver<Object>()
                    {
                        @Nullable
                        @Override
                        public Optional<Object> resolve( @Nonnull MethodParameter parameter,
                                                         @Nonnull MapEx<String, Object> resolveContext )
                                throws Exception
                        {
                            if( parameter.getType().isAssignableFrom( MAP_EX_TYPE_TOKEN ) )
                            {
                                return resolveContext.find( "cfg" );
                            }
                            return Optional.absent();
                        }
                    }
            );
        }

        public void handle( @Nonnull String configurationName,
                            @Nonnull MapEx<String, String> cfg,
                            @Nonnull DateTime modificationTime )
        {
            if( configurationName.equalsIgnoreCase( this.configurationName ) && this.lastChange.isBefore( modificationTime ) )
            {
                this.lastChange = modificationTime;

                MapEx<String, Object> context = new HashMapEx<>();
                context.put( "cfg", cfg );
                try
                {
                    this.invoker.resolve( context ).invoke();
                }
                catch( Throwable e )
                {
                    LOG.error( "Configuration listener '{}' failed: {}", this.endpoint, e.getMessage(), e );
                }
            }
        }
    }
}
