package org.mosaic.web.application.impl;

import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Properties;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.Period;
import org.mosaic.web.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.Files.*;

/**
 * @author arik
 */
final class ResourceImpl implements Application.ApplicationResources.Resource
{
    private static final Logger LOG = LoggerFactory.getLogger( ResourceImpl.class );

    @Nonnull
    private final Path path;

    private final boolean compressionEnabled;

    private final boolean browsingEnabled;

    @Nullable
    private final Period cachePeriod;

    ResourceImpl( @Nonnull Path contentRoot, @Nonnull Path path )
    {
        this.path = path;

        Boolean compressionEnabled = null;
        Boolean browsingEnabled = null;
        Period cachePeriod = null;
        Path p = isRegularFile( this.path ) ? this.path.getParent() : this.path;
        while( p.startsWith( contentRoot ) )
        {
            Path propertiesFile = p.resolve( "mosaic.properties" );
            if( exists( propertiesFile ) )
            {
                try( Reader reader = newBufferedReader( propertiesFile, Charset.forName( "UTF-8" ) ) )
                {
                    Properties properties = new Properties();
                    properties.load( reader );

                    if( compressionEnabled == null )
                    {
                        String compressionEnabledValue = properties.getProperty( "compressionEnabled" );
                        if( compressionEnabledValue != null )
                        {
                            compressionEnabled = Boolean.parseBoolean( compressionEnabledValue );
                        }
                    }

                    if( browsingEnabled == null )
                    {
                        String browsingEnabledValue = properties.getProperty( "browsingEnabled" );
                        if( browsingEnabledValue != null )
                        {
                            browsingEnabled = Boolean.parseBoolean( browsingEnabledValue );
                        }
                    }

                    if( cachePeriod == null )
                    {
                        String cachePeriodValue = properties.getProperty( "cachePeriod" );
                        if( cachePeriodValue != null )
                        {
                            cachePeriod = ApplicationImpl.parsePeriod( cachePeriodValue );
                        }
                    }
                }
                catch( Throwable e )
                {
                    LOG.warn( "Error reading Mosaic properties from '{}': {}", propertiesFile, e.getMessage(), e );
                }
            }

            if( compressionEnabled != null && cachePeriod != null )
            {
                break;
            }
            else
            {
                p = p.getParent();
            }
        }

        this.compressionEnabled = compressionEnabled != null && compressionEnabled;
        this.browsingEnabled = browsingEnabled != null && browsingEnabled;
        this.cachePeriod = cachePeriod;
    }

    @Nonnull
    @Override
    public Path getPath()
    {
        return this.path;
    }

    @Override
    public boolean isCompressionEnabled()
    {
        return this.compressionEnabled;
    }

    @Override
    public boolean isBrowsingEnabled()
    {
        return this.browsingEnabled;
    }

    @Nullable
    @Override
    public Period getCachePeriod()
    {
        return this.cachePeriod;
    }
}
