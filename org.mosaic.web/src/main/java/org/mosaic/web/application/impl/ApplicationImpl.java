package org.mosaic.web.application.impl;

import com.google.common.reflect.TypeToken;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import org.mosaic.util.xml.XmlElement;
import org.mosaic.web.application.Application;

import static java.nio.file.Files.exists;

/**
 * @author arik
 */
final class ApplicationImpl implements Application, Application.ApplicationSecurity, Application.ApplicationResources
{
    private static final String UNKNOWN_REALM = "org.mosaic.security.realm.unknown";

    private static final String UNKNOWN_PERMISSION_POLICY = "org.mosaic.security.permissionPolicies.unknown";

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
    @Component
    private Module module;

    @Nullable
    private ServiceRegistration<Application> registration;

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
    private String realmName = UNKNOWN_REALM;

    @Nonnull
    private String permissionPolicyName = UNKNOWN_PERMISSION_POLICY;

    @Nonnull
    private Set<SecurityConstraintImpl> securityConstraints;

    @Nonnull
    private Set<Path> contentRoots = Collections.emptySet();

    ApplicationImpl( @Nonnull String id, @Nonnull XmlElement appElt ) throws XPathException
    {
        this.id = id;
        parse( appElt );
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
            for( String constraintPath : constraint.getPaths() )
            {
                if( pathMatcher.matches( constraintPath, path ) )
                {
                    return constraint;
                }
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public Collection<? extends SecurityConstraint> getConstraints()
    {
        return this.securityConstraints;
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
        if( path.startsWith( "/" ) )
        {
            path = path.substring( 1 );
        }

        Path p = Paths.get( path );
        if( p.isAbsolute() )
        {
            return null;
        }
        else
        {
            p = p.normalize();
        }

        for( Path contentRoot : this.contentRoots )
        {
            Path resolved = contentRoot.resolve( p ).toAbsolutePath();
            if( exists( resolved ) )
            {
                return new ResourceImpl( contentRoot, resolved );
            }
        }

        return null;
    }

    void parse( @Nonnull XmlElement appElt ) throws XPathException
    {
        String name = appElt.find( "m:name", TypeToken.of( String.class ), this.id );
        Period maxSessionAge = parsePeriod( appElt.find( "m:max-session-age", TypeToken.of( String.class ), "30 minutes" ) );
        String realmName = appElt.find( "m:security/m:realm", TypeToken.of( String.class ), UNKNOWN_REALM );
        String permissionPolicyName = appElt.find( "m:security/m:permission-policy", TypeToken.of( String.class ), UNKNOWN_PERMISSION_POLICY );

        MapEx<String, String> context = new LinkedHashMapEx<>( 20 );
        for( XmlElement parameterElt : appElt.findElements( "m:context/m:parameter" ) )
        {
            String value = parameterElt.getValue();
            if( value != null )
            {
                context.put( parameterElt.requireAttribute( "name" ), PROPERTY_PLACEHOLDER_RESOLVER.resolve( value ) );
            }
        }

        Set<String> virtualHosts = new LinkedHashSet<>( 10 );
        for( XmlElement vhostElt : appElt.findElements( "m:virtual-hosts/m:virtual-host" ) )
        {
            virtualHosts.add( PROPERTY_PLACEHOLDER_RESOLVER.resolve( vhostElt.requireValue() ).toLowerCase() );
        }

        Set<SecurityConstraintImpl> securityConstraints = new LinkedHashSet<>();
        for( XmlElement constraintElt : appElt.findElements( "m:security/m:constraint" ) )
        {
            securityConstraints.add( new SecurityConstraintImpl( constraintElt ) );
        }

        Set<Path> contentRoots = new LinkedHashSet<>();
        for( String contentRootPath : appElt.findTexts( "m:resources/m:content-roots/m:content-root" ) )
        {
            contentRoots.add( Paths.get( PROPERTY_PLACEHOLDER_RESOLVER.resolve( contentRootPath ) ) );
        }

        this.name = name;
        this.context = UnmodifiableMapEx.of( context );
        this.virtualHosts = Collections.unmodifiableSet( virtualHosts );
        this.maxSessionAge = maxSessionAge;
        this.realmName = realmName;
        this.permissionPolicyName = permissionPolicyName;
        this.securityConstraints = Collections.unmodifiableSet( securityConstraints );
        this.contentRoots = Collections.unmodifiableSet( contentRoots );

        unregister();
        this.registration = this.module.getModuleWiring().register( Application.class, this, Property.property( "name", this.name ) );
    }

    void unregister()
    {
        if( this.registration != null )
        {
            this.registration.unregister();
            this.registration = null;
        }
    }
}
