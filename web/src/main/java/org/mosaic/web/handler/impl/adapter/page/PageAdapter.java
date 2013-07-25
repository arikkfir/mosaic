package org.mosaic.web.handler.impl.adapter.page;

import freemarker.cache.TemplateLoader;
import freemarker.core.Environment;
import freemarker.template.*;
import freemarker.template.Template;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.lifecycle.annotation.*;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.web.application.*;
import org.mosaic.web.handler.annotation.Context;
import org.mosaic.web.handler.impl.RequestExecutionPlan;
import org.mosaic.web.handler.impl.action.Handler;
import org.mosaic.web.handler.impl.adapter.ContextProviderAdapter;
import org.mosaic.web.handler.impl.adapter.RequestAdapter;
import org.mosaic.web.handler.impl.filter.FindPageFilter;
import org.mosaic.web.handler.impl.filter.HttpMethodFilter;
import org.mosaic.web.net.MediaType;
import org.mosaic.web.request.WebRequest;
import org.mosaic.web.request.WebResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static freemarker.template.Configuration.ANGLE_BRACKET_TAG_SYNTAX;
import static freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER;
import static org.mosaic.web.net.HttpMethod.GET;

/**
 * @author arik
 */
@Bean
public class PageAdapter
{
    private static final Logger LOG = LoggerFactory.getLogger( PageAdapter.class );

    private static final int PAGE_REQUEST_ADAPTER_ID = -24552;

    private static final int PAGE_REQUEST_ADAPTER_RANK = Integer.MIN_VALUE + 1000;

    @Nonnull
    private final Collection<ContextProviderAdapter> contextProviderAdapters = new ConcurrentSkipListSet<>();

    @Nonnull
    private ConversionService conversionService;

    @Nonnull
    private ExpressionParser expressionParser;

    @Nonnull
    private PageRequestAdapter requestAdapter;

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

    @Nonnull
    public PageRequestAdapter getRequestAdapter()
    {
        return this.requestAdapter;
    }

    @MethodEndpointBind(Context.class)
    public void addContextProvider( @Nonnull MethodEndpoint endpoint, @ServiceId long id, @Rank int rank )
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        this.contextProviderAdapters.add( new ContextProviderAdapter( this.conversionService, this.expressionParser, id, rank, endpoint ) );
        LOG.debug( "Added @Context provider {}", endpoint );
    }

    @MethodEndpointUnbind(Context.class)
    public void removeContextProvider( @Nonnull MethodEndpoint endpoint, @ServiceId long id )
    {
        for( Iterator<? extends ContextProviderAdapter> iterator = this.contextProviderAdapters.iterator(); iterator.hasNext(); )
        {
            ContextProviderAdapter handler = iterator.next();
            if( handler.getId() == id )
            {
                iterator.remove();
                return;
            }
        }
    }

    @PostConstruct
    public void init()
    {
        this.requestAdapter = new PageRequestAdapter();
    }

    public class PageRequestAdapter extends RequestAdapter
    {
        private PageRequestAdapter()
        {
            super( PageAdapter.this.conversionService, PAGE_REQUEST_ADAPTER_ID );
            setRank( PAGE_REQUEST_ADAPTER_RANK );
            setParticipator( new RenderPageHandler() );
            addFilter( new HttpMethodFilter( GET ) );
            addFilter( new FindPageFilter() );
        }
    }

    public class RenderPageHandler implements Handler
    {
        @Nonnull
        private final Configuration configuration;

        @Nonnull
        private final ThreadLocal<HierarchicalMap<String, Object>> handlerContextHolder = new ThreadLocal<>();

        @Nonnull
        private final PanelDirectiveModel panelDirectiveModel = new PanelDirectiveModel();

        public RenderPageHandler()
        {
            Configuration configuration = new Configuration();
            configuration.setClassicCompatible( false );
            configuration.setDefaultEncoding( "UTF-8" );
            configuration.setIncompatibleImprovements( new Version( "2.3.20" ) );
            configuration.setLocale( Locale.US );
            configuration.setLocalizedLookup( false );
            configuration.setOutputEncoding( "UTF-8" );
            configuration.setTagSyntax( ANGLE_BRACKET_TAG_SYNTAX );
            configuration.setTemplateExceptionHandler( RETHROW_HANDLER );
            configuration.setTemplateLoader( new PageTemplateLoader() );
            configuration.setTemplateUpdateDelay( 5 );
            this.configuration = configuration;
        }

        @Override
        public void apply( @Nonnull RequestExecutionPlan plan, @Nonnull MapEx<String, Object> context )
        {
            plan.addHandler( this, context );
        }

        @Nullable
        @Override
        public Object handle( @Nonnull WebRequest request, @Nonnull MapEx<String, Object> handlerContext )
                throws Exception
        {
            HierarchicalMap<String, Object> context = new HierarchicalMap<>( conversionService, handlerContext );
            this.handlerContextHolder.set( context );
            try
            {
                Page page = context.require( "page", Page.class );

                // create web application context
                applyContextProviders( "application", page.getWebContent().getContext() );

                // create page context
                applyContextProviders( "page", page.getContext() );
                context.put( "renderPanel", this.panelDirectiveModel );

                // obtain the FreeMarker template object
                Template freemarkerPageTemplate =
                        this.configuration.getTemplate( page.getTemplate().getSnippet().getName(),
                                                        request.getHeaders().getAcceptLanguage().get( 0 ),
                                                        "UTF-8",
                                                        true );

                // render the template into a buffer
                StringWriter bufferWriter = new StringWriter( 1024 * 8 );
                freemarkerPageTemplate.process( context, bufferWriter );

                // parse the resulting HTML so we can prettify it
                Document document = Jsoup.parse( bufferWriter.toString() );
                document.outputSettings()
                        .charset( "UTF-8" )
                        .indentAmount( 2 )
                        .prettyPrint( true );

                // output html to the response stream
                WebResponse response = request.getResponse();
                response.getHeaders().setContentType( MediaType.TEXT_HTML );
                response.getCharacterBody().write( document.outerHtml() );

                // since we output directly to the client, no need to return anything
                return null;
            }
            finally
            {
                this.handlerContextHolder.remove();
            }
        }

        private void applyContextProviders( @Nonnull String contextName,
                                            @Nonnull Collection<ContextProviderRef> contextProviderRefs )
                throws TemplateModelException
        {
            HierarchicalMap<String, Object> context = getContext().push( contextName );
            WebRequest request = getRequest();

            for( ContextProviderRef contextProviderRef : contextProviderRefs )
            {
                String name = contextProviderRef.getName();
                String type = contextProviderRef.getType();
                MapEx<String, String> parameters = contextProviderRef.getParameters();

                for( ContextProviderAdapter adapter : contextProviderAdapters )
                {
                    if( adapter.getName().equals( type ) && adapter.matches( request ) )
                    {
                        try
                        {
                            context.put( name, adapter.provide( context, parameters ) );
                        }
                        catch( Exception e )
                        {
                            throw new TemplateModelException( "Cannot provide context '" + adapter.getName() + "': " + e.getMessage(), e );
                        }
                        break;
                    }
                }
            }
        }

        @Nonnull
        private HierarchicalMap<String, Object> getContext()
        {
            HierarchicalMap<String, Object> context = this.handlerContextHolder.get();
            if( context != null )
            {
                return context;
            }
            else
            {
                throw new IllegalStateException( "Context is not available" );
            }
        }

        @Nonnull
        private WebRequest getRequest()
        {
            return getContext().require( "request", WebRequest.class );
        }

        @Nonnull
        private Page getPage()
        {
            return getContext().require( "page", Page.class );
        }

        private class PageTemplateLoader implements TemplateLoader
        {
            @Override
            public Object findTemplateSource( String name ) throws IOException
            {
                return getPage().getWebContent().getSnippet( name );
            }

            @Override
            public long getLastModified( Object templateSource )
            {
                return getPage().getWebContent().getApplication().getLastModified().getMillis();
            }

            @Override
            public Reader getReader( Object templateSource, String encoding ) throws IOException
            {
                Snippet snippet = ( Snippet ) templateSource;
                return new StringReader( snippet.getContent() );
            }

            @Override
            public void closeTemplateSource( Object templateSource ) throws IOException
            {
                // no-op
            }
        }

        private class PanelDirectiveModel implements TemplateDirectiveModel
        {
            @Override
            public void execute( Environment env,
                                 Map params,
                                 TemplateModel[] loopVars,
                                 TemplateDirectiveBody body )
                    throws TemplateException, IOException
            {
                Page page = getPage();

                // find panel name
                String panelName = Objects.toString( params.get( "name" ), null );
                if( panelName == null )
                {
                    throw new TemplateException( "'name' parameter must be passed to panel directive", env );
                }

                // find panel
                Panel panel = page.getTemplate().getPanel( panelName );
                if( panel == null )
                {
                    throw new TemplateException( "could not find panel '" + panelName + "' in template '" + page.getTemplate().getName() + "' of page '" + page.getName() + "'", env );
                }

                // create the panel context
                applyContextProviders( panel.getName(), panel.getContext() );
                try
                {
                    getContext().put( "panel", panel );

                    // render panel
                    env.getOut().append( "<div id=\"" ).append( panel.getName() ).append( "\" class=\"mosaic-panel\">" );
                    for( Block block : page.getBlocks( panelName ) )
                    {
                        applyContextProviders( block.getName(), block.getContext() );
                        try
                        {
                            env.getOut().append( "<div id=\"" ).append( block.getName() ).append( "\" class=\"mosaic-block\">" );
                            Template freemarkerBlockTemplate =
                                    configuration.getTemplate( block.getSnippet().getName(),
                                                               env.getLocale(),
                                                               "UTF-8",
                                                               true );
                            freemarkerBlockTemplate.process( getContext(), env.getOut() );
                            env.getOut().append( "</div>" );
                        }
                        finally
                        {
                            getContext().pop();
                        }
                    }
                    env.getOut().append( "</div>" );
                }
                finally
                {
                    getContext().pop();
                }
            }
        }
    }
}
