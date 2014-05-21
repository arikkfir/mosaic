package org.mosaic.core.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.workflow.Status;
import org.mosaic.core.util.workflow.TransitionAdapter;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleRevision;

/**
 * @author arik
 */
class BytecodeCachingCompiler extends TransitionAdapter implements BytecodeCompiler
{
    @Nonnull
    private final ServerImpl server;

    @Nonnull
    private final BytecodeCompiler compiler;

    @Nullable
    private Map<BundleRevision, BytecodeBundleRevisionCache> cache;

    @Nullable
    private ScheduledExecutorService executorService;

    BytecodeCachingCompiler( @Nonnull ServerImpl server, @Nonnull BytecodeCompiler compiler )
    {
        this.server = server;
        this.compiler = compiler;
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this )
                             .add( "compiler", this.compiler )
                             .toString();
    }

    @Override
    public void execute( @Nonnull Status origin, @Nonnull Status target ) throws Exception
    {
        if( target == ServerImpl.STARTED )
        {
            initialize();
        }
        else if( target == ServerImpl.STOPPED )
        {
            shutdown();
        }
    }

    @Override
    public void revert( @Nonnull Status origin, @Nonnull Status target ) throws Exception
    {
        if( target == ServerImpl.STARTED )
        {
            shutdown();
        }
    }

    @Nullable
    @Override
    public byte[] compile( @Nonnull ModuleRevisionImpl moduleRevision, @Nonnull WovenClass wovenClass )
    {
        try
        {
            BundleRevision bundleRevision = wovenClass.getBundleWiring().getRevision();
            BytecodeBundleRevisionCache revisionCache = getBundleRevisionCache( bundleRevision );
            return revisionCache.getBytecode( moduleRevision, wovenClass );
        }
        catch( WeavingException e )
        {
            throw e;
        }
        catch( Throwable e )
        {
            throw new WeavingException( "could not weave class '" + wovenClass.getClassName() + "' in " + moduleRevision, e );
        }
    }

    private void initialize()
    {
        this.server.getLogger().debug( "Initializing bytecode caching compiler" );
        this.cache = new HashMap<>();
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.executorService.scheduleWithFixedDelay( new Runnable()
        {
            @Override
            public void run()
            {
                BytecodeCachingCompiler.this.server.acquireWriteLock();
                try
                {
                    Map<BundleRevision, BytecodeBundleRevisionCache> cache = BytecodeCachingCompiler.this.cache;
                    if( cache != null )
                    {
                        for( BytecodeBundleRevisionCache revisionCache : cache.values() )
                        {
                            revisionCache.saveCacheToFile();
                        }
                    }
                }
                finally
                {
                    BytecodeCachingCompiler.this.server.releaseWriteLock();
                }
            }
        }, 1, 5, TimeUnit.SECONDS );
    }

    private void shutdown() throws InterruptedException
    {
        this.server.getLogger().debug( "Shutting down bytecode caching compiler" );
        ScheduledExecutorService executorService = this.executorService;
        if( executorService != null )
        {
            executorService.shutdown();
            executorService.awaitTermination( 5, TimeUnit.SECONDS );
            this.executorService = null;
        }

        this.cache = null;
    }

    @Nonnull
    private BytecodeBundleRevisionCache getBundleRevisionCache( @Nonnull BundleRevision bundleRevision )
    {
        this.server.acquireReadLock();
        try
        {
            Map<BundleRevision, BytecodeBundleRevisionCache> cache = this.cache;
            if( cache == null )
            {
                throw new WeavingException( "caching bytecode compiler is no longer available (is the server started?)" );
            }

            BytecodeBundleRevisionCache revisionCache = cache.get( bundleRevision );
            if( revisionCache != null )
            {
                return revisionCache;
            }

            this.server.releaseReadLock();
            this.server.acquireWriteLock();
            try
            {
                // check again in case it was just created between our release of the read lock and acquiring of the write lock
                revisionCache = cache.get( bundleRevision );
                if( revisionCache != null )
                {
                    return revisionCache;
                }

                revisionCache = new BytecodeBundleRevisionCache( this.server,
                                                                 this.compiler,
                                                                 this.server.getWork().resolve( "weaving" ),
                                                                 bundleRevision );
                cache.put( bundleRevision, revisionCache );
                return revisionCache;
            }
            catch( Throwable e )
            {
                throw new WeavingException( "could not create bytecode cache for bundle revision '" + bundleRevision + "'", e );
            }
            finally
            {
                this.server.releaseWriteLock();
                this.server.acquireReadLock();
            }
        }
        finally
        {
            this.server.releaseReadLock();
        }
    }
}
