package org.mosaic.web.application.impl;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.reflect.TypeToken;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.xpath.XPathException;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.expression.Expression;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.web.application.*;

import static java.util.Collections.*;

/**
 * @author arik
 */
public class PageImpl implements Page
{
    private static final Splitter LANGUAGES_SPLITTER = Splitter.on( CharMatcher.anyOf( ":;,/\\ \t\r\n\f" ) )
                                                               .omitEmptyStrings()
                                                               .trimResults( CharMatcher.anyOf( "\"' \t\r\n\f" ) );

    private static final Splitter PATHS_SPLITTER = Splitter.on( CharMatcher.anyOf( ",\t\r\n\f" ) )
                                                           .omitEmptyStrings()
                                                           .trimResults( CharMatcher.anyOf( " \t\r\n\f" ) );

    @Nonnull
    private final WebContent webContent;

    @Nonnull
    private final String name;

    @Nullable
    private final String displayName;

    private final boolean active;

    @Nonnull
    private final Template template;

    @Nullable
    private final Expression security;

    @Nonnull
    private final Map<String, Set<String>> urlPaths;

    @Nonnull
    private final Collection<ContextProviderRef> context;

    @Nonnull
    private final Map<String, Map<String, Block>> blocks;

    public PageImpl( @Nonnull ExpressionParser expressionParser,
                     @Nonnull ConversionService conversionService,
                     @Nonnull WebContent webContent,
                     @Nonnull XmlElement element ) throws XPathException, WebApplicationParseException
    {
        this.webContent = webContent;
        this.name = element.requireAttribute( "name" );
        this.displayName = element.getAttribute( "display-name" );
        this.active = element.requireAttribute( "active", TypeToken.of( Boolean.class ), true );

        String templateName = element.requireAttribute( "template" );
        Template template = this.webContent.getTemplate( templateName );
        if( template == null )
        {
            throw new WebApplicationParseException( "Page '" + this.name + "' uses an unknown template: " + templateName );
        }
        else
        {
            this.template = template;
        }

        String securityExpression = element.find( "c:security", TypeToken.of( String.class ) );
        this.security = securityExpression == null ? null : expressionParser.parseExpression( securityExpression );

        this.urlPaths = unmodifiableMap( parseUrlPaths( element.findElements( "c:url-paths/c:path" ) ) );
        this.context = ContextImpl.getContextProviderRefs( conversionService, template.getContext(), element );

        // start creating a compild blocks collection
        Map<String, Map<String, Block>> blocksByPanels = new LinkedHashMap<>();

        // populate template blocks first
        for( Panel panel : this.template.getPanels() )
        {
            Map<String, Block> panelBloks = new LinkedHashMap<>();
            for( Block block : panel.getBlocks() )
            {
                panelBloks.put( block.getName(), block );
            }
            blocksByPanels.put( panel.getName(), panelBloks );
        }

        // iterate page blocks, overriding corresponding template block if any; adding them otherwise
        for( XmlElement blockElement : element.findElements( "c:blocks/c:block" ) )
        {
            String blockName = blockElement.requireAttribute( "name" );

            String panelName = blockElement.requireAttribute( "panel" );
            Panel panel = this.template.getPanel( panelName );
            if( panel == null )
            {
                throw new WebApplicationParseException( "Block declaration '" + blockName + "' specifies unknown panel( '" + panelName + "' ) in page '" + this.name + "' " );
            }

            Block templateBlock = blocksByPanels.get( panelName ).get( blockName );
            if( templateBlock == null )
            {
                // a new block, does not exist in template, just add it to the panel blocks
                blocksByPanels.get( panelName ).put( blockName, new BlockImpl( conversionService, panel, null, blockElement ) );
            }
            else
            {
                // an extending block
                blocksByPanels.get( panelName ).put( blockName, new BlockImpl( conversionService, panel, templateBlock, blockElement ) );
            }
        }
        this.blocks = blocksByPanels;
    }

    @Nonnull
    @Override
    public WebContent getWebContent()
    {
        return this.webContent;
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
        return this.displayName == null ? getName() : this.displayName;
    }

    @Override
    public boolean isActive()
    {
        return this.active;
    }

    @Nonnull
    @Override
    public Template getTemplate()
    {
        return this.template;
    }

    @Nullable
    @Override
    public Expression getSecurity()
    {
        return this.security;
    }

    @Nonnull
    @Override
    public Set<String> getPaths()
    {
        Set<String> paths = new HashSet<>();
        for( Set<String> pathsForLanguage : this.urlPaths.values() )
        {
            paths.addAll( pathsForLanguage );
        }
        return paths;
    }

    @Nonnull
    @Override
    public Set<String> getPaths( @Nonnull String language )
    {
        Set<String> paths = this.urlPaths.get( language );
        return paths == null ? Collections.<String>emptySet() : paths;
    }

    @Nonnull
    @Override
    public Collection<ContextProviderRef> getContext()
    {
        return this.context;
    }

    @Nonnull
    @Override
    public List<Block> getBlocks( @Nonnull String panelName )
    {
        Map<String, Block> blocks = this.blocks.get( panelName );
        if( blocks == null )
        {
            return emptyList();
        }
        else
        {
            return new LinkedList<>( blocks.values() );
        }
    }

    private Map<String, Set<String>> parseUrlPaths( List<XmlElement> pathElements )
    {
        Map<String, Set<String>> urlPaths = new HashMap<>();
        for( XmlElement element : pathElements )
        {
            String languagesExpression = element.getAttribute( "languages" );
            Iterator<String> languagesIterator =
                    languagesExpression != null
                    ? LANGUAGES_SPLITTER.split( languagesExpression ).iterator()
                    : this.webContent.getApplication().getContentLanguages().iterator();
            while( languagesIterator.hasNext() )
            {
                String language = languagesIterator.next();
                Set<String> languagePaths = urlPaths.containsKey( language )
                                            ? new HashSet<>( urlPaths.get( language ) )
                                            : new HashSet<String>();
                for( String path : PATHS_SPLITTER.split( element.requireValue() ) )
                {
                    languagePaths.add( path );
                }
                urlPaths.put( language, unmodifiableSet( languagePaths ) );
            }
        }
        return urlPaths;
    }
}
