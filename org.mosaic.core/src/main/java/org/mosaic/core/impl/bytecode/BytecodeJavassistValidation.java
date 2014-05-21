package org.mosaic.core.impl.bytecode;

import java.lang.annotation.Annotation;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.mosaic.core.ModuleRevision;
import org.mosaic.core.util.Nonnull;
import org.osgi.framework.hooks.weaving.WeavingException;

import static javassist.Modifier.isAbstract;
import static javassist.Modifier.isNative;
import static org.mosaic.core.impl.bytecode.BytecodeUtil.javaCode;

/**
 * @author arik
 */
class BytecodeJavassistValidation
{
    void weaveNonnullChecks( @Nonnull ModuleRevision moduleRevision, @Nonnull CtClass ctClass )
    {
        for( CtBehavior behavior : ctClass.getDeclaredBehaviors() )
        {
            try
            {
                if( isAbstract( behavior.getModifiers() ) || isNative( behavior.getModifiers() ) )
                {
                    continue;
                }

                // weave checks for @Nonnull parameters
                Object[][] availableParameterAnnotations = behavior.getAvailableParameterAnnotations();
                for( int i = 0; i < availableParameterAnnotations.length; i++ )
                {
                    for( Object annotationObject : availableParameterAnnotations[ i ] )
                    {
                        Annotation annotation = ( Annotation ) annotationObject;
                        if( annotation.annotationType().equals( Nonnull.class ) )
                        {
                            behavior.insertBefore(
                                    javaCode( "if( $%d == null )                                                                                                     \n" +
                                              "{                                                                                                                     \n" +
                                              "  throw new NullPointerException( \"Method parameter %d of method '%s' is null, but is annotated with @Nonnull\" );   \n" +
                                              "}                                                                                                                     \n",
                                              i + 1, i, behavior.getLongName()
                                    )
                            );
                        }
                    }
                }

                // weave check for return type of @Nonnull methods
                if( behavior instanceof CtMethod )
                {
                    CtMethod method = ( CtMethod ) behavior;
                    if( method.hasAnnotation( Nonnull.class ) && !method.getReturnType().getName().equals( Void.class.getName() ) )
                    {
                        method.insertAfter( javaCode(
                                "if( $_ == null )                                                                                       \n" +
                                "{                                                                                                      \n" +
                                "   throw new NullPointerException( \"Method '%s' returned null, but is annotated with @Nonnull\" );    \n" +
                                "}                                                                                                      \n",
                                method.getLongName()
                        ) );
                    }
                }
            }
            catch( WeavingException e )
            {
                throw e;
            }
            catch( NotFoundException ignore )
            {
                // simply not weaving the class; it won't load anyway...
            }
            catch( Throwable e )
            {
                throw new WeavingException( "could not weave @Nonnull for behavior '" + behavior.getLongName() + "' of '" + ctClass.getName() + "' in " + moduleRevision, e );
            }
        }
    }
}
