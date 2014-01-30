package org.mosaic.web.application.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.PeriodFormatterBuilder;
import org.mosaic.modules.Component;
import org.mosaic.modules.Module;
import org.mosaic.modules.Property;
import org.mosaic.modules.ServiceRegistration;
import org.mosaic.util.collections.*;
import org.mosaic.util.properties.PropertyPlaceholderResolver;
import org.mosaic.util.resource.AntPathMatcher;
import org.mosaic.util.resource.PathMatcher;
import org.mosaic.util.resource.PathWatcher;
import org.mosaic.util.resource.PathWatcherContext;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.web.application.Application;
import org.mosaic.web.application.Resource;
import org.mosaic.web.security.SecurityConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import static java.nio.file.Files.exists;

/**
 * @author arik
 */
final class ApplicationImpl implements Application, Application.ApplicationSecurity, Application.ApplicationResources
{
    private static final Logger LOG = LoggerFactory.getLogger( ApplicationImpl.class );

    private static final String UNKNOWN_REALM_NAME = "org.mosaic.security.realm.unknown";

    private static final String UNKNOWN_PERMISSION_POLICY_NAME = "org.mosaic.security.permissionPolicies.unknown";

    private static final Resource UNKNOWN_RESOURCE = new Resource()
    {
        @Nonnull
        @Override
        public Path getPath()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isCompressionEnabled()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isBrowsingEnabled()
        {
            throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        public Period getCachePeriod()
        {
            throw new UnsupportedOperationException();
        }
    };

    static final PropertyPlaceholderResolver PROPERTY_PLACEHOLDER_RESOLVER = new PropertyPlaceholderResolver();

    @Nonnull
    static Period parsePeriod( @Nonnull String period )
    {
        PeriodFormatterBuilder builder = new PeriodFormatterBuilder().printZeroNever();
        if( period.contains( "year" ) )
        {
            builder.appendYears()
                   .appendSuffix( " year", " years" )
                   .appendSeparatorIfFieldsAfter( ", " );
        }
        if( period.contains( "month" ) )
        {
            builder.appendMonths()
                   .appendSuffix( " month", " months" )
                   .appendSeparatorIfFieldsAfter( ", " );
        }
        if( period.contains( "day" ) )
        {
            builder.appendDays()
                   .appendSuffix( " day", " days" )
                   .appendSeparatorIfFieldsAfter( ", " );
        }
        if( period.contains( "hour" ) )
        {
            builder.appendHours()
                   .appendSuffix( " hour", " hours" )
                   .appendSeparatorIfFieldsAfter( ", " );
        }
        if( period.contains( "minute" ) )
        {
            builder.appendMinutes()
                   .appendSuffix( " minute", " minutes" )
                   .appendSeparatorIfFieldsAfter( ", " );
        }
        if( period.contains( "second" ) )
        {
            builder.appendSeconds()
                   .appendSuffix( " second", " seconds" );
        }
        return builder.printZeroRarelyLast().toFormatter().parsePeriod( period ).normalizedStandard();
    }

    @Nonnull
    private final String id;

    @Nonnull
    private final Map<URI, XmlElement> parts = new LinkedHashMap<>();

    @Nonnull
    private final LoadingCache<String, Resource> resourceCache;

    @Nonnull
    @Component
    private Module module;

    @Nonnull
    @Component
    private ApplicationPartParser applicationPartParser;

    @Nullable
    private ServiceRegistration<Application> applicationRegistration;

    @Nullable
    private Collection<ServiceRegistration<PathWatcher>> watcherRegistrations;

    @Nonnull
    private String name = "Unknown";

    @Nonnull
    private MapEx<String, String> context = EmptyMapEx.emptyMapEx();

    @Nonnull
    private MapEx<String, Object> attributes = new ConcurrentHashMapEx<>( 100 );

    @Nonnull
    private Set<String> virtualHosts = Collections.emptySet();

    @Nonnull
    private Period maxSessionAge = new Period( 30, PeriodType.minutes() );

    @Nonnull
    private String realmName = UNKNOWN_REALM_NAME;

    @Nonnull
    private String permissionPolicyName = UNKNOWN_PERMISSION_POLICY_NAME;

    @Nonnull
    private Set<SecurityConstraintImpl> securityConstraints;

    @Nonnull
    private Set<Path> contentRoots = Collections.emptySet();

    ApplicationImpl( @Nonnull String id ) throws XPathException
    {
        this.id = id;
        this.resourceCache = CacheBuilder.from( "concurrencyLevel=100," +
                                                "initialCapacity=1000," +
                                                "maximumSize=1000000" ).build( new CacheLoader<String, Resource>()
        {
            @Nonnull
            @Override
            public Resource load( @Nonnull String path ) throws Exception
            {
                if( path.startsWith( "/" ) )
                {
                    path = path.substring( 1 );
                }

                Path p = Paths.get( path );
                if( p.isAbsolute() )
                {
                    return UNKNOWN_RESOURCE;
                }
                else if( !p.toString().isEmpty() )
                {
                    p = p.normalize();
                }

                for( Path contentRoot : ApplicationImpl.this.contentRoots )
                {
                    Path resolved = contentRoot.resolve( p ).toAbsolutePath().normalize();
                    if( exists( resolved ) )
                    {
                        return new ResourceImpl( contentRoot, resolved );
                    }
                }

                return UNKNOWN_RESOURCE;
            }
        } );
    }

    @Nonnull
    @Override
    public String getId()
    {
        return this.id;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return this.name;
    }

    @Nonnull
    @Override
    public MapEx<String, String> getContext()
    {
        return this.context;
    }

    @Nonnull
    @Override
    public MapEx<String, Object> getAttributes()
    {
        return this.attributes;
    }

    @Nonnull
    @Override
    public Set<String> getVirtualHosts()
    {
        return this.virtualHosts;
    }

    @Nonnull
    @Override
    public Period getMaxSessionAge()
    {
        return this.maxSessionAge;
    }

    @Nonnull
    @Override
    public ApplicationSecurity getSecurity()
    {
        return this;
    }

    @Nonnull
    @Override
    public ApplicationResources getResources()
    {
        return this;
    }

    @Nonnull
    @Override
    public String getRealmName()
    {
        return this.realmName;
    }

    @Nonnull
    @Override
    public String getPermissionPolicyName()
    {
        return this.permissionPolicyName;
    }

    @Nullable
    @Override
    public SecurityConstraint getConstraintForPath( @Nonnull String path )
    {
        PathMatcher pathMatcher = new AntPathMatcher();
        for( SecurityConstraintImpl constraint : this.securityConstraints )
        {
            if( pathMatcher.matches( constraint.getPath(), path ) )
            {
                return constraint;
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public Collection<Path> getContentRoots()
    {
        return this.contentRoots;
    }

    @Nullable
    @Override
    public Resource getResource( @Nonnull String path )
    {
        Resource resource = this.resourceCache.getUnchecked( path );
        if( resource == UNKNOWN_RESOURCE )
        {
            return null;
        }
        else
        {
            return resource;
        }
    }

    synchronized void addPart( @Nonnull URL file )
            throws ParserConfigurationException, SAXException, IOException, XPathException, URISyntaxException
    {
        URI key = file.toURI();
        if( !this.parts.containsKey( key ) )
        {
            Map<URI, XmlElement> partsMap = new LinkedHashMap<>( this.parts );
            partsMap.put( key, this.applicationPartParser.parse( file ) );
            update( partsMap );
        }
    }

    synchronized void addPart( @Nonnull Path file )
            throws ParserConfigurationException, SAXException, IOException, XPathException
    {
        URI key = file.toUri();
        if( !this.parts.containsKey( key ) )
        {
            Map<URI, XmlElement> partsMap = new LinkedHashMap<>( this.parts );
            partsMap.put( key, this.applicationPartParser.parse( file ) );
            update( partsMap );
        }
    }

    private synchronized void update( @Nonnull Map<URI,XmlElement> partsMap )
            throws ParserConfigurationException, SAXException, IOException, XPathException
    {
        Collection<XmlElement> parts = partsMap.values();

        String name = this.id;
        String maxSessionAgeString = "30 minutes";
        String realmName = UNKNOWN_REALM_NAME;
        String permissionPolicyName = UNKNOWN_PERMISSION_POLICY_NAME;
        MapEx<String, String> context = new LinkedHashMapEx<>( 20 );
        Set<String> virtualHosts = new LinkedHashSet<>( 10 );
        Set<SecurityConstraintImpl> securityConstraints = new LinkedHashSet<>();
        Set<Path> contentRoots = new LinkedHashSet<>();
        for( XmlElement appElt : parts )
        {
            name = appElt.find( "m:name", TypeToken.of( String.class ), name );
            maxSessionAgeString = appElt.find( "m:max-session-age", TypeToken.of( String.class ), maxSessionAgeString );
            realmName = appElt.find( "m:security/m:realm", TypeToken.of( String.class ), realmName );
            permissionPolicyName = appElt.find( "m:security/m:permission-policy", TypeToken.of( String.class ), permissionPolicyName );

            for( XmlElement parameterElt : appElt.findElements( "m:context/m:parameter" ) )
            {
                String value = parameterElt.getValue();
                if( value != null )
                {
                    context.put( parameterElt.requireAttribute( "name" ), PROPERTY_PLACEHOLDER_RESOLVER.resolve( value ) );
                }
            }

            for( XmlElement vhostElt : appElt.findElements( "m:virtual-hosts/m:virtual-host" ) )
            {
                virtualHosts.add( PROPERTY_PLACEHOLDER_RESOLVER.resolve( vhostElt.requireValue() ).toLowerCase() );
            }

            for( XmlElement constraintElt : appElt.findElements( "m:security/m:constraint" ) )
            {
                securityConstraints.add( new SecurityConstraintImpl( constraintElt ) );
            }

            for( String contentRootPath : appElt.findTexts( "m:resources/m:content-roots/m:content-root" ) )
            {
                contentRoots.add( Paths.get( PROPERTY_PLACEHOLDER_RESOLVER.resolve( contentRootPath ) ).normalize() );
            }
        }

        // parse period *before* applying changes, to make sure everything is valid
        Period maxSessionAge = parsePeriod( maxSessionAgeString );

        // apply changes
        this.name = name;
        this.maxSessionAge = maxSessionAge;
        this.realmName = realmName;
        this.permissionPolicyName = permissionPolicyName;
        this.context = UnmodifiableMapEx.of( context );
        this.virtualHosts = Collections.unmodifiableSet( virtualHosts );
        this.securityConstraints = Collections.unmodifiableSet( securityConstraints );
        this.contentRoots = Collections.unmodifiableSet( contentRoots );
        this.parts.clear();
        this.parts.putAll( partsMap );

        // re-register application
        unregister();
        this.applicationRegistration = this.module.getModuleWiring().register( Application.class, this, Property.property( "name", this.name ) );

        // re-register content root watchers
        Collection<ServiceRegistration<PathWatcher>> watcherRegistrations = new LinkedList<>();
        for( Path contentRoot : this.contentRoots )
        {
            try
            {
                watcherRegistrations.add( this.module.getModuleWiring().register(
                        PathWatcher.class,
                        new PathWatcher()
                        {
                            @Override
                            public void handle( @Nonnull PathWatcherContext context ) throws Exception
                            {
                                ApplicationImpl.this.resourceCache.invalidateAll();
                            }
                        },
                        Property.property( "location", contentRoot ) )
                );
            }
            catch( Exception e )
            {
                LOG.warn( "Could not register content root watcher for '{}' at '{}': {}", this.id, contentRoot, e.getMessage(), e );
            }
        }
        this.watcherRegistrations = watcherRegistrations;
    }

    synchronized void removePart( @Nonnull Path file )
            throws ParserConfigurationException, XPathException, SAXException, IOException
    {
        URI key = file.toUri();
        if( this.parts.containsKey( key ) )
        {
            Map<URI, XmlElement> partsMap = new LinkedHashMap<>( this.parts );
            partsMap.remove( key );
            update( partsMap );
        }
    }

    synchronized void removePart( @Nonnull URL file )
            throws ParserConfigurationException, XPathException, SAXException, IOException, URISyntaxException
    {
        URI key = file.toURI();
        if( this.parts.containsKey( key ) )
        {
            Map<URI, XmlElement> partsMap = new LinkedHashMap<>( this.parts );
            partsMap.remove( key );
            update( partsMap );
        }
    }

    synchronized void unregister()
    {
        if( this.applicationRegistration != null )
        {
            this.applicationRegistration.unregister();
            this.applicationRegistration = null;
        }
        if( this.watcherRegistrations != null )
        {
            for( ServiceRegistration<PathWatcher> registration : this.watcherRegistrations )
            {
                registration.unregister();
            }
            this.watcherRegistrations = null;
        }
    }
}
