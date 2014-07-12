package org.mosaic.core.weaving.impl;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.mosaic.core.launcher.impl.ServerImpl;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleRevision;

/**
 * @author arik
 */
class BytecodeCachingCompiler implements BytecodeCompiler
{
    @Nonnull
    private final ReadWriteLock lock;

    @Nonnull
    private final Path weavingDirectory;

    @Nonnull
    private final BytecodeCompiler compiler;

    @Nullable
    private Map<BundleRevision, BytecodeBundleRevisionCache> cache;

    @Nullable
    private ScheduledExecutorService executorService;

    BytecodeCachingCompiler( @Nonnull ServerImpl server )
    {
        this.lock = new ReadWriteLock( "Bytecode-Caching-Compiler" );
        this.weavingDirectory = server.getWork().resolve( "weaving" );
        this.compiler = new BytecodeJavassistCompiler();

        server.addStartupHook( bundleContext -> {
            this.cache = new HashMap<>();
            this.executorService = Executors.newSingleThreadScheduledExecutor();
            this.executorService.scheduleWithFixedDelay(
                    () -> {
                        Map<BundleRevision, BytecodeBundleRevisionCache> cache = this.cache;
                        if( cache != null )
                        {
                            for( BytecodeBundleRevisionCache revisionCache : cache.values() )
                            {
                                revisionCache.saveCacheToFile();
                            }
                        }
                    },
                    30, 5, TimeUnit.SECONDS
            );
        } );

        server.addShutdownHook( bundleContext -> {
            ScheduledExecutorService executorService = this.executorService;
            if( executorService != null )
            {
                executorService.shutdown();
                this.executorService = null;
            }

            this.cache = null;
        } );
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this ).toString();
    }

    @Nullable
    @Override
    public byte[] compile( @Nonnull WovenClass wovenClass )
    {
        try
        {
            BundleRevision bundleRevision = wovenClass.getBundleWiring().getRevision();
            BytecodeBundleRevisionCache revisionCache = getBundleRevisionCache( bundleRevision );
            return revisionCache.getBytecode( wovenClass );
        }
        catch( WeavingException e )
        {
            throw e;
        }
        catch( Throwable e )
        {
            throw new WeavingException( "could not weave class '" + wovenClass.getClassName() + "' in " + BytecodeUtil.toString( wovenClass.getBundleWiring().getRevision() ), e );
        }
    }

    @Nonnull
    private BytecodeBundleRevisionCache getBundleRevisionCache( @Nonnull BundleRevision bundleRevision )
    {
        try
        {
            return this.lock.read( () -> {
                Map<BundleRevision, BytecodeBundleRevisionCache> cache = this.cache;
                if( cache == null )
                {
                    throw new IllegalStateException( "bytecode cache not available" );
                }

                BytecodeBundleRevisionCache revisionCache = cache.get( bundleRevision );
                if( revisionCache != null )
                {
                    return revisionCache;
                }

                return this.lock.write( () -> {
                    // check again in case it was just created between our release of the read lock and acquiring of the write lock
                    BytecodeBundleRevisionCache revisionCache2 = cache.get( bundleRevision );
                    if( revisionCache2 != null )
                    {
                        return revisionCache2;
                    }

                    revisionCache2 = new BytecodeBundleRevisionCache( this.compiler, this.weavingDirectory, bundleRevision );
                    cache.put( bundleRevision, revisionCache2 );
                    return revisionCache2;
                } );
            } );
        }
        catch( Throwable e )
        {
            throw new WeavingException( "could not create bytecode cache for bundle revision '" + bundleRevision + "'", e );
        }
    }
}
