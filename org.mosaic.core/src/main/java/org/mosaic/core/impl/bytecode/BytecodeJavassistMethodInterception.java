package org.mosaic.core.impl.bytecode;

import java.util.HashMap;
import java.util.Map;
import javassist.*;
import javassist.bytecode.AccessFlag;
import org.mosaic.core.MethodInterceptor;
import org.mosaic.core.ModuleRevision;
import org.mosaic.core.util.Nonnull;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javassist.Modifier.*;
import static javassist.bytecode.AccessFlag.BRIDGE;
import static javassist.bytecode.AccessFlag.SYNTHETIC;
import static org.mosaic.core.impl.bytecode.BytecodeUtil.*;

/**
 * @author arik
 */
class BytecodeJavassistMethodInterception
{
    void weaveMethodsInterception( @Nonnull ModuleRevision moduleRevision, @Nonnull CtClass ctClass )
    {
        try
        {
            if( BytecodeUtil.isSubtypeOf( ctClass, MethodInterceptor.class.getName() ) )
            {
                Logger logger = LoggerFactory.getLogger( ctClass.getName() );
                logger.info( "Class '{}' will not be weaved for method interception, because it implements {}",
                             ctClass.getName(), MethodInterceptor.class.getName() );
                return;
            }

            // creates a shared map of method entries for this class
            Map<CtMethod, Long> methodIds = createMethodIdsMap( moduleRevision, ctClass );

            // weave methods for interception
            for( CtMethod method : ctClass.getDeclaredMethods() )
            {
                Long id = methodIds.get( method );
                if( id != null )
                {
                    weaveInterceptionFor( id, method );
                }
            }
        }
        catch( NotFoundException ignore )
        {
            // simply not weaving the class; it won't load anyway...
        }
        catch( WeavingException e )
        {
            throw e;
        }
        catch( Throwable e )
        {
            throw new WeavingException( "could not weave interception for methods in '" + ctClass.getName() + "': " + e.getMessage(), e );
        }
    }

    @Nonnull
    private Map<CtMethod, Long> createMethodIdsMap( @Nonnull ModuleRevision moduleRevision,
                                                    @Nonnull CtClass ctClass )
    {
        try
        {
            CtConstructor classInitializer = ctClass.getClassInitializer();
            if( classInitializer == null )
            {
                classInitializer = ctClass.makeClassInitializer();
            }
            ctClass.addField( CtField.make( "public static final java.util.Map __METHOD_ENTRIES = new java.util.HashMap(100);", ctClass ) );

            // this counter will increment for each declared method in this class
            // ie. each methods receives a unique ID (in the context of this class..)
            long id = 0;

            // iterate declared methods, and for each method, add code that populates the __METHOD_ENTRIES static map
            // with a MethodEntry for that method. The entry will receive the method's unique ID.
            // in addition to generating the code to populate the class's method entries map, we save the method IDs
            // mapping in a local map here and return it - so that the code weaved to our methods will use that id to
            // fetch method entry on runtime when methods are invoked.

            StringBuilder methodIdMapSrc = new StringBuilder();
            Map<CtMethod, Long> methodIds = new HashMap<>();
            for( CtMethod method : ctClass.getDeclaredMethods() )
            {
                int acc = AccessFlag.of( method.getModifiers() );
                if( ( acc & BRIDGE ) == 0 && ( acc & SYNTHETIC ) == 0 )
                {
                    int modifiers = method.getModifiers();
                    if( !isAbstract( modifiers ) && !isNative( modifiers ) )
                    {
                        long methodId = id++;
                        methodIdMapSrc.append(
                                javaCode(
                                        "%s.__METHOD_ENTRIES.put(                                       \n" +
                                        "   Long.valueOf( %dl ),                                        \n" +
                                        "   new MethodEntry( %dl, %dl, %dl, %s.class, \"%s\", \"%s\" )  \n" +
                                        ");                                                             \n",
                                        ctClass.getName(),                      // first %s
                                        methodId,                               // key of map
                                        methodId,                               // 1st param
                                        moduleRevision.getModule().getId(),     // 2nd param
                                        moduleRevision.getId(),                 // 3rd param
                                        ctClass.getName(),                      // 4th param
                                        method.getName(),                       // 5th param
                                        getParametersDesc( method )             // 6th param
                                )
                        );
                        methodIds.put( method, methodId );
                    }
                }
            }
            classInitializer.insertBefore( "{\n" + methodIdMapSrc.toString() + "}" );
            return methodIds;
        }
        catch( Throwable e )
        {
            throw new WeavingException( "could not create class initializer for class '" + ctClass.getName() + "': " + e.getMessage(), e );
        }
    }

    private void weaveInterceptionFor( long methodId, @Nonnull CtMethod method ) throws CannotCompileException
    {
        try
        {
            // call 'afterSuccessfulInvocation( (MyReturnType) myReturnValue )' if all is good, at the end of execution
            method.insertAfter( javaCode( getReturnStatement( method, "ModulesSpi.afterSuccessfulInvocation( ($w) $_ )" ) ), false );

            // add exception handler which calls 'afterThrowable(e)' and then throws the catched exception
            method.addCatch(
                    javaCode( "{                \n" +
                              "   %s            \n" +
                              "   throw $e;     \n" +
                              "}                \n",
                              getReturnStatement( method, "ModulesSpi.afterThrowable( $e )" )
                    ),
                    method.getDeclaringClass().getClassPool().get( Throwable.class.getName() ),
                    "$e"
            );

            // add code that invokes the 'before' interception
            method.insertBefore(
                    javaCode(
                            "{                                                                                      \n" +
                            "   MethodEntry __myEntry = (MethodEntry) __METHOD_ENTRIES.get( Long.valueOf( %dl ) );  \n" +
                            "   if( !ModulesSpi.beforeInvocation( __myEntry, %s, $args ) )                          \n" +
                            "   {                                                                                   \n" +
                            "       %s;                                                                             \n" +
                            "   }                                                                                   \n" +
                            "}                                                                                      \n",
                            methodId,
                            isStatic( method.getModifiers() ) ? "null" : "this",
                            getReturnStatement( method, "ModulesSpi.afterAbortedInvocation()" )
                    )
            );

            // add code that invokes the 'after' interception
            method.insertAfter(
                    javaCode(
                            "ModulesSpi.cleanup( (MethodEntry)__METHOD_ENTRIES.get( Long.valueOf( %dl ) ) );",
                            methodId
                    ),
                    true
            );
        }
        catch( NotFoundException ignore )
        {
            // ignoring since class wouldn't load anyway
        }
    }
}
