package org.mosaic.modules.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleRevision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.*;

/**
 * @author arik
 */
class BytecodeCachingCompiler extends BytecodeCompiler
{
    private static final Logger LOG = LoggerFactory.getLogger( BytecodeCachingCompiler.class );

    @Nonnull
    private final BytecodeCompiler compiler;

    @Nonnull
    private final LoadingCache<BundleRevision, BundleRevisionCache> cache;

    @Nullable
    private ScheduledExecutorService executorService;

    BytecodeCachingCompiler( @Nonnull BytecodeCompiler compiler )
    {
        this.compiler = compiler;
        this.cache = CacheBuilder
                .newBuilder()
                .weakKeys().initialCapacity( 100 )
                .build( new CacheLoader<BundleRevision, BundleRevisionCache>()
                {
                    @Override
                    public BundleRevisionCache load( @Nonnull BundleRevision key ) throws Exception
                    {
                        if( executorService == null )
                        {
                            throw new IllegalStateException( "Weaving hook no longer valid" );
                        }
                        return new BundleRevisionCache( key );
                    }
                } );
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.executorService.scheduleWithFixedDelay( new Runnable()
        {
            @Override
            public void run()
            {
                for( BundleRevisionCache bundleRevisionCache : cache.asMap().values() )
                {
                    bundleRevisionCache.saveCacheToFile();
                }
            }
        }, 1, 5, TimeUnit.SECONDS );
    }

    void stop()
    {
        if( this.executorService != null )
        {
            this.executorService.shutdown();
            this.executorService = null;
        }
    }

    @Nullable
    @Override
    byte[] compile( @Nonnull WovenClass wovenClass )
    {
        if( this.executorService == null )
        {
            throw new IllegalStateException( "Weaving hook no longer valid" );
        }

        try
        {
            BundleRevision bundleRevision = wovenClass.getBundleWiring().getRevision();
            BundleRevisionCache revisionCache = this.cache.get( bundleRevision );
            return revisionCache.getBytecode( wovenClass );
        }
        catch( Exception e )
        {
            LOG.warn( "Could not load cached bytecode for '{}': {}", wovenClass.getClassName(), e.getMessage(), e );
            return null;
        }
    }

    private class BundleRevisionCache
    {
        @Nonnull
        private final Path file;

        @Nonnull
        private final Map<String, byte[]> byteCodes = new HashMap<>();

        @Nonnull
        private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

        private boolean dirty;

        private BundleRevisionCache( @Nonnull BundleRevision bundleRevision ) throws IOException
        {
            Path workDir = Activator.getWorkPath();
            Path weavingDir = workDir.resolve( "weaving" );
            Path bundleDir = weavingDir.resolve( bundleRevision.getBundle().getSymbolicName() );
            Path versionDir = bundleDir.resolve( bundleRevision.getVersion().toString() );
            this.file = versionDir.resolve( bundleRevision.toString() + ".bytes" );
            loadCacheFromFile();
        }

        @Nullable
        private byte[] getBytecode( @Nonnull WovenClass wovenClass ) throws IOException, InterruptedException
        {
            String className = wovenClass.getClassName();

            this.readWriteLock.readLock().lock();
            try
            {
                byte[] bytes = this.byteCodes.get( className );
                if( bytes == null )
                {
                    this.readWriteLock.readLock().unlock();
                    this.readWriteLock.writeLock().lock();
                    try
                    {
                        bytes = compiler.compile( wovenClass );
                        if( bytes == null )
                        {
                            bytes = new byte[ 0 ];
                        }
                        this.byteCodes.put( className, bytes );
                        this.dirty = true;
                    }
                    finally
                    {
                        this.readWriteLock.writeLock().unlock();
                        this.readWriteLock.readLock().lock();
                    }
                }
                return bytes.length == 0 ? null : bytes;
            }
            finally
            {
                this.readWriteLock.readLock().unlock();
            }
        }

        private void loadCacheFromFile()
        {
            this.readWriteLock.writeLock().lock();
            try
            {
                if( exists( this.file ) )
                {
                    boolean corrupt = false;
                    try( DataInputStream in = new DataInputStream( newInputStream( this.file, READ ) ) )
                    {
                        int classCount = in.readInt();
                        for( int i = 0; i < classCount; i++ )
                        {
                            String className = in.readUTF();
                            byte[] bytes = new byte[ in.readInt() ];
                            if( in.read( bytes ) != bytes.length )
                            {
                                corrupt = true;
                                this.byteCodes.clear();
                                break;
                            }
                            this.byteCodes.put( className, bytes );
                        }
                    }
                    catch( IOException e )
                    {
                        LOG.error( "Error reading bytecode cache from '{}': {}", this.file, e.getMessage(), e );
                    }

                    if( corrupt )
                    {
                        LOG.error( "Bytecode cache file at '{}' is corrupt", this.file );
                        try
                        {
                            Files.delete( this.file );
                        }
                        catch( IOException ignore )
                        {
                        }
                    }
                }
                this.dirty = false;
            }
            finally
            {
                this.readWriteLock.writeLock().unlock();
            }
        }

        private void saveCacheToFile()
        {
            if( !this.dirty )
            {
                return;
            }

            this.readWriteLock.writeLock().lock();
            try
            {
                try
                {
                    Files.createDirectories( this.file.getParent() );
                }
                catch( IOException e )
                {
                    LOG.error( "Could not create cache file directory at '{}': {}", this.file.getParent(), e.getMessage(), e );
                    return;
                }

                try( DataOutputStream out = new DataOutputStream( newOutputStream( this.file, CREATE, WRITE, TRUNCATE_EXISTING ) ) )
                {
                    out.writeInt( this.byteCodes.size() );
                    for( Map.Entry<String, byte[]> entry : this.byteCodes.entrySet() )
                    {
                        out.writeUTF( entry.getKey() );

                        byte[] bytes = entry.getValue();
                        out.writeInt( bytes.length );
                        out.write( bytes );
                    }
                    out.flush();
                    this.dirty = false;
                }
                catch( IOException e )
                {
                    LOG.error( "Error persisting bytecode cache to '{}': {}", this.file, e.getMessage(), e );
                }
            }
            finally
            {
                this.readWriteLock.writeLock().unlock();
            }
        }
    }
}
