package org.mosaic.core.weaving.impl;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javassist.*;
import javassist.bytecode.Descriptor;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.weaving.MethodEntry;
import org.mosaic.core.weaving.WeavingSpi;
import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleRevision;

import static java.lang.String.format;

/**
 * @author arik
 */
class BytecodeUtil
{

    private static final Pattern BUNDLE_REVISION_REGEX = Pattern.compile( "\\d+\\.(\\d+)" );

    @Nonnull
    static String getParametersDesc( @Nonnull CtMethod method ) throws NotFoundException
    {
        return Descriptor.ofMethod( null, method.getParameterTypes() );
    }

    static void addBeforeBody( @Nonnull Collection<CtConstructor> ctors, @Nonnull String statement )
            throws CannotCompileException
    {
        for( CtConstructor constructor : ctors )
        {
            constructor.insertBeforeBody( statement );
        }
    }

    @Nonnull
    static Collection<CtConstructor> getSuperCallingConstructors( @Nonnull WovenClass wovenClass,
                                                                  @Nonnull CtClass ctClass )
    {
        try
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
        catch( Throwable e )
        {
            throw new WeavingException( "could not find list of super-calling constructors for class '" + ctClass.getName() + "' in " + toString( wovenClass.getBundleWiring().getRevision() ), e );
        }
    }

    @Nullable
    static CtClass loadConcreteClass( @Nonnull WovenClass wovenClass )
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

    static boolean isSubtypeOf( @Nonnull CtClass type, @Nonnull String superTypeName ) throws NotFoundException
    {
        if( type.getName().equals( superTypeName ) )
        {
            return true;
        }

        for( String interfaceName : type.getClassFile2().getInterfaces() )
        {
            try
            {
                if( interfaceName.equals( superTypeName ) || isSubtypeOf( type.getClassPool().get( interfaceName ), superTypeName ) )
                {
                    return true;
                }
            }
            catch( NotFoundException ignore )
            {
                // simply not weaving the class; it won't load anyway...
            }
        }

        String supername = type.getClassFile2().getSuperclass();
        return supername != null && isSubtypeOf( type.getClassPool().get( supername ), superTypeName );
    }

    @Nonnull
    static String getReturnStatement( @Nonnull CtMethod method, @Nonnull String valueStmt ) throws NotFoundException
    {
        CtClass returnType = method.getReturnType();
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
    static String javaCode( @Nonnull String code, @Nonnull Object... args )
    {
        return format( code, args )
                .replace( WeavingSpi.class.getSimpleName(), WeavingSpi.class.getName() )
                .replace( MethodEntry.class.getSimpleName(), MethodEntry.class.getName() );
    }

    @Nonnull
    static String toString( @Nonnull BundleRevision bundleRevision )
    {
        Bundle bundle = bundleRevision.getBundle();
        return bundle.getSymbolicName() + "@" + bundle.getVersion() + "[" + bundleRevision + "]";
    }

    static int getBundleRevisionNumber( @Nonnull BundleRevision bundleRevision )
    {
        Matcher matcher = BUNDLE_REVISION_REGEX.matcher( bundleRevision.toString() );
        if( matcher.matches() )
        {
            return Integer.parseInt( matcher.group( 1 ) );
        }
        else
        {
            throw new IllegalStateException( "could not extract bundle revision number from " + bundleRevision );
        }
    }

    private BytecodeUtil()
    {
    }
}
