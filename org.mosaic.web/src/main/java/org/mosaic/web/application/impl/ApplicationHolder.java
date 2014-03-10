package org.mosaic.web.application.impl;

import com.google.common.base.Optional;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.xpath.XPathException;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.Period;
import org.mosaic.modules.*;
import org.mosaic.server.Server;
import org.mosaic.util.collections.ConcurrentHashMapEx;
import org.mosaic.util.collections.LinkedHashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.collections.UnmodifiableMapEx;
import org.mosaic.util.properties.PropertyPlaceholderResolver;
import org.mosaic.util.resource.PathMatcher;
import org.mosaic.util.xml.XmlDocument;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.util.xml.XmlParser;
import org.mosaic.web.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import static java.nio.file.Files.exists;
import static org.mosaic.util.reflection.TypeTokens.STRING;

/**
 * @author arik
 */
final class ApplicationHolder
{
    private static final Logger LOG = LoggerFactory.getLogger( ApplicationHolder.class );

    private static final PropertyPlaceholderResolver PROPERTY_PLACEHOLDER_RESOLVER = new PropertyPlaceholderResolver();

    private static final String UNKNOWN_REALM_NAME = "org.mosaic.security.realm.unknown";

    private static final String UNKNOWN_PERMISSION_POLICY_NAME = "org.mosaic.security.permissionPolicies.unknown";

    @Nonnull
    private final Schema applicationSchema;

    @Nonnull
    private final Schema applicationFragmentSchema;

    @Nonnull
    private final String id;

    @Nonnull
    private final Set<Path> applicationFiles = new HashSet<>();

    @Nonnull
    private final Set<Path> contributionFiles = new HashSet<>();

    @Nonnull
    private final MapEx<String, Object> attributes = new ConcurrentHashMapEx<>( 100 );

    @Nonnull
    @Component
    private Module module;

    @Nonnull
    @Service
    private Server server;

    @Nonnull
    @Service
    private ModuleManager moduleManager;

    @Nonnull
    @Service
    private PathMatcher pathMatcher;

    @Nonnull
    @Service
    private XmlParser xmlParser;

    @Nullable
    private ServiceRegistration<Application> registration;

    ApplicationHolder( @Nonnull String id,
                       @Nonnull Schema applicationSchema,
                       @Nonnull Schema applicationFragmentSchema )
    {
        this.id = id;
        this.applicationSchema = applicationSchema;
        this.applicationFragmentSchema = applicationFragmentSchema;
    }

    void addApplicationFile( @Nonnull Path file )
    {
        this.applicationFiles.add( file.normalize().toAbsolutePath() );
        updateApplication();
    }

    void removeApplicationFile( @Nonnull Path file )
    {
        this.applicationFiles.remove( file.normalize().toAbsolutePath() );
        updateApplication();
    }

    void addContributionFile( @Nonnull Path file )
    {
        this.contributionFiles.add( file.normalize().toAbsolutePath() );
        updateApplication();
    }

    void removeContributionFile( @Nonnull Path file )
    {
        this.contributionFiles.remove( file.normalize().toAbsolutePath() );
        updateApplication();
    }

    private synchronized void updateApplication()
    {
        // parse all application files
        Map<Path, XmlElement> xmlElements = new LinkedHashMap<>();
        for( Path file : ApplicationHolder.this.contributionFiles )
        {
            try
            {
                xmlElements.put( file, parse( file, ApplicationHolder.this.applicationFragmentSchema ) );
            }
            catch( Throwable e )
            {
                LOG.error( "Could not read or parse application file at '{}': {}", file, e.getMessage(), e );
                return;
            }
        }
        for( Path file : ApplicationHolder.this.applicationFiles )
        {
            try
            {
                xmlElements.put( file, parse( file, ApplicationHolder.this.applicationSchema ) );
            }
            catch( Throwable e )
            {
                LOG.error( "Could not read or parse application file at '{}': {}", file, e.getMessage(), e );
                return;
            }
        }

        // if no application XML files left - destry the application
        if( xmlElements.isEmpty() )
        {
            unregisterApplication();
            LOG.info( "Application '{}' has been deleted", this.id );
        }
        else
        {
            ApplicationImpl application;
            try
            {
                application = new ApplicationImpl( xmlElements );
            }
            catch( Throwable e )
            {
                LOG.error( "Could not create/update application '{}': {}", this.id, e.getMessage(), e );
                return;
            }

            unregisterApplication();
            this.registration = this.module.register( Application.class, application, Property.property( "id", this.id ) );
            LOG.info( "Application '{}' has been added/updated", this.id );
        }
    }

    private synchronized void unregisterApplication()
    {
        ServiceRegistration<Application> appRegistration = this.registration;
        if( appRegistration != null )
        {
            appRegistration.unregister();
            this.registration = null;
        }
    }

    @Nonnull
    private XmlElement parse( @Nonnull Path file, @Nonnull Schema schema )
            throws IOException, SAXException, ParserConfigurationException
    {
        XmlDocument document = ApplicationHolder.this.xmlParser.parse( file, schema );
        document.addNamespace( "m", "http://www.mosaicserver.com/application-1.0.0" );
        return document.getRoot();
    }

    private class ApplicationImpl implements Application
    {
        @Nonnull
        private final String name;

        @Nonnull
        private final MapEx<String, String> context;

        @Nonnull
        private final Set<String> virtualHosts;

        @Nonnull
        private final Period maxSessionAge;

        @Nonnull
        private final String realmName;

        @Nonnull
        private final String permissionPolicyName;

        @Nonnull
        private final Set<SecuredPath> securityConstraints;

        @Nonnull
        private final Set<Path> contentRoots;

        private ApplicationImpl( Map<Path, XmlElement> xmlElements )
                throws IOException, SAXException, ParserConfigurationException, XPathException
        {
            String[] idTokens = StringUtils.splitByCharacterTypeCamelCase( ApplicationHolder.this.id );
            StringBuilder name = new StringBuilder();
            for( String token : idTokens )
            {
                String trimmed = token.trim();
                name.append( trimmed.isEmpty() ? " " : trimmed );
            }

            String nameHolder = name.toString();
            MapEx<String, String> context = new LinkedHashMapEx<>( 20 );
            Set<String> virtualHosts = new LinkedHashSet<>( 10 );
            String maxSessionAgeString = "30 minutes";
            Set<SecuredPath> securityConstraints = new LinkedHashSet<>();
            String realmName = UNKNOWN_REALM_NAME;
            String permissionPolicyName = UNKNOWN_PERMISSION_POLICY_NAME;
            Set<Path> contentRoots = new LinkedHashSet<>();

            for( Map.Entry<Path, XmlElement> entry : xmlElements.entrySet() )
            {
                Path file = entry.getKey();
                XmlElement appElt = entry.getValue();

                Optional<String> nameValue = appElt.find( "m:name", STRING );
                if( nameValue.isPresent() )
                {
                    nameHolder = nameValue.get();
                }

                for( XmlElement parameterElt : appElt.findElements( "m:context/m:parameter" ) )
                {
                    Optional<String> value = parameterElt.getValue();
                    if( value.isPresent() )
                    {
                        context.put( parameterElt.getAttribute( "name" ).get(), PROPERTY_PLACEHOLDER_RESOLVER.resolve( value.get() ) );
                    }
                }

                for( String vhost : appElt.findTexts( "m:virtual-hosts/m:virtual-host" ) )
                {
                    virtualHosts.add( PROPERTY_PLACEHOLDER_RESOLVER.resolve( vhost ).toLowerCase() );
                }

                maxSessionAgeString = appElt.find( "m:max-session-age", STRING ).or( maxSessionAgeString );

                realmName = appElt.find( "m:security/m:realm", STRING ).or( realmName );
                permissionPolicyName = appElt.find( "m:security/m:permission-policy", STRING ).or( permissionPolicyName );
                for( XmlElement constraintElt : appElt.findElements( "m:security/m:constraint" ) )
                {
                    securityConstraints.add( new SecuredPathImpl( constraintElt ) );
                }

                for( String contentRootPath : appElt.findTexts( "m:resources/m:content-roots/m:content-root" ) )
                {
                    contentRoots.add( Paths.get( PROPERTY_PLACEHOLDER_RESOLVER.resolve( contentRootPath ) ).normalize().toAbsolutePath() );
                }

                if( file.toString().equals( "/WEB-INF/application.xml" ) )
                {
                    Path contentRoot = file.getParent();

                    String host = file.toUri().getHost();
                    if( host != null && !host.isEmpty() )
                    {
                        try
                        {
                            int moduleId = Integer.parseInt( host );
                            Optional<? extends Module> moduleHolder = moduleManager.getModule( moduleId );
                            if( moduleHolder.isPresent() )
                            {
                                Module module = moduleHolder.get();
                                Optional<String> bundleResourcesHeader = module.getHeaders().find( "Bundle-Resources" );
                                if( bundleResourcesHeader.isPresent() )
                                {
                                    String localResourcesLocation = bundleResourcesHeader.get();
                                    if( localResourcesLocation != null && !localResourcesLocation.isEmpty() )
                                    {
                                        contentRoot = Paths.get( localResourcesLocation ).resolve( "WEB-INF" );
                                    }
                                }
                            }
                        }
                        catch( NumberFormatException ignore )
                        {
                        }
                    }
                    contentRoots.add( contentRoot );
                }
            }

            // parse period *before* applying changes, to make sure everything is valid
            Period maxSessionAge = ApplicationManager.parsePeriod( maxSessionAgeString );

            this.name = nameHolder;
            this.context = UnmodifiableMapEx.of( context );
            this.virtualHosts = Collections.unmodifiableSet( virtualHosts );
            this.maxSessionAge = maxSessionAge;
            this.realmName = realmName;
            this.permissionPolicyName = permissionPolicyName;
            this.securityConstraints = Collections.unmodifiableSet( securityConstraints );
            this.contentRoots = Collections.unmodifiableSet( contentRoots );
        }

        @Nonnull
        @Override
        public String getId()
        {
            return ApplicationHolder.this.id;
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
            return ApplicationHolder.this.attributes;
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
        public Collection<Path> getContentRoots()
        {
            return this.contentRoots;
        }

        @Nullable
        @Override
        public ApplicationResource getResource( @Nonnull String path )
        {
            // TODO: cache application resources

            if( path.startsWith( "/" ) )
            {
                path = path.substring( 1 );
            }

            Path p = Paths.get( path );
            if( p.isAbsolute() )
            {
                return null;
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
                    return new ApplicationResourceImpl( contentRoot, resolved );
                }
            }

            return null;
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
        public Application.SecuredPath getConstraintForPath( @Nonnull String path )
        {
            for( SecuredPath constraint : this.securityConstraints )
            {
                if( ApplicationHolder.this.pathMatcher.matches( constraint.getPath(), path ) )
                {
                    return constraint;
                }
            }
            return null;
        }
    }
}
