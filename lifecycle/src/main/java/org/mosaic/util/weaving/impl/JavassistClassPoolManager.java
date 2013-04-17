package org.mosaic.util.weaving.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
public class JavassistClassPoolManager
{
    @Nonnull
    private static final List<String> FORBIDDEN_BUNDLES = asList(
            "org.mosaic.api",
            "org.mosaic.lifecycle"
    );

    @Nonnull
    private final LoadingCache<BundleWiring, ClassPool> classPoolsCache;

    public JavassistClassPoolManager()
    {
        this.classPoolsCache = CacheBuilder.newBuilder()
                                           .concurrencyLevel( 3 )
                                           .expireAfterAccess( 1, TimeUnit.HOURS )
                                           .initialCapacity( 100 )
                                           .weakKeys()
                                           .build( new CacheLoader<BundleWiring, ClassPool>()
                                           {
                                               @Override
                                               public ClassPool load( BundleWiring key ) throws Exception
                                               {
                                                   ClassPool classPool = new ClassPool( false );
                                                   classPool.appendClassPath( new LoaderClassPath( getClass().getClassLoader() ) );
                                                   classPool.appendClassPath( new LoaderClassPath( key.getClassLoader() ) );
                                                   return classPool;
                                               }
                                           } );
    }

    @Nullable
    public synchronized CtClass findCtClassFor( @Nonnull WovenClass wovenClass )
    {
        BundleWiring bundleWiring = wovenClass.getBundleWiring();

        // don't weave the system bundle or a no-weaving bundle
        if( !shouldWeaveBundle( bundleWiring ) )
        {
            return null;
        }
        else
        {
            try
            {
                return this.classPoolsCache.get( bundleWiring ).get( wovenClass.getClassName() );
            }
            catch( NotFoundException e )
            {
                throw new WeavingException( "Could not find weaving target class '" + wovenClass.getClassName() + "': " + e.getMessage(), e );
            }
            catch( ExecutionException e )
            {
                throw new WeavingException( "Could not create class pool for target class '" + wovenClass.getClassName() + "': " + e.getMessage(), e );
            }
        }
    }

    @Nullable
    public synchronized CtClass findCtClassFor( @Nonnull WovenClass wovenClass, @Nonnull String className )
    {
        BundleWiring bundleWiring = wovenClass.getBundleWiring();

        // don't weave the system bundle or a no-weaving bundle
        if( !shouldWeaveBundle( bundleWiring ) )
        {
            return null;
        }
        else
        {
            try
            {
                return this.classPoolsCache.get( bundleWiring ).get( className );
            }
            catch( NotFoundException e )
            {
                throw new WeavingException( "Could not find weaving target class '" + wovenClass.getClassName() + "': " + e.getMessage(), e );
            }
            catch( ExecutionException e )
            {
                throw new WeavingException( "Could not create class pool for target class '" + wovenClass.getClassName() + "': " + e.getMessage(), e );
            }
        }
    }

    private boolean shouldWeaveBundle( BundleWiring bundleWiring )
    {
        return bundleWiring.getBundle().getBundleId() != 0 && !FORBIDDEN_BUNDLES.contains( bundleWiring.getBundle().getSymbolicName() );
    }
}
