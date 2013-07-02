package org.mosaic.web.application.impl;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.reflect.TypeToken;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.xpath.XPathException;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.expression.Expression;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.web.application.Block;
import org.mosaic.web.application.ContextProviderRef;
import org.mosaic.web.application.Page;
import org.mosaic.web.application.WebApplication;
import org.mosaic.web.net.MediaType;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

/**
 * @author arik
 */
public class PageImpl implements Page
{
    private static final Pattern CACHE_PATTERN = Pattern.compile( "(\\d+) (seconds|minutes|hours|days)" );

    private static final Splitter LANGUAGES_SPLITTER = Splitter.on( CharMatcher.anyOf( ":;,/\\ \t\r\n\f" ) )
                                                               .omitEmptyStrings()
                                                               .trimResults( CharMatcher.anyOf( "\"' \t\r\n\f" ) );

    @Nonnull
    private final WebApplication application;

    private final boolean abstractPage;

    @Nullable
    private final String parentPageName;

    @Nonnull
    private final String name;

    @Nullable
    private final String displayName;

    @Nonnull
    private final Set<String> tags;

    @Nullable
    private final Expression security;

    @Nullable
    private final Expression filter;

    private final boolean active;

    @Nonnull
    private final MediaType mediaType;

    private final long secondsToCache;

    @Nonnull
    private final Map<String, Set<String>> urlPaths;

    @Nonnull
    private final ContextImpl context;

    @Nonnull
    private final Map<String, Block> blocks;

    public PageImpl( @Nonnull ExpressionParser expressionParser,
                     @Nonnull ConversionService conversionService,
                     @Nonnull WebApplication application,
                     @Nonnull XmlElement element ) throws XPathException
    {
        this.application = application;
        this.abstractPage = element.requireAttribute( "abstract", TypeToken.of( Boolean.class ), false );
        this.parentPageName = element.getAttribute( "parent" );
        this.name = element.requireAttribute( "name" );
        this.displayName = element.getAttribute( "display-name" );
        this.active = element.requireAttribute( "active", TypeToken.of( Boolean.class ), true );
        this.mediaType = element.find( "c:media-type", TypeToken.of( MediaType.class ), MediaType.TEXT_HTML );
        this.secondsToCache = parseSecondsToCache( element.find( "c:seconds-to-cache", TypeToken.of( String.class ), "0" ) );

        Set<String> tags = new LinkedHashSet<>();
        for( String tag : element.findTexts( "c:tags/c:tag" ) )
        {
            tags.add( tag.toLowerCase() );
        }
        this.tags = unmodifiableSet( tags );

        String securityExpression = element.find( "c:security", TypeToken.of( String.class ) );
        this.security = securityExpression == null ? null : expressionParser.parseExpression( securityExpression );

        String filterExpression = element.find( "c:filter", TypeToken.of( String.class ) );
        this.filter = filterExpression == null ? null : expressionParser.parseExpression( filterExpression );

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

        Map<String, Block> blocks = new HashMap<>();
        for( XmlElement blockElement : element.findElements( "c:blocks/c:block" ) )
        {
            Block block = new BlockImpl( conversionService, this, blockElement );
            blocks.put( block.getName(), block );
        }
        this.blocks = blocks;
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

    @Override
    public boolean isAbstract()
    {
        return this.abstractPage;
    }

    @Override
    public boolean isActive()
    {
        return this.active;
    }

    @Nullable
    @Override
    public Page getParent()
    {
        return this.parentPageName == null ? null : getApplication().getPageMap().get( this.parentPageName );
    }

    @Nonnull
    @Override
    public String getDisplayName()
    {
        return this.displayName == null ? getName() : this.displayName;
    }

    @Nonnull
    @Override
    public Set<String> getTags()
    {
        return this.tags;
    }

    @Nullable
    @Override
    public Expression getSecurity()
    {
        return this.security;
    }

    @Nullable
    @Override
    public Expression getFilter()
    {
        return this.filter;
    }

    @Nonnull
    @Override
    public MediaType getMediaType()
    {
        return this.mediaType;
    }

    @Override
    public long getSecondsToCache()
    {
        return this.secondsToCache;
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
    public Map<String, Block> getBlocks()
    {
        return this.blocks;
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
                languagePaths.add( element.requireValue().trim() );
                urlPaths.put( language, unmodifiableSet( languagePaths ) );
            }
        }
        return urlPaths;
    }

    private long parseSecondsToCache( @Nonnull String secondsToCacheString )
    {
        try
        {
            return Long.parseLong( secondsToCacheString );
        }
        catch( NumberFormatException e )
        {
            Matcher matcher = CACHE_PATTERN.matcher( secondsToCacheString );
            if( matcher.matches() )
            {
                switch( matcher.group( 2 ) )
                {
                    case "seconds":
                        return Long.parseLong( matcher.group( 1 ) );
                    case "minutes":
                        return TimeUnit.MINUTES.toSeconds( Long.parseLong( matcher.group( 1 ) ) );
                    case "hours":
                        return TimeUnit.HOURS.toSeconds( Long.parseLong( matcher.group( 1 ) ) );
                    case "days":
                        return TimeUnit.DAYS.toSeconds( Long.parseLong( matcher.group( 1 ) ) );
                }
            }
        }
        throw new IllegalArgumentException( "Illegal cache expression: " + secondsToCacheString );
    }
}
