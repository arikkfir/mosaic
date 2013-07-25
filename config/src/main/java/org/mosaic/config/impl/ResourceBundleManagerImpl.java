package org.mosaic.config.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import org.mosaic.Server;
import org.mosaic.config.ResourceBundleManager;
import org.mosaic.filewatch.annotation.FileWatcher;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.Files.*;
import static org.mosaic.filewatch.WatchEvent.*;
import static org.mosaic.filewatch.WatchRoot.ETC;

/**
 * @author arik
 */
@Service(ResourceBundleManager.class)
public class ResourceBundleManagerImpl implements ResourceBundleManager
{
    private static final Logger LOG = LoggerFactory.getLogger( ResourceBundleManagerImpl.class );

    @Nonnull
    private final LoadingCache<String, LoadingCache<Locale, MosaicResourceBundle>> resourceBundles;

    @Nonnull
    private Server server;

    @Nonnull
    private Path resourceBundlesRoot;

    public ResourceBundleManagerImpl()
    {
        this.resourceBundles =
                CacheBuilder.newBuilder()
                            .concurrencyLevel( 10 )
                            .initialCapacity( 50 )
                            .build( new CacheLoader<String, LoadingCache<Locale, MosaicResourceBundle>>()
                            {
                                @Override
                                public LoadingCache<Locale, MosaicResourceBundle> load( final String baseName )
                                        throws Exception
                                {
                                    return CacheBuilder.newBuilder()
                                                       .concurrencyLevel( 10 )
                                                       .initialCapacity( 10 )
                                                       .build( new CacheLoader<Locale, MosaicResourceBundle>()
                                                       {
                                                           @Override
                                                           public MosaicResourceBundle load( Locale locale )
                                                                   throws Exception
                                                           {
                                                               return new MosaicResourceBundle( baseName, locale );
                                                           }
                                                       } );
                                }
                            } );
    }

    @ServiceRef
    public void setServer( @Nonnull Server server ) throws MalformedURLException
    {
        this.server = server;
    }

    @PostConstruct
    public void init() throws MalformedURLException
    {
        this.resourceBundlesRoot = this.server.getEtc().resolve( "resource-bundles" );
    }

    @Nonnull
    @Override
    public ResourceBundle getResourceBundle( @Nonnull String name, @Nonnull Locale locale )
    {
        return this.resourceBundles.getUnchecked( name ).getUnchecked( locale );
    }

    @FileWatcher(root = ETC,
                 pattern = "resource-bundles/**/*.properties",
                 event = { FILE_ADDED, FILE_DELETED, FILE_MODIFIED })
    public synchronized void onAddedResourceBundle( @Nonnull Path file )
    {
        this.resourceBundles.invalidateAll();
    }

    private class MosaicResourceBundle extends ResourceBundle
    {
        @Nonnull
        private final String baseName;

        @Nonnull
        private final Locale locale;

        @Nonnull
        private final Properties entries = new Properties();

        @Nonnull
        private final Path resource;

        private MosaicResourceBundle( @Nonnull String baseName, @Nonnull Locale locale )
        {
            this.baseName = baseName;
            this.locale = locale;

            String language = locale.getLanguage(), languageTag = language.isEmpty() ? "" : "_" + language;
            String country = locale.getCountry(), countryTag = country.isEmpty() ? "" : "_" + country;
            String variant = locale.getVariant(), variantTag = variant.isEmpty() ? "" : "_" + variant;
            if( !language.isEmpty() && !country.isEmpty() && !variant.isEmpty() )
            {
                setParent( resourceBundles.getUnchecked( baseName ).getUnchecked( new Locale( language, country ) ) );
            }
            else if( !language.isEmpty() && !country.isEmpty() )
            {
                setParent( resourceBundles.getUnchecked( baseName ).getUnchecked( new Locale( language ) ) );
            }
            else if( !language.isEmpty() )
            {
                setParent( resourceBundles.getUnchecked( baseName ).getUnchecked( Locale.ROOT ) );
            }

            this.resource = resourceBundlesRoot.resolve( baseName + languageTag + countryTag + variantTag + ".properties" );
            if( exists( this.resource ) && isRegularFile( this.resource ) && isReadable( this.resource ) )
            {
                try( Reader reader = new InputStreamReader( newInputStream( this.resource ), "UTF-8" ) )
                {
                    this.entries.load( reader );
                }
                catch( IOException e )
                {
                    LOG.warn( "Could not read resource bundle from '{}': {}", this.resource, e.getMessage(), e );
                }
            }
        }

        @SuppressWarnings("UnusedDeclaration")
        @Nonnull
        public String getBaseName()
        {
            return baseName;
        }

        @Nonnull
        @Override
        public Locale getLocale()
        {
            return locale;
        }

        @SuppressWarnings("UnusedDeclaration")
        @Nonnull
        public Path getResource()
        {
            return resource;
        }

        @Override
        public Enumeration<String> getKeys()
        {
            return new Enumeration<String>()
            {
                private final Iterator<String> privateEntries = entries.stringPropertyNames().iterator();

                private final Enumeration<String> parentEntries = parent != null ? parent.getKeys() : null;

                @Override
                public boolean hasMoreElements()
                {
                    return this.privateEntries.hasNext() || ( parentEntries != null && parentEntries.hasMoreElements() );
                }

                @Override
                public String nextElement()
                {
                    if( this.privateEntries.hasNext() )
                    {
                        return this.privateEntries.next();
                    }
                    else if( this.parentEntries != null )
                    {
                        return this.parentEntries.nextElement();
                    }
                    else
                    {
                        throw new NoSuchElementException();
                    }
                }
            };
        }

        @Override
        protected Object handleGetObject( String key )
        {
            return this.entries.get( key );
        }
    }
}
