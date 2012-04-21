package org.mosaic.server.boot.impl.logging;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javassist.*;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.logging.Trace;
import org.mosaic.osgi.util.BundleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;

import static org.osgi.framework.FrameworkUtil.getBundle;

/**
 * @author arik
 */
public class LogWeaver implements WeavingHook {

    private static final String ENTER_CODE =
            "{\n" +
            "   String $mosaicMethodName = \"___METHOD_NAME___\";\n" +
            "   org.mosaic.logging.LoggerFactory.getLogger( this.getClass() ).trace( \n" +
            "       \"Entering '{}'\", new Object[]{$mosaicMethodName} \n" +
            "   );\n" +
            "}\n";

    private static final String EXIT_CODE =
            "{\n" +
            "   String $mosaicMethodName = \"___METHOD_NAME___\";\n" +
            "   org.mosaic.logging.LoggerFactory.getLogger( this.getClass() ).trace( \n" +
            "       \"Exiting '{}'\", new Object[]{$mosaicMethodName} \n" +
            "   );\n" +
            "}\n";

    private static final String EXCEPTION_CODE =
            "{\n" +
            "   String $mosaicMethodName = \"___METHOD_NAME___\";\n" +
            "   org.mosaic.logging.LoggerFactory.getLogger( this.getClass() ).trace( \n" +
            "       \"Exiting '{}'\", new Object[]{$mosaicMethodName,$e.getMessage(),$e} \n" +
            "   );\n" +
            "   throw $e;\n" +
            "}\n";

    private static final Logger LOG = LoggerFactory.getLogger( LogWeaver.class );

    private static final Set<String> IGNORED_BUNDLES = new HashSet<>( Arrays.asList(
            "org.mosaic.api", "org.mosaic.server.api"
    ) );

    private final String orgMosaicLoggingPackageVersion;

    private BundleContext bundleContext;

    private ServiceRegistration<WeavingHook> registration;

    public LogWeaver() {
        this.orgMosaicLoggingPackageVersion = getBundle( LoggerFactory.class ).getVersion().toString();
    }

    public void open( BundleContext bundleContext ) {
        this.bundleContext = bundleContext;
        this.bundleContext.registerService( WeavingHook.class, this, null );
    }

    public void close() {
        try {
            if( this.registration != null ) {
                this.registration.unregister();
            }
        } catch( IllegalStateException ignore ) {
        } finally {
            this.registration = null;
            this.bundleContext = null;
        }
    }

    @Override
    public void weave( WovenClass wovenClass ) {
        if( this.bundleContext == null ) {

            // not initialized yet or already closed - ignore call
            return;

        } else if( wovenClass.getBundleWiring().getBundle().getBundleId() == this.bundleContext.getBundle().getBundleId() ) {

            // we mustn't weave classes from our bundle - will cause a circular class load
            return;

        } else if( IGNORED_BUNDLES.contains( wovenClass.getBundleWiring().getBundle().getSymbolicName() ) ) {

            // also never weave classes in the API bundles (causes many head-aches...)
            return;

        }

        // javassist uses thread context class-loader - set it to weaved bundle's class loader
        ClassLoader previousTCCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader( wovenClass.getBundleWiring().getClassLoader() );
        try {

            // instrument the class if it has @Transactional methods; if so, it will be returned to OSGi container
            CtClass ctClass = instrument( createClassPool( wovenClass ), wovenClass );
            if( ctClass != null ) {
                wovenClass.getDynamicImports().addAll( Arrays.asList(
                        "org.mosaic.logging;version:=\"" + this.orgMosaicLoggingPackageVersion + "\","
                ) );
                wovenClass.setBytes( ctClass.toBytecode() );
            }

        } catch( Exception e ) {
            throw new WeavingException( "Error weaving class '" + wovenClass.getClassName() + "': " + e.getMessage(), e );

        } finally {
            Thread.currentThread().setContextClassLoader( previousTCCL );
        }
    }

    private ClassPool createClassPool( WovenClass wovenClass ) {
        ClassPool classPool = new ClassPool( false );
        classPool.appendClassPath( new LoaderClassPath( wovenClass.getBundleWiring().getClassLoader() ) );
        classPool.appendClassPath( new LoaderClassPath( getClass().getClassLoader() ) );
        return classPool;
    }

    private CtClass instrument( ClassPool classPool, WovenClass wovenClass ) throws IOException,
            ClassNotFoundException, CannotCompileException, NotFoundException {

        Bundle wovenBundle = wovenClass.getBundleWiring().getBundle();
        String wovenClassName = wovenClass.getClassName();
        boolean modified = false;

        CtClass ctClass = classPool.makeClass( new ByteArrayInputStream( wovenClass.getBytes() ) );
        CtClass currentClass = ctClass;
        while( currentClass != null ) {
            for( CtMethod method : currentClass.getDeclaredMethods() ) {
                String methodName = method.getName();

                Object annotation = method.getAnnotation( Trace.class );
                if( annotation != null ) {
                    LOG.trace( "Weaving method '{}' in class '{}' of bundle '{}'", methodName, wovenClassName, BundleUtils.toString( wovenBundle ) );

                    // add code that will run before actual source code and start/join transactions
                    method.insertBefore( ENTER_CODE.replace( "___METHOD_NAME___", methodName ) );

                    // add code to commit transaction. this code runs after the actual method's source code, and
                    // assumes that everything went well (no exceptions). if exceptions occur, this code won't run
                    method.insertAfter( EXIT_CODE.replace( "___METHOD_NAME___", methodName ) );

                    // add code that catches any exception, and rolls back the transaction, unless it hasn't started
                    // yet (checks for CannotCreateTransactionException)
                    method.addCatch( EXCEPTION_CODE.replace( "___METHOD_NAME___", methodName ), classPool.get( "java.lang.Exception" ), "$e" );

                    // remember that at-least one method was modified, so byte code will be returned to OSGi container
                    modified = true;
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        // if at least one method was modified, return this modified class; otherwise, discard this CtClass
        if( modified ) {
            return ctClass;
        } else {
            if( ctClass != null ) {
                ctClass.detach();
            }
            return null;
        }
    }
}
