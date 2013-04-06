package org.mosaic.util.weaving.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javassist.CtClass;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.impl.util.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import static java.lang.String.format;

/**
 * @author arik
 * @todo framework still starts even after weaving hooks fail
 */
public abstract class BaseWeavingHook implements WeavingHook, InitializingBean, DisposableBean
{
    @Nonnull
    private final ThreadLocal<Map<String, String>> dynamicImportsHolder = new ThreadLocal<Map<String, String>>()
    {
        @Override
        protected Map<String, String> initialValue()
        {
            return new HashMap<>();
        }
    };

    @Nonnull
    private final BundleContext bundleContext;

    @Nonnull
    private final JavassistClassPoolManager classPoolManager;

    @Nullable
    private ServiceRegistration<WeavingHook> serviceRegistration;

    public BaseWeavingHook( @Nonnull BundleContext bundleContext, @Nonnull JavassistClassPoolManager classPoolManager )
    {
        this.bundleContext = bundleContext;
        this.classPoolManager = classPoolManager;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        this.serviceRegistration = ServiceUtils.register( this.bundleContext, WeavingHook.class, this );
    }

    @Override
    public void destroy() throws Exception
    {
        this.serviceRegistration = ServiceUtils.unregister( this.serviceRegistration );
    }

    @Override
    public final void weave( @Nonnull WovenClass wovenClass )
    {
        CtClass ctClass = this.classPoolManager.findCtClassFor( wovenClass );
        if( ctClass == null )
        {
            // this bundle is prohibited from being weaved
            return;
        }
        else if( ctClass.isInterface() || ctClass.isAnnotation() || ctClass.isEnum() )
        {
            // no point weaving interfaces, annotations or enums
            return;
        }

        Map<String, String> dynamicImports = this.dynamicImportsHolder.get();
        dynamicImports.clear();
        try
        {
            weave( wovenClass, ctClass, dynamicImports );
            if( ctClass.isModified() )
            {
                addRequiredPackageImports( wovenClass, dynamicImports );
                wovenClass.setBytes( ctClass.toBytecode() );
            }
        }
        catch( Exception e )
        {
            throw new IllegalStateException( "Could not weave target class '" + wovenClass.getClassName() + "': " + e.getMessage(), e );
        }
    }

    protected final CtClass findCtClass( @Nonnull WovenClass wovenClass, @Nonnull Class<?> type )
    {
        return this.classPoolManager.findCtClassFor( wovenClass, type.getName() );
    }

    protected final CtClass findCtClass( @Nonnull WovenClass wovenClass, @Nonnull String type )
    {
        return this.classPoolManager.findCtClassFor( wovenClass, type );
    }

    protected abstract void weave( @Nonnull WovenClass wovenClass,
                                   @Nonnull CtClass ctClass,
                                   @Nonnull Map<String, String> dynamicImports ) throws Exception;

    private void addRequiredPackageImports( @Nonnull WovenClass wovenClass,
                                            @Nonnull Map<String, String> imports )
    {
        for( String anImport : wovenClass.getDynamicImports() )
        {
            Iterator<String> iterator = imports.keySet().iterator();
            while( iterator.hasNext() )
            {
                String packageName = iterator.next();
                if( anImport.startsWith( packageName + ";" ) || anImport.equals( packageName ) )
                {
                    iterator.remove();
                }
            }
        }

        for( Map.Entry<String, String> entry : imports.entrySet() )
        {
            wovenClass.getDynamicImports().add( format( "%s;version=\"%s\"", entry.getKey(), entry.getValue() ) );
        }
    }
}
