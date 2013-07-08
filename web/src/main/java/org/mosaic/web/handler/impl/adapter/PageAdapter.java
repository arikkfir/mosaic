package org.mosaic.web.handler.impl.adapter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import org.joda.time.Period;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.lifecycle.annotation.*;
import org.mosaic.util.collect.HashMapEx;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.web.application.*;
import org.mosaic.web.handler.annotation.Context;
import org.mosaic.web.handler.impl.RequestExecutionPlan;
import org.mosaic.web.handler.impl.action.Handler;
import org.mosaic.web.handler.impl.filter.FindPageFilter;
import org.mosaic.web.handler.impl.filter.HttpMethodFilter;
import org.mosaic.web.net.MediaType;
import org.mosaic.web.request.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.Arguments;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateProcessingParameters;
import org.thymeleaf.context.VariablesMap;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dom.Element;
import org.thymeleaf.dom.Node;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.messageresolver.AbstractMessageResolver;
import org.thymeleaf.messageresolver.MessageResolution;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.processor.ProcessorResult;
import org.thymeleaf.processor.attr.AbstractAttrProcessor;
import org.thymeleaf.resourceresolver.IResourceResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

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

    @MethodEndpointBind( Context.class )
    public void addContextProvider( @Nonnull MethodEndpoint endpoint, @ServiceId long id, @Rank int rank )
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        this.contextProviderAdapters.add( new ContextProviderAdapter( this.conversionService, this.expressionParser, id, rank, endpoint ) );
        LOG.info( "Added @Context provider {}", endpoint );
    }

    @MethodEndpointUnbind( Context.class )
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

            ClassLoader oldTccl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader( getClass().getClassLoader() );
            try
            {
                setRank( PAGE_REQUEST_ADAPTER_RANK );
                setParticipator( new RenderPageHandler() );
                addFilter( new HttpMethodFilter( GET ) );
                addFilter( new FindPageFilter() );
            }
            finally
            {
                Thread.currentThread().setContextClassLoader( oldTccl );
            }
        }
    }

    public class RenderPageHandler implements Handler
    {
        @Nonnull
        private final MosaicTemplateEngine templateEngine = new MosaicTemplateEngine();

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
            org.mosaic.web.application.Page page = handlerContext.require( "page", Page.class );

            // create thymeleaf context
            Locale locale = request.getHeaders().getAcceptLanguage().get( 0 );
            org.thymeleaf.context.Context thymeleafContext = new org.thymeleaf.context.Context( locale );
            thymeleafContext.setVariable( WebRequest.class.getName(), request );
            thymeleafContext.setVariable( "request", request );
            thymeleafContext.setVariable( Page.class.getName(), page );
            thymeleafContext.setVariable( "page", page );

            // build page context
            thymeleafContext.setVariables( handlerContext );
            applyContextProviders( request, thymeleafContext, handlerContext, page.getApplication().getContext() );
            applyContextProviders( request, thymeleafContext, handlerContext, page.getTemplate().getContext() );
            applyContextProviders( request, thymeleafContext, handlerContext, page.getContext() );

            // process thymeleaf template into response stream
            // -> thymeleaf uses TCCL to locate some classes so we have to fool it
            ClassLoader oldTccl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader( getClass().getClassLoader() );
            try
            {
                request.getResponse().getHeaders().setContentType( MediaType.TEXT_HTML );
                String snippetName = page.getTemplate().getSnippet().getName();
                Writer responseWriter = request.getResponse().getCharacterBody();
                this.templateEngine.process( snippetName, thymeleafContext, responseWriter );
            }
            finally
            {
                Thread.currentThread().setContextClassLoader( oldTccl );
            }
            return null;
        }

        private void applyContextProviders( @Nonnull WebRequest request,
                                            org.thymeleaf.context.Context thymeleafContext,
                                            @Nonnull MapEx<String, Object> handlerContext,
                                            @Nonnull Collection<ContextProviderRef> contextProviderRefs )
                throws Exception
        {
            for( ContextProviderRef contextProviderRef : contextProviderRefs )
            {
                String name = contextProviderRef.getName();
                String type = contextProviderRef.getType();

                for( ContextProviderAdapter adapter : contextProviderAdapters )
                {
                    if( adapter.getName().equals( type ) && adapter.matches( request ) )
                    {
                        Object value = adapter.provide( handlerContext, contextProviderRef.getParameters() );
                        thymeleafContext.setVariable( name, value );
                        break;
                    }
                }
            }
        }

        private class PanelElementProcessor extends AbstractAttrProcessor
        {
            private PanelElementProcessor()
            {
                super( "panel" );
            }

            @Override
            public int getPrecedence()
            {
                return 0;
            }

            @Override
            protected ProcessorResult processAttribute( Arguments arguments, Element element, String attributeName )
            {
                VariablesMap<String, Object> variables = arguments.getContext().getVariables();

                // find application
                WebRequest request = ( WebRequest ) variables.get( WebRequest.class.getName() );
                if( request == null )
                {
                    throw new TemplateProcessingException( "Cannot process '" + attributeName + "' attribute - cannot find web request in context" );
                }

                // find page
                Page page = ( Page ) variables.get( Page.class.getName() );
                if( page == null )
                {
                    throw new TemplateProcessingException( "Cannot process '" + attributeName + "' attribute - cannot find page in context" );
                }

                // find panel
                String panelName = element.getAttributeValue( attributeName );
                Panel panel = page.getTemplate().getPanel( panelName );
                if( panel == null )
                {
                    throw new TemplateProcessingException( "Cannot process '" + attributeName + "' attribute - cannot find panel '" + panelName + "' in page '" + page.getName() + "'" );
                }

                // invoke context providers for this panel
                MapEx<String, Object> handlerContext = new HashMapEx<>( variables, conversionService );
                try
                {
                    applyContext( element, request, handlerContext, panel.getContext() );
                }
                catch( Exception e )
                {
                    throw new TemplateProcessingException( "Cannot process '" + attributeName + "' - cannot apply context for panel '" + panelName + "' in page '" + page.getName() + "': " + e.getMessage(), e );
                }

                // mark the element as a panel
                if( element.hasAttribute( "class" ) )
                {
                    element.setAttribute( "class", element.getAttributeValue( "class" ) + " panel panel-" + panelName );
                }
                else
                {
                    element.setAttribute( "class", "panel panel-" + panelName );
                }

                // add child block elements
                element.clearChildren();
                element.removeAttribute( attributeName );
                for( Block block : panel.getBlocks() )
                {
                    addBlock( arguments, element, request, page, block, handlerContext );
                }
                for( Block block : page.getBlocks( panelName ) )
                {
                    addBlock( arguments, element, request, page, block, handlerContext );
                }
                return ProcessorResult.OK;
            }

            private void addBlock( @Nonnull Arguments arguments,
                                   @Nonnull Element panelElement,
                                   @Nonnull WebRequest request,
                                   @Nonnull Page page,
                                   @Nonnull Block block,
                                   @Nonnull MapEx<String, Object> handlerContext )
            {
                Element blockElement = new Element( "div" );
                blockElement.setAttribute( "class", "block block-" + block.getName() );
                try
                {
                    applyContext( blockElement, request, handlerContext, block.getContext() );
                }
                catch( Exception e )
                {
                    throw new TemplateProcessingException( "Cannot apply context for block '" + block.getName() + "' in page '" + page.getName() + "': " + e.getMessage(), e );
                }

                List<Node> fragmentNodes = arguments.getTemplateRepository().getFragment( arguments, block.getSnippet().getContent() );
                blockElement.setChildren( fragmentNodes );

                panelElement.addChild( blockElement );
            }

            private void applyContext( @Nonnull Element element,
                                       @Nonnull WebRequest request,
                                       @Nonnull MapEx<String, Object> handlerContext,
                                       @Nonnull Collection<ContextProviderRef> contextProviderRefs ) throws Exception
            {
                for( ContextProviderRef contextProviderRef : contextProviderRefs )
                {
                    String name = contextProviderRef.getName();
                    String type = contextProviderRef.getType();

                    for( ContextProviderAdapter adapter : contextProviderAdapters )
                    {
                        if( adapter.getName().equals( type ) && adapter.matches( request ) )
                        {
                            Object value = adapter.provide( handlerContext, contextProviderRef.getParameters() );
                            element.setNodeLocalVariable( name, value );
                            break;
                        }
                    }
                }
            }
        }

        private class MosaicDialect extends AbstractDialect
        {
            @Override
            public String getPrefix()
            {
                return "mc";
            }

            @Override
            public boolean isLenient()
            {
                return false;
            }

            @Override
            public Set<IProcessor> getProcessors()
            {
                Set<IProcessor> processors = new LinkedHashSet<>();
                processors.add( new PanelElementProcessor() );
                return processors;
            }
        }

        private class MosaicSnippetsResourceResolver implements IResourceResolver
        {
            @Override
            public String getName()
            {
                return "mosaicSnippetsResourceResolver";
            }

            @Override
            public InputStream getResourceAsStream( @Nonnull TemplateProcessingParameters processingParameters,
                                                    @Nonnull String resourceName )
            {
                VariablesMap<String, Object> variables = processingParameters.getContext().getVariables();
                Page page = ( Page ) variables.get( Page.class.getName() );
                if( page == null )
                {
                    return null;
                }

                Snippet snippet = page.getApplication().getSnippet( resourceName );
                if( snippet == null )
                {
                    return null;
                }

                try
                {
                    byte[] bytes = snippet.getContent().getBytes( "UTF-8" );
                    return new ByteArrayInputStream( bytes );
                }
                catch( UnsupportedEncodingException e )
                {
                    throw new IllegalStateException();
                }
            }
        }

        private class MosaicMessageResolver extends AbstractMessageResolver
        {
            @Override
            public MessageResolution resolveMessage( Arguments arguments, String key, Object[] messageParameters )
            {
                return new MessageResolution( "arik" );
            }
        }

        private class MosaicTemplateResolver extends TemplateResolver
        {
            private MosaicTemplateResolver()
            {
                setCharacterEncoding( "UTF-8" );
                setTemplateMode( "HTML5" );
                setCacheable( true );
                setCacheTTLMs( ( long ) Period.hours( 1 ).getMillis() );
                setResourceResolver( new MosaicSnippetsResourceResolver() );
            }
        }

        private class MosaicTemplateEngine extends TemplateEngine
        {
            private MosaicTemplateEngine()
            {
                setTemplateResolver( new MosaicTemplateResolver() );
                addDialect( new MosaicDialect() );
                setMessageResolver( new MosaicMessageResolver() );
            }
        }
    }
}
