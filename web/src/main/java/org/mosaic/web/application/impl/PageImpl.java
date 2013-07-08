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

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

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
    private final WebApplicationFactory.WebApplicationImpl application;

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
    private final ContextImpl context;

    @Nonnull
    private final List<Block> blocks;

    public PageImpl( @Nonnull ExpressionParser expressionParser,
                     @Nonnull ConversionService conversionService,
                     @Nonnull WebApplicationFactory.WebApplicationImpl application,
                     @Nonnull XmlElement element ) throws XPathException, WebApplicationParseException
    {
        this.application = application;
        this.name = element.requireAttribute( "name" );
        this.displayName = element.getAttribute( "display-name" );
        this.active = element.requireAttribute( "active", TypeToken.of( Boolean.class ), true );

        String templateName = element.getAttribute( "template" );
        if( templateName == null )
        {
            throw new WebApplicationParseException( "Page '" + this.name + "' has no template declaration" );
        }
        Template template = this.application.getTemplate( templateName );
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

        XmlElement contextElement = element.getFirstChildElement( "context" );
        if( contextElement != null )
        {
            this.context = new ContextImpl( conversionService, contextElement );
        }
        else
        {
            this.context = new ContextImpl();
        }

        List<Block> blocks = new LinkedList<>();
        for( XmlElement blockElement : element.findElements( "c:blocks/c:block" ) )
        {
            String panelName = blockElement.getAttribute( "panel" );
            if( panelName == null )
            {
                throw new WebApplicationParseException( "Found block with no panel declaration in page '" + this.name + "'" );
            }

            Panel panel = this.template.getPanel( panelName );
            if( panel == null )
            {
                throw new WebApplicationParseException( "Found block with unknown panel declaration in page '" + this.name + "'" );
            }

            blocks.add( new BlockImpl( conversionService, panel, blockElement ) );
        }
        this.blocks = blocks;

        // validate no duplicate block names
        Set<String> blockNames = new HashSet<>();
        for( Panel panel : this.template.getPanels() )
        {
            for( Block block : panel.getBlocks() )
            {
                blockNames.add( block.getName() );
            }
        }
        for( Block block : this.blocks )
        {
            if( blockNames.contains( block.getName() ) )
            {
                throw new WebApplicationParseException( "Page '" + this.name + "' contains duplicate block names ('" + block.getName() + "')" );
            }
            blockNames.add( block.getName() );
        }
    }

    @Nonnull
    @Override
    public WebApplication getApplication()
    {
        return this.application;
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
        return this.context.getContextProviderRefs();
    }

    @Nonnull
    @Override
    public List<Block> getBlocks()
    {
        return this.blocks;
    }

    @Nonnull
    @Override
    public List<Block> getBlocks( @Nonnull String panelName )
    {
        List<Block> blocks = null;
        for( Block block : this.blocks )
        {
            if( block.getPanel().getName().equals( panelName ) )
            {
                if( blocks == null )
                {
                    blocks = new LinkedList<>();
                }
                blocks.add( block );
            }
        }
        return blocks == null ? Collections.<Block>emptyList() : blocks;
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
                    : getApplication().getContentLanguages().iterator();
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
