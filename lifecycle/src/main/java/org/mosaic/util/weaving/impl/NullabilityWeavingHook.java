package org.mosaic.util.weaving.impl;

import java.lang.annotation.Annotation;
import java.util.Map;
import javassist.*;
import javax.annotation.Nonnull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.weaving.WovenClass;

/**
 * @author arik
 */
public class NullabilityWeavingHook extends BaseWeavingHook
{
    public NullabilityWeavingHook( @Nonnull BundleContext bundleContext,
                                   @Nonnull JavassistClassPoolManager classPoolManager )
    {
        super( bundleContext, classPoolManager );
    }

    @Override
    protected void weave( @Nonnull WovenClass wovenClass,
                          @Nonnull CtClass ctClass,
                          @Nonnull Map<String, String> dynamicImports ) throws NotFoundException, CannotCompileException
    {
        for( CtMethod ctMethod : ctClass.getDeclaredMethods() )
        {
            String ctMethodLongName = ctMethod.getLongName();
            if( Modifier.isAbstract( ctMethod.getModifiers() ) )
            {
                // skip abstract methods
                continue;
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
        }
    }
}
