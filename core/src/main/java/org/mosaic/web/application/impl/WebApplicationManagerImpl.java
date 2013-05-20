package org.mosaic.web.application.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.mosaic.filewatch.annotation.FileWatcher;
import org.mosaic.lifecycle.DP;
import org.mosaic.lifecycle.Module;
import org.mosaic.lifecycle.annotation.BeanRef;
import org.mosaic.lifecycle.annotation.ModuleRef;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.mosaic.security.User;
import org.mosaic.security.policy.PermissionPoliciesManager;
import org.mosaic.security.policy.PermissionPolicy;
import org.mosaic.util.collect.*;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.xml.impl.Digester;
import org.mosaic.util.xml.impl.StrictErrorHandler;
import org.mosaic.web.application.WebApplication;
import org.mosaic.web.application.WebApplicationManager;
import org.mosaic.web.security.AuthenticatorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static org.mosaic.filewatch.WatchEvent.*;
import static org.mosaic.filewatch.WatchRoot.APPS;
import static org.mosaic.web.security.AuthenticatorType.*;

/**
 * @author arik
 */
@Service( WebApplicationManager.class )
public class WebApplicationManagerImpl implements WebApplicationManager
{
    private static final Logger LOG = LoggerFactory.getLogger( WebApplicationManagerImpl.class );

    private static final PermissionPolicy NO_OP_PERMISSION_POLICY = new NoOpPermissionPolicy();

    private static final Schema WEBAPP_SCHEMA;

    static
    {
        SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
        schemaFactory.setErrorHandler( StrictErrorHandler.INSTANCE );
        try
        {
            WEBAPP_SCHEMA = schemaFactory.newSchema( WebApplication.class.getResource( "web-application.xsd" ) );
        }
        catch( SAXException e )
        {
            throw new IllegalStateException( "Could not find 'web-application.xsd' schema resource in Mosaic API bundle: " + e.getMessage(), e );
        }
    }

    private static class NoOpPermissionPolicy implements PermissionPolicy
    {
        @Override
        public boolean isPermitted( @Nonnull User user, @Nonnull String permission )
        {
            return false;
        }
    }

    @Nonnull
    private final Map<String, WebApplicationImpl> applications = new ConcurrentHashMap<>();

    @Nonnull
    private ConversionService conversionService;

    @Nonnull
    private PermissionPoliciesManager permissionPoliciesManager;

    @Nonnull
    private Module module;

    @ModuleRef
    public void setModule( @Nonnull Module module )
    {
        this.module = module;
    }

    @ServiceRef
    public void setConversionService( @Nonnull ConversionService conversionService )
    {
        this.conversionService = conversionService;
    }

    @BeanRef
    public void setPermissionPoliciesManager( @Nonnull PermissionPoliciesManager permissionPoliciesManager )
    {
        this.permissionPoliciesManager = permissionPoliciesManager;
    }

    @Nonnull
    @Override
    public Collection<? extends WebApplication> getApplications()
    {
        return this.applications.values();
    }

    @Nullable
    @Override
    public WebApplication getApplication( @Nonnull String name )
    {
        return this.applications.get( name );
    }

    @FileWatcher( root = APPS, pattern = "*.xml", event = { FILE_ADDED, FILE_MODIFIED } )
    public synchronized void onFileModified( @Nonnull Path file, @Nonnull BasicFileAttributes attrs ) throws IOException
    {
        Digester digester = new Digester( getClass(), WEBAPP_SCHEMA );
        digester.setRuleNamespaceURI( "https://github.com/arikkfir/mosaic/web-application-1.0.0" );

        digester.addSetProperties( "application", "display-name", "displayName" );

        digester.addCallMethod( "application/virtual-hosts/virtual-host", "addVirtualHost" );
        digester.addCallParam( "application/virtual-hosts/virtual-host", 0 );

        digester.addCallMethod( "application/content-languages/language", "addContentLanguage" );
        digester.addCallParam( "application/content-languages/language", 0 );

        digester.addSetProperties( "application/content-languages", "support-uri-selection", "uriLanguageSelectionEnabled" );
        digester.addSetProperties( "application/content-languages", "default-language", "defaultLanguage" );

        digester.addBeanPropertySetter( "application/unknown-url-page", "unknownPageUrl" );
        digester.addBeanPropertySetter( "application/internal-error-page", "internalErrorPage" );
        digester.addBeanPropertySetter( "application/resource-compression", "internalErrorPage" );

        digester.addCallMethod( "application/parameters/parameter", "addParameter" );
        digester.addCallParam( "application/parameters/parameter", 0, "name" );
        digester.addCallParam( "application/parameters/parameter", 1 );

        digester.addCallMethod( "application/security/authentication/realms", "addRealm" );
        digester.addCallParam( "application/security/authentication/realms", 0 );

        digester.addCallMethod( "application/security/authentication/default-authenticators/authenticator", "addDefaultAuthenticator" );
        digester.addCallParam( "application/security/authentication/default-authenticators/authenticator", 0 );

        digester.addCallMethod( "application/security/authentication/service-authenticators/authenticator", "addServiceAuthenticator" );
        digester.addCallParam( "application/security/authentication/service-authenticators/authenticator", 0 );

        digester.addCallMethod( "application/security/authentication/resources-authenticators/authenticator", "addResourceAuthenticator" );
        digester.addCallParam( "application/security/authentication/resources-authenticators/authenticator", 0 );

        digester.addCallMethod( "application/security/authentication/page-authenticators/authenticator", "addPageAuthenticator" );
        digester.addCallParam( "application/security/authentication/page-authenticators/authenticator", 0 );

        digester.addBeanPropertySetter( "application/security/authentication/form-login-url", "formLoginUrl" );

        digester.addBeanPropertySetter( "application/security/permission-policy", "permissionPolicyName" );
        digester.addBeanPropertySetter( "application/security/access-denied-url", "accessDeniedUrl" );

        try
        {
            String fileName = file.getFileName().toString();
            String appName = fileName.substring( 0, fileName.lastIndexOf( "." ) );

            WebApplicationImpl application = this.applications.get( appName );
            if( application == null )
            {
                application = new WebApplicationImpl( this.conversionService, appName );
            }

            digester.push( application );
            digester.parse( file.toFile() );

            this.applications.put( appName, application );
            application.register();
        }
        catch( SAXException e )
        {
            LOG.error( "Error parsing web application at '{}': {}", file, e.getMessage(), e );
        }
    }

    @FileWatcher( root = APPS, pattern = "*.xml", event = FILE_DELETED )
    public synchronized void onFileDeleted( @Nonnull Path file ) throws IOException
    {
        String fileName = file.getFileName().toString();
        String appName = fileName.substring( 0, fileName.lastIndexOf( "." ) );

        WebApplicationImpl application = this.applications.remove( appName );
        if( application != null )
        {
            application.unregister();
        }
    }

    private class WebApplicationImpl extends ConcurrentHashMapEx<String, Object> implements WebApplication
    {
        @Nonnull
        private final String name;

        @Nullable
        private String displayName;

        @Nonnull
        private Set<String> virtualHosts;

        @Nonnull
        private Set<String> contentLanguages;

        private boolean uriLanguageSelectionEnabled;

        @Nonnull
        private String defaultLanguage = "en";

        @Nullable
        private String unknownUrlPage;

        @Nullable
        private String internalErrorPage;

        private boolean resourceCompressionEnabled = true;

        @Nonnull
        private MapEx<String, String> parameters;

        @Nonnull
        private List<String> realms;

        @Nullable
        private String permissionPolicyName;

        @Nonnull
        private List<AuthenticatorType> defaultAuthenticators = Arrays.asList( BASIC_AUTH_HEADER );

        @Nonnull
        private List<AuthenticatorType> serviceAuthenticators = Arrays.asList( SESSION_COOKIE, REMEMBER_ME_COOKIE, BASIC_AUTH_HEADER );

        @Nonnull
        private List<AuthenticatorType> resourcesAuthenticators = Arrays.asList( SESSION_COOKIE, REMEMBER_ME_COOKIE, BASIC_AUTH_HEADER );

        @Nonnull
        private List<AuthenticatorType> pageAuthenticators = Arrays.asList( SESSION_COOKIE, REMEMBER_ME_COOKIE, FORM_PAGE );

        @Nullable
        private String formLoginUrl;

        @Nullable
        private String accessDeniedUrl;

        @Nullable
        private Module.ServiceExport export;

        private WebApplicationImpl( @Nonnull ConversionService conversionService, @Nonnull String name )
        {
            super( conversionService );
            this.name = name;

            Set<String> virtualHosts = new HashSet<>();
            virtualHosts.add( "localhost" );
            this.virtualHosts = unmodifiableSet( virtualHosts );

            Set<String> contentLanguages = new HashSet<>();
            contentLanguages.add( "en" );
            this.contentLanguages = unmodifiableSet( contentLanguages );

            this.parameters = EmptyMapEx.emptyMapEx();

            this.realms = Collections.emptyList();
        }

        public synchronized void register()
        {
            unregister();
            this.export = WebApplicationManagerImpl.this.module.exportService( WebApplication.class, this, DP.dp( "name", this.name ) );
        }

        public synchronized void unregister()
        {
            if( this.export != null )
            {
                this.export.unregister();
                this.export = null;
            }
        }

        @Nonnull
        @Override
        public String getName()
        {
            return this.name;
        }

        @Nonnull
        @Override
        public String getDisplayName()
        {
            if( this.displayName == null )
            {
                return getName();
            }
            else
            {
                return this.displayName;
            }
        }

        @SuppressWarnings( "UnusedDeclaration" )
        public void setDisplayName( @Nullable String displayName )
        {
            this.displayName = displayName;
        }

        @Nonnull
        @Override
        public Set<String> getVirtualHosts()
        {
            return this.virtualHosts;
        }

        @SuppressWarnings( "UnusedDeclaration" )
        public void addVirtualHost( @Nonnull String host )
        {
            Set<String> virtualHosts = new HashSet<>( this.virtualHosts );
            virtualHosts.add( host.toLowerCase() );
            this.virtualHosts = unmodifiableSet( virtualHosts );
        }

        @Override
        public boolean isHostIncluded( @Nonnull String host )
        {
            return getVirtualHosts().contains( host.toLowerCase() );
        }

        @Nonnull
        @Override
        public Collection<String> getContentLanguages()
        {
            return this.contentLanguages;
        }

        @SuppressWarnings( "UnusedDeclaration" )
        public void addContentLanguage( @Nonnull String language )
        {
            Set<String> contentLanguages = new HashSet<>( this.contentLanguages );
            contentLanguages.add( language.toLowerCase() );
            this.contentLanguages = unmodifiableSet( contentLanguages );
        }

        @Override
        public boolean isUriLanguageSelectionEnabled()
        {
            return this.uriLanguageSelectionEnabled;
        }

        @SuppressWarnings( "UnusedDeclaration" )
        public void setUriLanguageSelectionEnabled( boolean uriLanguageSelectionEnabled )
        {
            this.uriLanguageSelectionEnabled = uriLanguageSelectionEnabled;
        }

        @Nonnull
        @Override
        public String getDefaultLanguage()
        {
            return this.defaultLanguage;
        }

        @SuppressWarnings( "UnusedDeclaration" )
        public void setDefaultLanguage( @Nonnull String defaultLanguage )
        {
            this.defaultLanguage = defaultLanguage;
        }

        @Nullable
        @Override
        public String getUnknownUrlPage()
        {
            return this.unknownUrlPage;
        }

        @SuppressWarnings( "UnusedDeclaration" )
        public void setUnknownUrlPage( @Nullable String unknownUrlPage )
        {
            this.unknownUrlPage = unknownUrlPage;
        }

        @Nullable
        @Override
        public String getInternalErrorPage()
        {
            return this.internalErrorPage;
        }

        @SuppressWarnings( "UnusedDeclaration" )
        public void setInternalErrorPage( @Nullable String internalErrorPage )
        {
            this.internalErrorPage = internalErrorPage;
        }

        @Override
        public boolean isResourceCompressionEnabled()
        {
            return this.resourceCompressionEnabled;
        }

        @SuppressWarnings( "UnusedDeclaration" )
        public void setResourceCompressionEnabled( boolean resourceCompressionEnabled )
        {
            this.resourceCompressionEnabled = resourceCompressionEnabled;
        }

        @Nonnull
        @Override
        public MapEx<String, String> getParameters()
        {
            return this.parameters;
        }

        @SuppressWarnings( "UnusedDeclaration" )
        public void addParameter( @Nonnull String name, @Nullable String value )
        {
            MapEx<String, String> parameters = new HashMapEx<>( WebApplicationManagerImpl.this.conversionService );
            parameters.put( name, value );
            this.parameters = new UnmodifiableMapEx<>( parameters );
        }

        @Nonnull
        @Override
        public Collection<String> getRealms()
        {
            return this.realms;
        }

        @SuppressWarnings( "UnusedDeclaration" )
        public void addRealm( @Nonnull String realm )
        {
            List<String> realms = new LinkedList<>( this.realms );
            realms.add( realm );
            this.realms = unmodifiableList( realms );
        }

        @Nullable
        @SuppressWarnings( "UnusedDeclaration" )
        public String getPermissionPolicyName()
        {
            return this.permissionPolicyName;
        }

        @SuppressWarnings( "UnusedDeclaration" )
        public void setPermissionPolicyName( @Nullable String permissionPolicyName )
        {
            this.permissionPolicyName = permissionPolicyName;
        }

        @Nonnull
        @Override
        public PermissionPolicy getPermissionPolicy()
        {
            if( this.permissionPolicyName == null )
            {
                return NO_OP_PERMISSION_POLICY;
            }
            else
            {
                PermissionPolicy permissionPolicy = permissionPoliciesManager.getPolicy( this.permissionPolicyName );
                return permissionPolicy == null ? NO_OP_PERMISSION_POLICY : permissionPolicy;
            }
        }

        @Nonnull
        @Override
        public Collection<AuthenticatorType> getDefaultAuthenticatorTypes()
        {
            return this.defaultAuthenticators;
        }

        @SuppressWarnings( "UnusedDeclaration" )
        public void addDefaultAuthenticator( @Nonnull String authenticator )
        {
            List<AuthenticatorType> authenticators = new LinkedList<>( this.defaultAuthenticators );
            authenticators.add( AuthenticatorType.valueOf( authenticator ) );
            this.defaultAuthenticators = unmodifiableList( authenticators );
        }

        @Nonnull
        @Override
        public Collection<AuthenticatorType> getServiceAuthenticatorTypes()
        {
            return this.serviceAuthenticators;
        }

        @SuppressWarnings( "UnusedDeclaration" )
        public void addServiceAuthenticator( @Nonnull String authenticator )
        {
            List<AuthenticatorType> authenticators = new LinkedList<>( this.serviceAuthenticators );
            authenticators.add( AuthenticatorType.valueOf( authenticator ) );
            this.serviceAuthenticators = unmodifiableList( authenticators );
        }

        @Nonnull
        @Override
        public Collection<AuthenticatorType> getResourceAuthenticatorTypes()
        {
            return this.resourcesAuthenticators;
        }

        @SuppressWarnings( "UnusedDeclaration" )
        public void addResourceAuthenticator( @Nonnull String authenticator )
        {
            List<AuthenticatorType> authenticators = new LinkedList<>( this.resourcesAuthenticators );
            authenticators.add( AuthenticatorType.valueOf( authenticator ) );
            this.resourcesAuthenticators = unmodifiableList( authenticators );
        }

        @Nonnull
        @Override
        public Collection<AuthenticatorType> getPageAuthenticatorTypes()
        {
            return this.pageAuthenticators;
        }

        @SuppressWarnings( "UnusedDeclaration" )
        public void addPageAuthenticator( @Nonnull String authenticator )
        {
            List<AuthenticatorType> authenticators = new LinkedList<>( this.pageAuthenticators );
            authenticators.add( AuthenticatorType.valueOf( authenticator ) );
            this.pageAuthenticators = unmodifiableList( authenticators );
        }

        @Nullable
        @Override
        public String getFormLoginUrl()
        {
            return this.formLoginUrl;
        }

        @SuppressWarnings( "UnusedDeclaration" )
        public void setFormLoginUrl( @Nullable String formLoginUrl )
        {
            this.formLoginUrl = formLoginUrl;
        }

        @Nullable
        @Override
        public String getAccessDeniedUrl()
        {
            return this.accessDeniedUrl;
        }

        @SuppressWarnings( "UnusedDeclaration" )
        public void setAccessDeniedUrl( @Nullable String accessDeniedUrl )
        {
            this.accessDeniedUrl = accessDeniedUrl;
        }
    }
}
