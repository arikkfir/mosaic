package org.mosaic.util.weaving.impl;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javassist.*;
import javassist.bytecode.SyntheticAttribute;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.reflection.impl.AnnotationUtils;
import org.mosaic.util.weaving.EnableWeaving;
import org.mosaic.util.weaving.MethodInterceptor;
import org.mosaic.util.weaving.MethodInvocation;
import org.mosaic.util.weaving.spi.WeaverSpi;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.beans.factory.DisposableBean;

import static java.lang.String.format;
import static java.util.Arrays.asList;

/**
 * @author arik
 */
public class InterceptionWeavingHook implements WeavingHook, DisposableBean
{
    @Nonnull
    private static final List<String> FORBIDDEN_BUNDLES = asList(
            "api",
            "lifecycle"
    );

    @Nonnull
    private final Map<BundleWiring, ClassPool> classPools = new WeakHashMap<>( 500 );

    @Nullable
    private ServiceTracker<MethodInterceptor, MethodInterceptor> tracker;

    public InterceptionWeavingHook( @Nonnull BundleContext bundleContext )
    {
        this.tracker = new ServiceTracker<>( bundleContext, MethodInterceptor.class, null );
    }

    @Override
    public void destroy() throws Exception
    {
        ServiceTracker<MethodInterceptor, MethodInterceptor> tracker = this.tracker;
        if( tracker != null )
        {
            tracker.close();
        }
        this.tracker = null;
    }

    @Nonnull
    public Collection<MethodInterceptor> getInterceptors()
    {
        ServiceTracker<MethodInterceptor, MethodInterceptor> tracker = this.tracker;
        if( tracker != null )
        {
            return tracker.getTracked().values();
        }
        else
        {
            throw new IllegalStateException( "Weaving hook has been closed" );
        }
    }

    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    @Override
    public synchronized void weave( @Nonnull WovenClass wovenClass )
    {
        BundleWiring bundleWiring = wovenClass.getBundleWiring();

        // don't weave the system bundle or a no-weaving bundle
        if( !shouldWeaveBundle( bundleWiring ) )
        {
            return;
        }

        // iterate the class and see if we have any methods that need weaving
        CtClass ctClass = getCtClass( wovenClass, bundleWiring );
        for( CtMethod originalMethod : ctClass.getDeclaredMethods() )
        {
            try
            {
                // should this method be weaved?
                if( !shouldWeaveMethod( originalMethod ) )
                {
                    continue;
                }

                // rename original method
                String methodName = originalMethod.getName();
                originalMethod.setName( methodName + "0" );

                // create new method replacing the original one
                CtMethod wrapperMethod = CtNewMethod.copy( originalMethod, methodName, originalMethod.getDeclaringClass(), null );
                wrapperMethod.getMethodInfo().addAttribute( new SyntheticAttribute( originalMethod.getMethodInfo().getConstPool() ) );

                //  replace the body of the method with a wrapping that calls our method interceptors via WeaverSpi
                StringBuilder body = new StringBuilder( 300 );
                body.append( "{                                                                                 \n" );
                body.append( "    // create invocation                                                          \n" );
                body.append( "    MethodInvocation $invocation = WeaverSpi.createInvocation(                    \n" );
                body.append( "          $class,             // class declaring current method                   \n" );
                body.append( "          $implMethodName,    // name of the current method                       \n" );
                body.append( "          $sig,               // array of parameter types (class objects)         \n" );
                body.append( "          $0                  // the current 'this'                               \n" );
                body.append( "    );                                                                            \n" );
                body.append( returnsVoid( originalMethod )
                             ? "  $invocation.proceed( $args );                                                 \n"
                             : "  return $invocation.proceed( $args );                                          \n" );
                body.append( "}                                                                                 \n" );

                // finalize method source code
                String src = body.toString();
                src = src.replace( "$implMethodName", "\"" + originalMethod.getName() + "\"" );
                src = src.replace( "WeaverSpi", WeaverSpi.class.getName() );
                src = src.replace( "MethodInvocation", MethodInvocation.class.getName() );

                // change method body (apply)
                wrapperMethod.setBody( src );
            }
            catch( Exception e )
            {
                throw new IllegalStateException( "Could not weave target class '" + wovenClass.getClassName() + "': " + e.getMessage(), e );
            }
        }

        // if modified - apply to woven class
        if( ctClass.isModified() )
        {
            try
            {
                addWeaverSpiPackageImport( wovenClass );
                wovenClass.setBytes( ctClass.toBytecode() );
            }
            catch( Exception e )
            {
                throw new IllegalStateException( "Could not weave target class '" + wovenClass.getClassName() + "': " + e.getMessage(), e );
            }
        }
    }

    private boolean returnsVoid( CtMethod originalMethod ) throws NotFoundException
    {
        return Void.class.getName().equals( originalMethod.getReturnType().getName() );
    }

    private boolean shouldWeaveBundle( BundleWiring bundleWiring )
    {
        return bundleWiring.getBundle().getBundleId() != 0 && !FORBIDDEN_BUNDLES.contains( bundleWiring.getBundle().getSymbolicName() );
    }

    @Nonnull
    private ClassPool getClassPool( @Nonnull final BundleWiring bundleWiring )
    {
        ClassPool classPool = this.classPools.get( bundleWiring );
        if( classPool == null )
        {
            classPool = new ClassPool( false );
            classPool.appendClassPath( new WeavingClassPath( bundleWiring ) );
            this.classPools.put( bundleWiring, classPool );
        }
        return classPool;
    }

    private CtClass getCtClass( WovenClass wovenClass, BundleWiring bundleWiring )
    {
        CtClass ctClass;
        try
        {
            ctClass = getClassPool( bundleWiring ).get( wovenClass.getClassName() );
        }
        catch( NotFoundException e )
        {
            throw new WeavingException( "Could not find weaving target class '" + wovenClass.getClassName() + "': " + e.getMessage(), e );
        }
        return ctClass;
    }

    private boolean shouldWeaveMethod( CtMethod method )
    {
        for( Object ann : method.getAvailableAnnotations() )
        {
            if( isEnablingWeaving( ( Annotation ) ann ) )
            {
                return true;
            }
        }
        for( Object[] parameterAnnotations : method.getAvailableParameterAnnotations() )
        {
            for( Object ann : parameterAnnotations )
            {
                if( isEnablingWeaving( ( Annotation ) ann ) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private boolean isEnablingWeaving( Annotation annotation )
    {
        Class<? extends Annotation> annType = annotation.annotationType();
        if( annType.getPackage().getName().startsWith( "javax.annotation." ) )
        {
            return true;
        }
        else
        {
            return AnnotationUtils.getAnnotation( annType, EnableWeaving.class ) != null;
        }
    }

    private void addWeaverSpiPackageImport( WovenClass wovenClass )
    {
        boolean addSpiImport = true;
        List<String> imports = wovenClass.getDynamicImports();
        for( String anImport : imports )
        {
            if( anImport.contains( WeaverSpi.class.getPackage().getName() ) )
            {
                addSpiImport = false;
                break;
            }
        }

        if( addSpiImport )
        {
            imports.add( format( "%s;version=\"%s\"", WeaverSpi.class.getPackage().getName(), "[1,2)" ) );
        }
    }
}
