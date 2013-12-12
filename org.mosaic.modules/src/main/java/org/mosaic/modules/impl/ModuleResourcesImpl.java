package org.mosaic.modules.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Module;
import org.mosaic.modules.ModuleResources;
import org.mosaic.modules.ModuleState;
import org.mosaic.util.resource.AntPathMatcher;
import org.mosaic.util.resource.PathMatcher;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

/**
 * @author arik
 */
final class ModuleResourcesImpl extends Lifecycle implements ModuleResources
{
    @Nonnull
    private final PathMatcher pathMatcher = new AntPathMatcher();

    @Nonnull
    private final ModuleImpl module;

    @Nonnull
    private final LoadingCache<String, Collection<URL>> resourcesLookupCache;

    ModuleResourcesImpl( @Nonnull ModuleImpl module )
    {
        this.module = module;
        this.resourcesLookupCache = CacheBuilder
                .newBuilder()
                .concurrencyLevel( 5 )
                .initialCapacity( 100 )
                .maximumSize( 1000 )
                .build( new CacheLoader<String, Collection<URL>>()
                {
                    @Nonnull
                    @Override
                    public Collection<URL> load( @Nonnull String key ) throws Exception
                    {
                        if( !key.startsWith( "/" ) )
                        {
                            key = "/" + key;
                        }

                        Enumeration<URL> entries = requireResolvedBundle().findEntries( "/", null, true );
                        if( entries == null )
                        {
                            return Collections.emptyList();
                        }

                        List<URL> matches = null;
                        while( entries.hasMoreElements() )
                        {
                            URL url = entries.nextElement();
                            if( ModuleResourcesImpl.this.pathMatcher.matches( key, url.getPath() ) )
                            {
                                if( matches == null )
                                {
                                    matches = new LinkedList<>();
                                }
                                matches.add( url );
                            }
                        }
                        return matches == null ? Collections.<URL>emptyList() : Collections.unmodifiableList( matches );
                    }
                } );
    }

    @Override
    public String toString()
    {
        return "ModuleResources[" + this.module + "]";
    }

    @Nonnull
    @Override
    public Module getModule()
    {
        return this.module;
    }

    @Nullable
    @Override
    public URL getResource( @Nonnull String name )
    {
        return requireResolvedBundle().getResource( name );
    }

    @Nonnull
    @Override
    public Collection<URL> getResources( @Nonnull String name )
    {
        try
        {
            Enumeration<URL> resources = requireResolvedBundle().getResources( name );
            return resources == null ? Collections.<URL>emptyList() : Collections.list( resources );
        }
        catch( IOException e )
        {
            throw new IllegalStateException( "cannot read from module " + this.module + ": " + e.getMessage(), e );
        }
    }

    @Nonnull
    @Override
    public Collection<URL> findResources( @Nonnull String glob )
    {
        return this.resourcesLookupCache.getUnchecked( glob );
    }

    @Nonnull
    @Override
    public ClassLoader getClassLoader()
    {
        BundleWiring bundleWiring = requireResolvedBundle().adapt( BundleWiring.class );
        if( bundleWiring == null )
        {
            throw new IllegalStateException( "bundle for module " + this.module + " has no bundle wiring" );
        }

        ClassLoader classLoader = bundleWiring.getClassLoader();
        if( classLoader == null )
        {
            throw new IllegalStateException( "bundle for module " + this.module + " has no class loader" );
        }

        return classLoader;
    }

    @Nonnull
    @Override
    public Class<?> loadClass( @Nonnull String className ) throws ClassNotFoundException
    {
        if( this.module.getState() == ModuleState.ACTIVE )
        {
            return requireResolvedBundle().loadClass( className );
        }
        else
        {
            throw new IllegalStateException( "module " + this.module + " is not active" );
        }
    }

    protected synchronized void clearCache()
    {
        this.resourcesLookupCache.invalidateAll();
    }

    @Nonnull
    private Bundle requireResolvedBundle()
    {
        Bundle bundle = ModuleResourcesImpl.this.module.getBundle();
        if( bundle.getState() == Bundle.INSTALLED )
        {
            throw new IllegalStateException( "cannot search for resources in module " + ModuleResourcesImpl.this.module + " while it is in the INSTALLED state" );
        }
        else if( bundle.getState() == Bundle.UNINSTALLED )
        {
            throw new IllegalStateException( "cannot search for resources in module " + ModuleResourcesImpl.this.module + " while it is in the UNINSTALLED state" );
        }
        else
        {
            return bundle;
        }
    }
}
