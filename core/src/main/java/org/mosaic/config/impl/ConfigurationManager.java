package org.mosaic.config.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.mosaic.Server;
import org.mosaic.lifecycle.DP;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.lifecycle.ServicePropertiesProvider;
import org.mosaic.lifecycle.annotation.*;
import org.mosaic.util.collect.HashMapEx;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.io.FileVisitorAdapter;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Multimaps.synchronizedMultimap;

/**
 * @author arik
 */
@Service( FileVisitor.class )
public class ConfigurationManager extends FileVisitorAdapter implements ServicePropertiesProvider
{
    private static final Logger LOG = LoggerFactory.getLogger( ConfigurationManager.class );

    @Nonnull
    private Server server;

    @Nonnull
    private ConversionService conversionService;

    @Nullable
    private Multimap<String, MethodEndpoint> endpoints;

    @Nullable
    private Map<String, Properties> configurations;

    @ServiceRef
    public void setServer( @Nonnull Server server )
    {
        this.server = server;
    }

    @ServiceRef
    public void setConversionService( @Nonnull ConversionService conversionService )
    {
        this.conversionService = conversionService;
    }

    @Nonnull
    @Override
    public DP[] getServiceProperties()
    {
        return new DP[] {
                DP.dp( "root", this.server.getEtc() ),
                DP.dp( "pattern", "**/*.properties" )
        };
    }

    @PostConstruct
    public void init()
    {
        this.configurations = new ConcurrentHashMap<>( 50 );
        this.endpoints = synchronizedMultimap( ArrayListMultimap.<String, MethodEndpoint>create() );
    }

    @PreDestroy
    public void destory()
    {
        this.endpoints = null;
        this.configurations = null;
    }

    @ServiceBind( updates = false )
    public void addConfigurable( @Nonnull MethodEndpoint endpoint, @Nullable @ServiceProperty String type )
    {
        Multimap<String, MethodEndpoint> endpoints = this.endpoints;
        if( endpoints != null )
        {
            if( Configurable.class.getName().equals( type ) )
            {
                Configurable ann = ( Configurable ) endpoint.getType();
                endpoints.put( ann.value(), endpoint );

                Map<String, Properties> configurations = this.configurations;
                if( configurations != null )
                {
                    Properties properties = configurations.get( ann.value() );
                    if( properties != null )
                    {
                        notifyConfigurable( properties, endpoint );
                    }
                }
            }
        }
    }

    @ServiceUnbind
    public void removeConfigurable( @Nonnull MethodEndpoint endpoint, @Nullable @ServiceProperty String type )
    {
        Multimap<String, MethodEndpoint> endpoints = this.endpoints;
        if( endpoints != null )
        {
            if( Configurable.class.getName().equals( type ) )
            {
                Configurable ann = ( Configurable ) endpoint.getType();
                endpoints.remove( ann.value(), endpoint );
            }
        }
    }

    @Override
    protected FileVisitResult onFileModified( @Nonnull Path file,
                                              @SuppressWarnings( "UnusedParameters" ) @Nonnull BasicFileAttributes attrs )
            throws IOException
    {
        Map<String, Properties> configurations = this.configurations;
        if( configurations == null )
        {
            return FileVisitResult.TERMINATE;
        }

        // load configuration data
        final Properties properties = new Properties();
        properties.load( new ByteArrayInputStream( Files.readAllBytes( file ) ) );

        // store (possibly replace previous) configuration in memory
        String fileName = file.getFileName().toString();
        String configurationName = fileName.substring( 0, fileName.length() - ".properties".length() );
        configurations.put( configurationName, properties );

        // notify interested endpoints
        Multimap<String, MethodEndpoint> endpoints = this.endpoints;
        if( endpoints != null )
        {
            for( MethodEndpoint endpoint : endpoints.get( configurationName ) )
            {
                notifyConfigurable( properties, endpoint );
            }
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    protected FileVisitResult onFileDeleted( @Nonnull Path file ) throws IOException
    {
        Map<String, Properties> configurations = this.configurations;
        if( configurations == null )
        {
            return FileVisitResult.TERMINATE;
        }

        // store (possibly replace previous) configuration in memory
        String fileName = file.getFileName().toString();
        String configurationName = fileName.substring( 0, fileName.length() - ".properties".length() );
        configurations.remove( configurationName );

        // notify interested endpoints
        Multimap<String, MethodEndpoint> endpoints = this.endpoints;
        if( endpoints != null )
        {
            final Properties emptyProperties = new Properties();
            for( MethodEndpoint endpoint : endpoints.get( configurationName ) )
            {
                notifyConfigurable( emptyProperties, endpoint );
            }
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    protected FileVisitResult onError( @Nonnull Path file,
                                       @Nonnull IOException exception ) throws IOException
    {
        LOG.error( "Could not process configuration file at '{}': {}", file, exception.getMessage(), exception );
        return FileVisitResult.CONTINUE;
    }

    private void notifyConfigurable( final Properties properties, MethodEndpoint endpoint )
    {
        try
        {
            endpoint.createInvoker( new MethodHandle.ParameterResolver()
            {
                @Nullable
                @Override
                public Object resolve( @Nonnull MethodParameter parameter,
                                       @Nonnull MapEx<String, Object> resolveContext )
                {
                    if( parameter.isMapEx() )
                    {
                        return new HashMapEx<>( Maps.fromProperties( properties ), conversionService );
                    }
                    else if( parameter.isMap() )
                    {
                        return Maps.fromProperties( properties );
                    }
                    else if( parameter.isProperties() )
                    {
                        return properties;
                    }
                    else
                    {
                        throw new MethodHandle.UnresolvableArgumentException( "unsupported @Configurable parameter type '" + parameter.getType() + "'", parameter );
                    }
                }
            } ).resolve( Collections.<String, Object>emptyMap() ).invoke();
        }
        catch( Exception e )
        {
            LOG.error( "Could not provide configuration to @Configurable '{}': {}", endpoint, e.getMessage(), e );
        }
    }
}
