package org.mosaic.web.application.impl;

import com.google.common.io.Resources;
import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathException;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.mosaic.lifecycle.DP;
import org.mosaic.lifecycle.Module;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.BeanRef;
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
import org.mosaic.util.pair.ImmutablePair;
import org.mosaic.util.xml.StrictErrorHandler;
import org.mosaic.util.xml.XmlDocument;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.util.xml.XmlParser;
import org.mosaic.web.application.*;
import org.mosaic.web.handler.impl.util.PathParametersCompiler;
import org.mosaic.web.security.AuthenticatorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import static java.nio.file.Files.exists;
import static java.util.Collections.*;
import static org.mosaic.web.security.AuthenticatorType.*;

/**
 * @author arik
 */
@Bean
public class WebApplicationFactory
{
    private static final Logger LOG = LoggerFactory.getLogger( WebApplicationFactory.class );

    private static final String WEB_APP_SCHEMA_NS = "https://github.com/arikkfir/mosaic/web/application-1.0.0";

    private static final String WEB_CONTENT_SCHEMA_NS = "https://github.com/arikkfir/mosaic/web/content-1.0.0";

    public static final PermissionPolicy NO_OP_PERMISSION_POLICY = new WebApplicationFactory.NoOpPermissionPolicy();

    public static class NoOpPermissionPolicy implements PermissionPolicy
    {
        @Override
        public boolean isPermitted( @Nonnull User user, @Nonnull String permission )
        {
            return false;
        }
    }

    private final Schema webAppSchema;

    private final Schema webContentSchema;

    @Nonnull
    private final PeriodFormatter maxSessionAgePeriodFormatter = new PeriodFormatterBuilder()
            .appendYears()
            .appendSuffix( " year", " years" ).appendSeparator( ", ", " and " )
            .appendMonths()
            .appendSuffix( " month", " months" ).appendSeparator( ", ", " and " )
            .appendDays()
            .appendSuffix( " day", " days" ).appendSeparator( ", ", " and " )
            .appendHours()
            .appendSuffix( " hour", " hours" ).appendSeparator( ", ", " and " )
            .printZeroRarelyLast()
            .appendMinutes()
            .appendSuffix( " minute", " minutes" )
            .toFormatter();

    @Nonnull
    private final PeriodFormatter periodFormatter;

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

    @Nonnull
    private PathParametersCompiler pathParametersCompiler;

    public WebApplicationFactory() throws IOException, SAXException
    {
        this.periodFormatter = new PeriodFormatterBuilder()
                .printZeroRarelyLast()
                .appendYears()
                .appendSuffix( " year", " years" )
                .appendSeparator( ", ", " and " )
                .appendMonths()
                .appendSuffix( " month", " months" )
                .appendDays()
                .appendSuffix( " day", " days" )
                .appendHours()
                .appendSuffix( " hour", " hour" )
                .appendMinutes()
                .appendSuffix( " minute", " minutes" )
                .appendSeconds()
                .appendSuffix( " second", " seconds" )
                .toFormatter();

        SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
        schemaFactory.setErrorHandler( StrictErrorHandler.INSTANCE );

        // load web app schema
        try( InputStream stream100 = Resources.getResource( WebApplication.class, "web-application-1.0.0.xsd" ).openStream() )
        {
            this.webAppSchema = schemaFactory.newSchema( new Source[] {
                    new StreamSource( stream100, "https://github.com/arikkfir/mosaic/web/application-1.0.0" )
            } );
        }

        // load web app schema
        try( InputStream stream100 = Resources.getResource( WebApplication.class, "web-contents-1.0.0.xsd" ).openStream() )
        {
            this.webContentSchema = schemaFactory.newSchema( new Source[] {
                    new StreamSource( stream100, "https://github.com/arikkfir/mosaic/web/content-1.0.0" )
            } );
        }
    }

    @BeanRef
    public void setPathParametersCompiler( @Nonnull PathParametersCompiler pathParametersCompiler )
    {
        this.pathParametersCompiler = pathParametersCompiler;
    }

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

    public class WebApplicationImpl implements WebApplication
    {
        @Nonnull
        private final ConcurrentHashMapEx<String, Object> attributes;

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
        private Period maxSessionAge;

        @Nonnull
        private WebContentImpl webContent;

        @Nullable
        private Module.ServiceExport export;

        @Nonnull
        private DateTime lastModified = DateTime.now();

        private WebApplicationImpl( @Nonnull Path file )
                throws IOException, SAXException, ParserConfigurationException, XPathException
        {
            this.attributes = new ConcurrentHashMapEx<>( 100, WebApplicationFactory.this.conversionService );
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
            XmlDocument document = xmlParser.parse( this.file, webAppSchema );
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

            Period maxSessionAge;
            String maxSessionAgeString = root.find( "a:max-session-age", TypeToken.of( String.class ) );
            if( maxSessionAgeString != null )
            {
                maxSessionAge = maxSessionAgePeriodFormatter.parsePeriod( maxSessionAgeString );
            }
            else
            {
                maxSessionAge = new Period( 30, PeriodType.minutes() );
            }

            // parameters
            MapEx<String, String> parameters = new LinkedHashMapEx<>( 20, this.attributes.getConversionService() );
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

            // web content
            WebContentImpl webContent;
            try
            {
                webContent = new WebContentImpl( root );
            }
            catch( Exception e )
            {
                LOG.warn( "Could not read web contents for app '{}': {}", this.name, e.getMessage(), e );
                webContent = new WebContentImpl();
            }

            // apply
            this.lastModified = DateTime.now();
            this.displayName = displayName;
            this.virtualHosts = virtualHosts;
            this.uriLanguageSelectionEnabled = uriLanguageSelectionEnabled;
            this.defaultLanguage = defaultLanguage;
            this.contentLanguages = contentLanguages;
            this.unknownUrlPageName = unknownUrlPageName;
            this.internalErrorPageName = internalErrorPageName;
            this.resourceCompressionEnabled = resourceCompressionEnabled;
            this.maxSessionAge = maxSessionAge;
            this.parameters = new UnmodifiableMapEx<>( parameters );
            this.realms = realms;
            this.permissionPolicyName = permissionPolicyName;
            this.defaultAuthenticators = defaultAuthenticators;
            this.serviceAuthenticators = serviceAuthenticators;
            this.resourcesAuthenticators = resourcesAuthenticators;
            this.pageAuthenticators = pageAuthenticators;
            this.formLoginUrl = formLoginUrl;
            this.accessDeniedPageName = accessDeniedPageName;
            this.webContent = webContent;
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
        public MapEx<String, Object> getAttributes()
        {
            return this.attributes;
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
            return this.unknownUrlPageName == null ? null : this.webContent.getPage( this.unknownUrlPageName );
        }

        @Nullable
        @Override
        public Page getInternalErrorPage()
        {
            return this.internalErrorPageName == null ? null : this.webContent.getPage( this.internalErrorPageName );
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
        public Period getMaxSessionAge()
        {
            return this.maxSessionAge;
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
            return this.accessDeniedPageName == null ? null : this.webContent.getPage( this.accessDeniedPageName );
        }

        @Nonnull
        @Override
        public WebContent getWebContent()
        {
            return this.webContent;
        }

        @Nonnull
        @Override
        public DateTime getLastModified()
        {
            return this.lastModified;
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

        private class WebContentImpl implements WebContent
        {
            @Nonnull
            private final Set<Path> contentRoots;

            @Nonnull
            private final Map<String, Snippet> snippets;

            @Nonnull
            private final Map<String, Template> templates;

            @Nonnull
            private final Map<String, Page> pages;

            @Nonnull
            private final Collection<ContextProviderRef> context;

            @Nonnull
            private final Map<String, Period> cachePeriods;

            private WebContentImpl()
            {
                this.contentRoots = emptySet();
                this.snippets = emptyMap();
                this.templates = emptyMap();
                this.pages = emptyMap();
                this.context = emptyList();
                this.cachePeriods = emptyMap();
            }

            private WebContentImpl( @Nonnull XmlElement root )
                    throws XPathException, IOException, SAXException, ParserConfigurationException
            {
                // content roots
                Set<Path> contentRoots = new LinkedHashSet<>();
                for( String value : root.findTexts( "a:content-roots/a:content-root" ) )
                {
                    contentRoots.add( Paths.get( value ) );
                }
                this.contentRoots = unmodifiableSet( contentRoots );

                // web content
                Path contentFile = WebApplicationImpl.this.file.resolveSibling( "content" ).resolve( WebApplicationImpl.this.name + "-content.xml" );
                if( exists( contentFile ) )
                {
                    // parse
                    XmlDocument document = xmlParser.parse( contentFile, webContentSchema );
                    document.addNamespace( "c", WEB_CONTENT_SCHEMA_NS );
                    root = document.getRoot();

                    // caching
                    Map<String, Period> cachePeriods = new LinkedHashMap<>();
                    for( XmlElement element : root.findElements( "c:caching/c:pattern" ) )
                    {
                        String pattern = element.getAttribute( "path" );
                        String periodText = element.getAttribute( "period" );
                        Period period;
                        if( "0".equals( periodText ) || "disable".equalsIgnoreCase( periodText ) || "none".equalsIgnoreCase( periodText ) )
                        {
                            period = Period.ZERO;
                        }
                        else
                        {
                            period = WebApplicationFactory.this.periodFormatter.parsePeriod( periodText );
                        }
                        cachePeriods.put( pattern, period );
                    }
                    this.cachePeriods = cachePeriods;

                    // context
                    this.context = ContextImpl.getContextProviderRefs(
                            WebApplicationImpl.this.attributes.getConversionService(), root );

                    // snippets
                    Map<String, Snippet> snippets = new HashMap<>( 500 );
                    for( XmlElement element : root.findElements( "c:snippets/c:snippet" ) )
                    {
                        String name = element.requireAttribute( "name" );
                        String content = element.find( "c:content", TypeToken.of( String.class ) );
                        snippets.put( name, new SnippetImpl( name, content ) );
                    }
                    this.snippets = unmodifiableMap( snippets );

                    // templates
                    Map<String, Template> templates = new HashMap<>( 500 );
                    for( XmlElement element : root.findElements( "c:templates/c:template" ) )
                    {
                        TemplateImpl template = new TemplateImpl( WebApplicationImpl.this.attributes.getConversionService(), this, element );
                        templates.put( template.getName(), template );
                    }
                    this.templates = unmodifiableMap( templates );

                    // pages
                    Map<String, Page> pages = new HashMap<>( 100 );
                    for( XmlElement element : root.findElements( "c:pages/c:page" ) )
                    {
                        PageImpl page = new PageImpl( expressionParser, WebApplicationImpl.this.attributes.getConversionService(), this, element );
                        pages.put( page.getName(), page );
                    }
                    this.pages = pages;
                }
                else
                {
                    this.snippets = emptyMap();
                    this.templates = emptyMap();
                    this.pages = emptyMap();
                    this.context = emptyList();
                    this.cachePeriods = emptyMap();
                }
            }

            @Nonnull
            @Override
            public WebApplication getApplication()
            {
                return WebApplicationImpl.this;
            }

            @Nonnull
            @Override
            public Collection<Path> getContentRoots()
            {
                return this.contentRoots;
            }

            @Nonnull
            @Override
            public Collection<ContextProviderRef> getContext()
            {
                return this.context;
            }

            @Nullable
            @Override
            public Snippet getSnippet( @Nonnull String name )
            {
                return this.snippets.get( name );
            }

            @Nonnull
            @Override
            public Collection<Snippet> getSnippets()
            {
                return this.snippets.values();
            }

            @Nullable
            @Override
            public Template getTemplate( @Nonnull String name )
            {
                return this.templates.get( name );
            }

            @Nonnull
            @Override
            public Collection<Template> getTemplates()
            {
                return this.templates.values();
            }

            @Nullable
            @Override
            public Page getPage( @Nonnull String name )
            {
                return this.pages.get( name );
            }

            @Nonnull
            @Override
            public Collection<Page> getPages()
            {
                return this.pages.values();
            }

            @Nullable
            @Override
            public Period getCachePeriod( @Nonnull String path )
            {
                // TODO arik: cache these results

                for( Map.Entry<String, Period> entry : this.cachePeriods.entrySet() )
                {
                    ImmutablePair<String, String> key = ImmutablePair.of( entry.getKey(), path );
                    MapEx<String, String> match = WebApplicationFactory.this.pathParametersCompiler.load( key );
                    if( match != PathParametersCompiler.NO_MATCH )
                    {
                        return entry.getValue();
                    }
                }
                return null;
            }
        }
    }
}
