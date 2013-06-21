package org.mosaic.web.application.impl;

import com.google.common.io.Resources;
import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathException;
import org.mosaic.lifecycle.DP;
import org.mosaic.lifecycle.Module;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.ModuleRef;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.mosaic.security.User;
import org.mosaic.security.policy.PermissionPoliciesManager;
import org.mosaic.security.policy.PermissionPolicy;
import org.mosaic.util.collect.ConcurrentHashMapEx;
import org.mosaic.util.collect.LinkedHashMapEx;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.collect.UnmodifiableMapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.util.xml.StrictErrorHandler;
import org.mosaic.util.xml.XmlDocument;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.util.xml.XmlParser;
import org.mosaic.web.application.Page;
import org.mosaic.web.application.Snippet;
import org.mosaic.web.application.WebApplication;
import org.mosaic.web.application.WebApplicationParseException;
import org.mosaic.web.security.AuthenticatorType;
import org.xml.sax.SAXException;

import static java.nio.file.Files.exists;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableSet;
import static org.mosaic.web.security.AuthenticatorType.*;

/**
 * @author arik
 */
@Bean
public class WebApplicationFactory
{
    private static final String WEB_APP_SCHEMA_NS = "https://github.com/arikkfir/mosaic/web/application";

    private static final Schema WEB_APP_SCHEMA;

    private static final String WEB_CONTENT_NS = "https://github.com/arikkfir/mosaic/web/content";

    private static final Schema WEB_CONTENT_SCHEMA;

    public static final PermissionPolicy NO_OP_PERMISSION_POLICY = new WebApplicationFactory.NoOpPermissionPolicy();

    public static class NoOpPermissionPolicy implements PermissionPolicy
    {
        @Override
        public boolean isPermitted( @Nonnull User user, @Nonnull String permission )
        {
            return false;
        }
    }

    static
    {
        SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
        schemaFactory.setErrorHandler( StrictErrorHandler.INSTANCE );
        try
        {
            URL webAppSchemaResource = Resources.getResource( WebApplication.class, "web-application.xsd" );
            if( webAppSchemaResource == null )
            {
                throw new IllegalStateException( "Could not find 'web-application.xsd' in class-path!" );
            }
            WEB_APP_SCHEMA = schemaFactory.newSchema( webAppSchemaResource );

            URL webContextSchemaResource = Resources.getResource( WebApplication.class, "web-contents.xsd" );
            if( webContextSchemaResource == null )
            {
                throw new IllegalStateException( "Could not find 'web-content.xsd' in class-path!" );
            }
            WEB_CONTENT_SCHEMA = schemaFactory.newSchema( webContextSchemaResource );
        }
        catch( SAXException e )
        {
            throw new IllegalStateException( "Could not find 'web-application.xsd' schema resource in Mosaic API bundle: " + e.getMessage(), e );
        }
    }

    @Nonnull
    private Module module;

    @Nonnull
    private ConversionService conversionService;

    @Nonnull
    private ExpressionParser expressionParser;

    @Nonnull
    private PermissionPoliciesManager permissionPoliciesManager;

    @Nonnull
    private XmlParser xmlParser;

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

    @ServiceRef
    public void setExpressionParser( @Nonnull ExpressionParser expressionParser )
    {
        this.expressionParser = expressionParser;
    }

    @ServiceRef
    public void setPermissionPoliciesManager( @Nonnull PermissionPoliciesManager permissionPoliciesManager )
    {
        this.permissionPoliciesManager = permissionPoliciesManager;
    }

    @ServiceRef
    public void setXmlParser( @Nonnull XmlParser xmlParser )
    {
        this.xmlParser = xmlParser;
    }

    @Nonnull
    public WebApplicationImpl parseWebApplication( @Nonnull Path file ) throws WebApplicationParseException
    {
        try
        {
            return new WebApplicationImpl( file );
        }
        catch( Exception e )
        {
            throw new WebApplicationParseException( "Could not read web application from '" + file + "': " + e.getMessage(), e );
        }
    }

    @Nonnull
    public String getApplicationName( @Nonnull Path file )
    {
        String fileName = file.getFileName().toString();
        return fileName.substring( 0, fileName.lastIndexOf( "." ) );
    }

    public class WebApplicationImpl extends ConcurrentHashMapEx<String, Object> implements WebApplication
    {
        @Nonnull
        private final String name;

        @Nonnull
        private final Path file;

        @Nullable
        private String displayName;

        @Nonnull
        private Set<String> virtualHosts;

        private boolean uriLanguageSelectionEnabled;

        @Nonnull
        private String defaultLanguage;

        @Nonnull
        private Set<String> contentLanguages;

        @Nullable
        private String unknownUrlPageName;

        @Nullable
        private String internalErrorPageName;

        private boolean resourceCompressionEnabled;

        @Nonnull
        private MapEx<String, String> parameters;

        @Nonnull
        private Set<String> realms;

        @Nullable
        private String permissionPolicyName;

        @Nonnull
        private Set<AuthenticatorType> defaultAuthenticators;

        @Nonnull
        private Set<AuthenticatorType> serviceAuthenticators;

        @Nonnull
        private Set<AuthenticatorType> resourcesAuthenticators;

        @Nonnull
        private Set<AuthenticatorType> pageAuthenticators;

        @Nullable
        private String formLoginUrl;

        @Nullable
        private String accessDeniedPageName;

        @Nonnull
        private Set<Path> contentRoots;

        @Nonnull
        private Map<String, Snippet> snippets = emptyMap();

        @Nonnull
        private Map<String, Page> pages = emptyMap();

        @Nullable
        private Module.ServiceExport export;

        private WebApplicationImpl( @Nonnull Path file )
                throws IOException, SAXException, ParserConfigurationException, XPathException
        {
            super( WebApplicationFactory.this.conversionService );
            this.file = file;
            this.name = getApplicationName( file );
            refresh();
        }

        public void refresh() throws
                              IOException,
                              SAXException,
                              ParserConfigurationException,
                              XPathException
        {
            XmlDocument document = xmlParser.parse( this.file, WEB_APP_SCHEMA );
            document.addNamespace( "a", WEB_APP_SCHEMA_NS );
            XmlElement root = document.getRoot();

            String displayName = root.requireAttribute( "display-name" );
            Set<String> virtualHosts = parseTexts( root.findTexts( "a:virtual-hosts/a:virtual-host" ), "localhost" );
            Boolean uriLanguageSelectionEnabled = root.find( "a:content-languages/@support-uri-selection", TypeToken.of( Boolean.class ), false );
            String defaultLanguage = root.find( "a:content-languages/@default-language", TypeToken.of( String.class ), "en" );
            Set<String> contentLanguages = parseTexts( root.findTexts( "a:content-languages/a:language" ), "en" );
            String unknownUrlPageName = root.find( "a:unknown-url-page", TypeToken.of( String.class ) );
            String internalErrorPageName = root.find( "a:internal-error-page", TypeToken.of( String.class ) );
            Boolean resourceCompressionEnabled = root.find( "a:resource-compression", TypeToken.of( Boolean.class ), true );

            // parameters
            MapEx<String, String> parameters = new LinkedHashMapEx<>( 20, conversionService );
            for( XmlElement element : root.findElements( "a:parameters/a:parameter" ) )
            {
                parameters.put( element.requireAttribute( "name" ), element.getValue() );
            }

            Set<String> realms = parseTexts( root.findTexts( "a:security/a:authentication/a:realms/a:realm" ) );
            String permissionPolicyName = root.find( "a:security/a:permission-policy", TypeToken.of( String.class ) );
            Set<AuthenticatorType> defaultAuthenticators = parseAuthenticators( root.findTexts( "a:security/a:authentication/a:default-authenticators/a:authenticator" ), BASIC_AUTH_HEADER );
            Set<AuthenticatorType> serviceAuthenticators = parseAuthenticators( root.findTexts( "a:security/a:authentication/a:service-authenticators/a:authenticator" ), SESSION_COOKIE, REMEMBER_ME_COOKIE, BASIC_AUTH_HEADER );
            Set<AuthenticatorType> resourcesAuthenticators = parseAuthenticators( root.findTexts( "a:security/a:authentication/a:resources-authenticators/a:authenticator" ), SESSION_COOKIE, REMEMBER_ME_COOKIE, BASIC_AUTH_HEADER );
            Set<AuthenticatorType> pageAuthenticators = parseAuthenticators( root.findTexts( "a:security/a:authentication/a:page-authenticators/a:authenticator" ), SESSION_COOKIE, REMEMBER_ME_COOKIE, FORM_PAGE );
            String formLoginUrl = root.find( "a:security/a:authentication/a:form-login-url/@url", TypeToken.of( String.class ) );
            String accessDeniedPageName = root.find( "a:security/a:access-denied-page", TypeToken.of( String.class ) );

            // content roots
            Set<Path> contentRoots = new LinkedHashSet<>();
            for( String value : root.findTexts( "a:content-roots/a:content-root" ) )
            {
                contentRoots.add( Paths.get( value ) );
            }

            // web content
            Map<String, Snippet> snippets;
            Map<String, Page> pages;
            Path contentFile = this.file.resolveSibling( "content" ).resolve( this.name + "-content.xml" );
            if( exists( contentFile ) )
            {
                document = xmlParser.parse( contentFile, WEB_CONTENT_SCHEMA );
                document.addNamespace( "c", WEB_CONTENT_NS );
                root = document.getRoot();

                // snippets
                snippets = new HashMap<>( 500 );
                for( XmlElement element : root.findElements( "c:snippets/c:snippet" ) )
                {
                    String id = element.requireAttribute( "id" );
                    String content = element.find( "c:content", TypeToken.of( String.class ) );
                    snippets.put( id, new SnippetImpl( id, content ) );
                }

                // pages
                pages = new HashMap<>( 100 );
                for( XmlElement element : root.findElements( "c:pages/c:page" ) )
                {
                    PageImpl page = new PageImpl( expressionParser, conversionService, this, element );
                    pages.put( page.getName(), page );
                }
            }
            else
            {
                snippets = emptyMap();
                pages = emptyMap();
            }

            this.displayName = displayName;
            this.virtualHosts = virtualHosts;
            this.uriLanguageSelectionEnabled = uriLanguageSelectionEnabled;
            this.defaultLanguage = defaultLanguage;
            this.contentLanguages = contentLanguages;
            this.unknownUrlPageName = unknownUrlPageName;
            this.internalErrorPageName = internalErrorPageName;
            this.resourceCompressionEnabled = resourceCompressionEnabled;
            this.parameters = new UnmodifiableMapEx<>( parameters );
            this.realms = realms;
            this.permissionPolicyName = permissionPolicyName;
            this.defaultAuthenticators = defaultAuthenticators;
            this.serviceAuthenticators = serviceAuthenticators;
            this.resourcesAuthenticators = resourcesAuthenticators;
            this.pageAuthenticators = pageAuthenticators;
            this.formLoginUrl = formLoginUrl;
            this.accessDeniedPageName = accessDeniedPageName;
            this.contentRoots = unmodifiableSet( contentRoots );
            this.snippets = snippets;
            this.pages = pages;
            if( this.export != null )
            {
                this.export.update();
            }
        }

        public synchronized void register()
        {
            unregister();
            this.export = WebApplicationFactory.this.module.exportService( WebApplication.class, this, DP.dp( "name", getName() ) );
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

        @Nonnull
        @Override
        public Set<String> getVirtualHosts()
        {
            return this.virtualHosts;
        }

        @Nonnull
        @Override
        public Collection<String> getContentLanguages()
        {
            return this.contentLanguages;
        }

        @Override
        public boolean isUriLanguageSelectionEnabled()
        {
            return this.uriLanguageSelectionEnabled;
        }

        @Nonnull
        @Override
        public String getDefaultLanguage()
        {
            return this.defaultLanguage;
        }

        @Nullable
        @Override
        public Page getUnknownUrlPage()
        {
            return this.unknownUrlPageName == null ? null : this.pages.get( this.unknownUrlPageName );
        }

        @Nullable
        @Override
        public Page getInternalErrorPage()
        {
            return this.internalErrorPageName == null ? null : this.pages.get( this.internalErrorPageName );
        }

        @Override
        public boolean isResourceCompressionEnabled()
        {
            return this.resourceCompressionEnabled;
        }

        @Nonnull
        @Override
        public MapEx<String, String> getParameters()
        {
            return this.parameters;
        }

        @Nonnull
        @Override
        public Collection<String> getRealms()
        {
            return this.realms;
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

        @Nonnull
        @Override
        public Collection<AuthenticatorType> getServiceAuthenticatorTypes()
        {
            return this.serviceAuthenticators;
        }

        @Nonnull
        @Override
        public Collection<AuthenticatorType> getResourceAuthenticatorTypes()
        {
            return this.resourcesAuthenticators;
        }

        @Nonnull
        @Override
        public Collection<AuthenticatorType> getPageAuthenticatorTypes()
        {
            return this.pageAuthenticators;
        }

        @Nullable
        @Override
        public String getFormLoginUrl()
        {
            return this.formLoginUrl;
        }

        @Nullable
        @Override
        public Page getAccessDeniedPage()
        {
            return this.accessDeniedPageName == null ? null : this.pages.get( this.accessDeniedPageName );
        }

        @Nonnull
        @Override
        public Collection<Path> getContentRoots()
        {
            return this.contentRoots;
        }

        @Nonnull
        @Override
        public Map<String, Snippet> getSnippetMap()
        {
            return this.snippets;
        }

        @Nonnull
        @Override
        public Collection<Snippet> getSnippets()
        {
            return this.snippets.values();
        }

        @Nonnull
        @Override
        public Map<String, Page> getPageMap()
        {
            return this.pages;
        }

        @Nonnull
        @Override
        public Collection<Page> getPages()
        {
            return this.pages.values();
        }

        private Set<String> parseTexts( @Nonnull List<String> texts,
                                        @Nonnull String... defaultTexts )
        {
            Set<String> values = new LinkedHashSet<>( 5 );
            for( String value : texts )
            {
                values.add( value.toLowerCase() );
            }
            if( values.isEmpty() )
            {
                Collections.addAll( values, defaultTexts );
            }
            return unmodifiableSet( values );
        }

        private Set<AuthenticatorType> parseAuthenticators( @Nonnull List<String> texts,
                                                            @Nonnull AuthenticatorType... defaultAuthenticatorTypes )
        {
            Set<AuthenticatorType> authenticatorTypes = new LinkedHashSet<>();
            for( String value : texts )
            {
                authenticatorTypes.add( AuthenticatorType.valueOf( value.toUpperCase() ) );
            }
            if( authenticatorTypes.isEmpty() )
            {
                Collections.addAll( authenticatorTypes, defaultAuthenticatorTypes );
            }
            return unmodifiableSet( authenticatorTypes );
        }
    }
}
