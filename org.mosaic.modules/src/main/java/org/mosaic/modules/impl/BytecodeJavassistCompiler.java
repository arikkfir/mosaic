package org.mosaic.modules.impl;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.util.*;
import javassist.*;
import javassist.bytecode.AccessFlag;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Service;
import org.mosaic.modules.spi.MethodEntry;
import org.mosaic.modules.spi.MethodInterceptor;
import org.mosaic.modules.spi.ModulesSpi;
import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static javassist.Modifier.*;
import static javassist.bytecode.AccessFlag.BRIDGE;
import static javassist.bytecode.AccessFlag.SYNTHETIC;

/**
 * @author arik
 */
class BytecodeJavassistCompiler extends BytecodeCompiler
{
    @Nullable
    @Override
    byte[] compile( @Nonnull WovenClass wovenClass )
    {
        // ignore our own bundle and specific excluded bundles
        Bundle bundle = wovenClass.getBundleWiring().getRevision().getBundle();

        // weave the mother..!
        CtClass ctClass = loadConcreteClass( wovenClass );
        if( ctClass != null )
        {
            // weave instance initializer that wires @Component, @Service, etc to fields
            weaveFieldInjections( wovenClass, ctClass );

            // weave support for @Nonnull checks to all methods
            weaveNonnullChecks( ctClass );

            // weave support for dynamic method interception (via MethodInterceptor services)
            // the weaved code will invoke any interceptors available at the time a method is called (so the interceptors
            // need not be registered at weave time! they can even come and go as pleased...)
            weaveMethodInterception( ctClass );

            // if any weaving was done - compile the changes to the bytecode
            if( ctClass.isModified() )
            {
                try
                {
                    return ctClass.toBytecode();
                }
                catch( Throwable e )
                {
                    throw new WeavingException( "could not compile class '" + wovenClass.getClassName() + "': " + e.getMessage(), e );
                }
            }
        }

        // no weaving, return null
        return null;
    }

    private void weaveFieldInjections( @Nonnull WovenClass wovenClass, @Nonnull CtClass ctClass )
    {
        // list of constructors that should be weaved with our modifications
        try
        {
            Collection<CtConstructor> superCallingConstructors = getSuperCallingConstructors( ctClass );
            for( CtField field : ctClass.getDeclaredFields() )
            {
                int modifiers = field.getModifiers();
                if( !isStatic( modifiers ) && !isFinal( modifiers ) )
                {
                    if( field.hasAnnotation( org.mosaic.modules.Component.class ) || field.hasAnnotation( Service.class ) )
                    {
                        addBeforeBody(
                                superCallingConstructors,
                                process( "this.%s = (%s) ModulesSpi.getValueForField( %dl, %s.class, \"%s\" );",
                                         field.getName(),
                                         field.getType().getName(),
                                         wovenClass.getBundleWiring().getBundle().getBundleId(),
                                         ctClass.getName(),
                                         field.getName() )
                        );
                    }
                }
            }
        }
        catch( WeavingException e )
        {
            throw e;
        }
        catch( Throwable e )
        {
            throw new WeavingException( "could not weave fields of '" + wovenClass.getClassName() + "': " + e.getMessage(), e );
        }
    }

    private void weaveNonnullChecks( @Nonnull CtClass ctClass )
    {
        try
        {
            for( CtBehavior behavior : ctClass.getDeclaredBehaviors() )
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
                                    process( "if( $%d == null )                                                                                                     \n" +
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
                        method.insertAfter( process(
                                "if( $_ == null )                                                                                       \n" +
                                "{                                                                                                      \n" +
                                "   throw new NullPointerException( \"Method '%s' returned null, but is annotated with @Nonnull\" );    \n" +
                                "}                                                                                                      \n",
                                method.getLongName()
                        ) );
                    }
                }
            }
        }
        catch( WeavingException e )
        {
            throw e;
        }
        catch( Throwable e )
        {
            throw new WeavingException( "could not weave @Nonnull checks for '" + ctClass.getName() + "': " + e.getMessage(), e );
        }
    }

    private void weaveMethodInterception( @Nonnull CtClass ctClass )
    {
        try
        {
            if( isSubtypeOf( ctClass, MethodInterceptor.class.getName() ) )
            {
                Logger logger = LoggerFactory.getLogger( ctClass.getName() );
                logger.info( "Class '{}' will not be weaved for method interception, because it implements {}",
                             ctClass.getName(), MethodInterceptor.class.getName() );
                return;
            }

            // creates a shared map of method entries for this class
            Map<CtMethod, Long> methodIds = createClassInitializer( ctClass );

            // weave methods for interception
            for( CtMethod method : ctClass.getDeclaredMethods() )
            {
                Long id = methodIds.get( method );
                if( id != null )
                {
                    weaveMethodForInterception( id, method );
                }
            }
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
    private Map<CtMethod, Long> createClassInitializer( @Nonnull CtClass ctClass )
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
                                process(
                                        "%s.__METHOD_ENTRIES.put(                           \n" +
                                        "   Long.valueOf( %dl ),                            \n" +
                                        "   new MethodEntry( %dl, %s.class, \"%s\", %s )    \n" +
                                        ");                                                 \n",
                                        ctClass.getName(),
                                        methodId,
                                        methodId,
                                        method.getDeclaringClass().getName(),
                                        method.getName(),
                                        getMethodParametersArrayString( method )
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

    private void weaveMethodForInterception( long id, @Nonnull CtMethod method )
    {
        try
        {
            // add code that invokes the 'after' interception
            String afterSuccessSrc = process( getReturnStatement( method.getReturnType(), "ModulesSpi.afterSuccessfulInvocation( ($w)$_ )" ) );
            method.insertAfter( afterSuccessSrc, false
            );

            // add code that catches exceptions
            String catchSrc =
                    process( "{                                                                                         \n" +
                             "   " + getReturnStatement( method.getReturnType(), "ModulesSpi.afterThrowable( $e )" ) + "\n" +
                             "   throw $e;                                                                              \n" +
                             "}                                                                                         \n"
                    );
            method.addCatch( catchSrc, method.getDeclaringClass().getClassPool().get( Throwable.class.getName() ), "$e" );

            // add code that invokes the 'before' interception
            String beforeSrc = process(
                    "{                                                                                      \n" +
                    "   MethodEntry __myEntry = (MethodEntry) __METHOD_ENTRIES.get( Long.valueOf( %dl ) );  \n" +
                    "   if( !ModulesSpi.beforeInvocation( __myEntry, %s, $args ) )                          \n" +
                    "   {                                                                                   \n" +
                    "       %s;                                                                             \n" +
                    "   }                                                                                   \n" +
                    "}                                                                                      \n",
                    id,
                    isStatic( method.getModifiers() ) ? "null" : "this",
                    getReturnStatement( method.getReturnType(), "ModulesSpi.afterAbortedInvocation()" )
            );
            method.insertBefore( beforeSrc );

            // add code that invokes the 'after' interception
            String afterSrc = process( "ModulesSpi.cleanup( (MethodEntry)__METHOD_ENTRIES.get( Long.valueOf( %dl ) ) );", id );
            method.insertAfter( afterSrc, true );
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
            throw new WeavingException( "could not weave method '" + method.getLongName() + "' of '" + method.getDeclaringClass().getName() + "': " + e.getMessage(), e );
        }
    }

    @Nullable
    private CtClass loadConcreteClass( @Nonnull WovenClass wovenClass )
    {
        try
        {
            ClassPool classPool = new ClassPool( false );
            classPool.appendSystemPath();
            classPool.appendClassPath( new LoaderClassPath( BytecodeJavassistCompiler.class.getClassLoader() ) );
            classPool.appendClassPath( new LoaderClassPath( wovenClass.getBundleWiring().getClassLoader() ) );

            CtClass ctClass = classPool.makeClass( new ByteArrayInputStream( wovenClass.getBytes() ) );
            if( ctClass.isArray() || ctClass.isAnnotation() || ctClass.isEnum() || ctClass.isInterface() )
            {
                return null;
            }
            else
            {
                return ctClass;
            }
        }
        catch( Throwable e )
        {
            throw new WeavingException( "could not read class '" + wovenClass.getClassName() + "': " + e.getMessage(), e );
        }
    }

    @Nonnull
    private String getReturnStatement( @Nonnull CtClass returnType, @Nonnull String valueStmt )
    {
        switch( returnType.getName() )
        {
            case "void":
                return valueStmt + ";";
            case "boolean":
                return format( "return ( (Boolean) %s ).booleanValue();", valueStmt );
            case "byte":
                return format( "return ( (Number) %s ).byteValue();", valueStmt );
            case "char":
                return format( "return ( (Character) %s ).charValue();", valueStmt );
            case "double":
                return format( "return ( (Number) %s ).doubleValue();", valueStmt );
            case "float":
                return format( "return ( (Number) %s ).floatValue();", valueStmt );
            case "int":
                return format( "return ( (Number) %s ).intValue();", valueStmt );
            case "long":
                return format( "return ( (Number) %s ).longValue();", valueStmt );
            case "short":
                return format( "return ( (Number) %s ).shortValue();", valueStmt );
            default:
                return format( "return (%s) %s;", returnType.getName(), valueStmt );
        }
    }

    @Nonnull
    private Collection<CtConstructor> getSuperCallingConstructors( @Nonnull CtClass ctClass )
            throws CannotCompileException
    {
        List<CtConstructor> initializers = new LinkedList<>();
        CtConstructor[] declaredConstructors = ctClass.getDeclaredConstructors();
        for( CtConstructor ctor : declaredConstructors )
        {
            if( ctor.callsSuper() )
            {
                initializers.add( ctor );
            }
        }
        return initializers;
    }

    private boolean isSubtypeOf( @Nonnull CtClass type, @Nonnull String superTypeName ) throws NotFoundException
    {
        if( type.getName().equals( superTypeName ) )
        {
            return true;
        }

        for( String intercaceName : type.getClassFile2().getInterfaces() )
        {
            try
            {
                if( intercaceName.equals( MethodInterceptor.class.getName() )
                    || isSubtypeOf( type.getClassPool().get( intercaceName ), superTypeName ) )
                {
                    return true;
                }
            }
            catch( NotFoundException ignore )
            {
            }
        }

        String supername = type.getClassFile2().getSuperclass();
        return supername != null && isSubtypeOf( type.getClassPool().get( supername ), superTypeName );
    }

    private void addBeforeBody( @Nonnull Collection<CtConstructor> ctors, @Nonnull String statement )
            throws CannotCompileException
    {
        for( CtConstructor constructor : ctors )
        {
            constructor.insertBeforeBody( statement );
        }
    }

    @Nonnull
    private String process( @Nonnull String code, @Nonnull Object... args )
    {
        return String.format( code, args )
                     .replace( MethodEntry.class.getSimpleName(), MethodEntry.class.getName() )
                     .replace( ModulesSpi.class.getSimpleName(), ModulesSpi.class.getName() );
    }

    @Nonnull
    private String getMethodParametersArrayString( @Nonnull CtMethod method ) throws NotFoundException
    {
        CtClass[] parameterTypes = method.getParameterTypes();
        if( parameterTypes == null || parameterTypes.length == 0 )
        {
            return "new String[0]";
        }

        StringBuilder methodParameterTypes = new StringBuilder();
        for( CtClass ctParamType : parameterTypes )
        {
            if( methodParameterTypes.length() > 0 )
            {
                methodParameterTypes.append( ", " );
            }
            methodParameterTypes.append( "\"" ).append( ctParamType.getName() ).append( "\"" );
        }
        return "new String[] { " + methodParameterTypes + "}";
    }
}
