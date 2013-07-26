package org.mosaic.util.weaving.impl;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javassist.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.Valid;
import org.mosaic.lifecycle.impl.util.ServiceUtils;
import org.mosaic.util.weaving.EnableWeaving;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import static java.lang.String.format;
import static java.util.Arrays.asList;

/**
 * @author arik
 */
public class MethodInterceptorWeaver implements WeavingHook, InitializingBean, DisposableBean
{
    @Nonnull
    private static final List<String> FORBIDDEN_BUNDLES = asList(
            "org.mosaic.api",
            "org.mosaic.lifecycle"
    );

    @Nonnull
    private final BundleContext bundleContext;

    @Nonnull
    private final Map<ClassLoader, ClassPool> classPools = new WeakHashMap<>( 1000 );

    @Nullable
    private ServiceRegistration<WeavingHook> serviceRegistration;

    public MethodInterceptorWeaver( @Nonnull BundleContext bundleContext )
    {
        this.bundleContext = bundleContext;
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
    public synchronized void weave( WovenClass wovenClass )
    {
        BundleWiring bundleWiring = wovenClass.getBundleWiring();
        String symbolicName = bundleWiring.getBundle().getSymbolicName();

        // ignore forbidden bundles (including the system bundle)
        if( bundleWiring.getBundle().getBundleId() == 0 || FORBIDDEN_BUNDLES.contains( symbolicName ) )
        {
            return;
        }

        // find/create the Javassist class pool for the woven class' classloader
        ClassLoader classLoader = bundleWiring.getClassLoader();
        ClassPool classPool = this.classPools.get( classLoader );
        if( classPool == null )
        {
            classPool = new ClassPool( false );
            classPool.appendClassPath( new LoaderClassPath( getClass().getClassLoader() ) );
            classPool.appendClassPath( new LoaderClassPath( classLoader ) );
            this.classPools.put( classLoader, classPool );
        }

        // find/create the Javassist class descriptor for the class being woven
        CtClass ctClass;
        try
        {
            ctClass = classPool.get( wovenClass.getClassName() );
        }
        catch( NotFoundException e )
        {
            throw new WeavingException( "Could not weave class '" + wovenClass.getClassName() + "': " + e.getMessage(), e );
        }

        // ignore this class if it's an interface, annotation or enum (no code injection needed in these cases)
        if( !ctClass.isInterface() && !ctClass.isAnnotation() && !ctClass.isEnum() )
        {
            try
            {
                // find methods to weave :)
                for( CtMethod ctMethod : ctClass.getDeclaredMethods() )
                {
                    processMethod( ctClass, ctMethod );
                }

                // save the class if it was modified
                if( ctClass.isModified() )
                {
                    // add a dynamic import for 'org.mosaic.util.weaving.spi' package
                    wovenClass.getDynamicImports().add( format( "org.mosaic.util.weaving.spi;version=\"%s\"", "[1,2)" ) );
                    wovenClass.setBytes( ctClass.toBytecode() );
                }
            }
            catch( Exception e )
            {
                throw new WeavingException( "Could not weave class '" + wovenClass.getClassName() + "': " + e.getMessage(), e );
            }
        }
    }

    private void processMethod( CtClass ctClass, CtMethod ctMethod ) throws NotFoundException, CannotCompileException
    {
        String ctMethodLongName = ctMethod.getLongName();
        if( Modifier.isAbstract( ctMethod.getModifiers() ) )
        {
            // skip abstract methods
            return;
        }

        // check method return value if it is annotated with @Nonnull
        if( !ctMethod.getReturnType().getName().equals( Void.class.getName() ) && ctMethod.hasAnnotation( Nonnull.class ) )
        {
            ctMethod.insertAfter( String.format(
                    "{                                                                                                          \n" +
                    "   if( $_ == null )                                                                                        \n" +
                    "   {                                                                                                       \n" +
                    "       throw new NullPointerException( \"Method '%s' returned null, but is annotated with @Nonnull\" );    \n" +
                    "   }                                                                                                       \n" +
                    "}                                                                                                          \n",
                    ctMethodLongName
            ) );
        }

        // check parameter values if they are annotated with @Nonnull
        Object[][] availableParameterAnnotations = ctMethod.getAvailableParameterAnnotations();
        for( int i = 0; i < availableParameterAnnotations.length; i++ )
        {
            for( Object annotationObject : availableParameterAnnotations[ i ] )
            {
                Annotation annotation = ( Annotation ) annotationObject;
                if( annotation.annotationType().equals( Nonnull.class ) )
                {
                    String src = String.format(
                            "{                                                                                                                          \n" +
                            "   if( $%d == null )                                                                                                       \n" +
                            "   {                                                                                                                       \n" +
                            "       throw new NullPointerException( \"Method parameter %d of method '%s' is null, but is annotated with @Nonnull\" );   \n" +
                            "   }                                                                                                                       \n" +
                            "}                                                                                                                          \n",
                            i + 1, i, ctMethodLongName
                    );
                    ctMethod.insertBefore( src );
                }
            }
        }

        boolean enableWeaving = false;

        // check if the method is annotated with a weaving-triggering annotation (such as @EnableWeaving)
        for( Object annObject : ctMethod.getAvailableAnnotations() )
        {
            if( isWeavingTriggeringAnnotation( ( Annotation ) annObject ) )
            {
                enableWeaving = true;
                break;
            }
        }

        // check if one of the method parameters is annotated with a weaving-triggering annotation (such as @EnableWeaving)
        if( !enableWeaving )
        {
            for( Object[] parameterAnnObjects : ctMethod.getAvailableParameterAnnotations() )
            {
                for( Object parameterAnnObject : parameterAnnObjects )
                {
                    if( isWeavingTriggeringAnnotation( ( Annotation ) parameterAnnObject ) )
                    {
                        enableWeaving = true;
                        break;
                    }
                }
                if( enableWeaving )
                {
                    break;
                }
            }
        }

        if( enableWeaving )
        {
            //noinspection UnnecessaryLocalVariable
            CtMethod origMethod = ctMethod;
            CtMethod implMethod = CtNewMethod.copy( origMethod, origMethod.getName() + "$$Impl", ctClass, null );
            implMethod.setModifiers( Modifier.setPublic( implMethod.getModifiers() ) );
            ctClass.addMethod( implMethod );
            origMethod.setBody( createDelegatorBody( ctClass, origMethod, implMethod ) );
        }
    }

    private boolean isWeavingTriggeringAnnotation( @Nonnull Annotation annotation )
    {
        if( annotation.annotationType().getName().equals( Valid.class.getName() ) )
        {
            return true;
        }

        for( Annotation metaAnnotation : annotation.annotationType().getAnnotations() )
        {
            Class<? extends Annotation> metaAnnotationType = metaAnnotation.annotationType();
            if( metaAnnotationType.getName().equals( EnableWeaving.class.getName() )
                || metaAnnotationType.getName().startsWith( "org.hibernate.validator.constraints." )
                || metaAnnotationType.getName().startsWith( "javax.validation.constraints." ) )
            {
                return true;
            }
        }
        return false;
    }

    private String createDelegatorBody( CtClass ctClass, CtMethod origMethod, CtMethod implMethod )
            throws NotFoundException
    {
        String type = origMethod.getReturnType().getName();
        StringBuilder body = new StringBuilder();
        body.append( "{\n" );
        CtClass[] parameterTypes = implMethod.getParameterTypes();
        body.append( "  Class[] paramTypes$$ = new Class[" ).append( parameterTypes.length ).append( "];\n" );
        for( int i = 0; i < parameterTypes.length; i++ )
        {
            body.append( "  paramTypes$$[" ).append( i ).append( "] = " ).append( parameterTypes[ i ].getName() ).append( ".class;\n" );
        }
        if( !"void".equals( type ) )
        {
            body.append( "  return " );
        }
        body.append( "org.mosaic.util.weaving.spi.WeavingSpi.getInstance().intercept( \n" );
        body.append( "    this, \n" );
        body.append( "    " ).append( ctClass.getName() ).append( ".class, \n" );
        body.append( "    \"" ).append( origMethod.getName() ).append( "\", \n" );
        body.append( "    paramTypes$$, \n" );
        body.append( "    $args \n" );
        body.append( "  );\n" );
        body.append( "}\n" );
        return body.toString();
    }
}
