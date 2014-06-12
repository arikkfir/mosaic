package org.mosaic.core.impl.bytecode;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.mosaic.core.impl.ServerStatus;
import org.mosaic.core.modules.ModuleRevision;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.mosaic.core.util.workflow.Workflow;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleRevision;
import org.slf4j.Logger;

/**
 * @author arik
 */
class BytecodeCachingCompiler implements BytecodeCompiler
{
    @Nonnull
    private final Logger logger;

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

    BytecodeCachingCompiler( @Nonnull Workflow workflow, @Nonnull Path weavingDirectory )
    {
        this.logger = workflow.getLogger();
        this.lock = workflow.getLock();
        this.weavingDirectory = weavingDirectory;
        this.compiler = new BytecodeJavassistCompiler();
        workflow.addAction( ServerStatus.STARTED, c -> initialize(), c -> shutdown() );
        workflow.addAction( ServerStatus.STOPPED, c -> shutdown() );
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this )
                             .add( "compiler", this.compiler )
                             .toString();
    }

    @Nullable
    @Override
    public byte[] compile( @Nonnull ModuleRevision moduleRevision, @Nonnull WovenClass wovenClass )
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
        this.logger.debug( "Initializing bytecode caching compiler" );
        this.cache = new HashMap<>();
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.executorService.scheduleWithFixedDelay( () -> {
            BytecodeCachingCompiler.this.lock.acquireWriteLock();
            try
            {
                Map<BundleRevision, BytecodeBundleRevisionCache> cache1 = BytecodeCachingCompiler.this.cache;
                if( cache1 != null )
                {
                    for( BytecodeBundleRevisionCache revisionCache : cache1.values() )
                    {
                        revisionCache.saveCacheToFile();
                    }
                }
            }
            finally
            {
                BytecodeCachingCompiler.this.lock.releaseWriteLock();
            }
        }, 1, 5, TimeUnit.SECONDS );
    }

    private void shutdown() throws InterruptedException
    {
        this.logger.debug( "Shutting down bytecode caching compiler" );
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
        this.lock.acquireReadLock();
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

            this.lock.releaseReadLock();
            this.lock.acquireWriteLock();
            try
            {
                // check again in case it was just created between our release of the read lock and acquiring of the write lock
                revisionCache = cache.get( bundleRevision );
                if( revisionCache != null )
                {
                    return revisionCache;
                }

                revisionCache = new BytecodeBundleRevisionCache( this.logger,
                                                                 this.lock,
                                                                 this.compiler,
                                                                 this.weavingDirectory,
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
                this.lock.releaseWriteLock();
                this.lock.acquireReadLock();
            }
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }
}
