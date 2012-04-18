package org.mosaic.server.transaction.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javassist.*;
import org.mosaic.lifecycle.ContextRef;
import org.mosaic.lifecycle.ServiceExport;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.osgi.util.BundleUtils;
import org.mosaic.server.transaction.Transactions;
import org.mosaic.transaction.Transactional;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import static org.osgi.framework.FrameworkUtil.getBundle;

/**
 * @author arik
 * @WISH 4/17/12 the 'addCatch' works only if adding '-XX:-UseSplitVerifier' to the JVM options (see https://play.lighthouseapp.com/projects/57987/tickets/1234-javalangverifyerror-inconsistent-stackmap-frames-when-i-try-to-use-play-124-rc2-and-java-7)
 * @WISH 4/17/12 reuse ClassPool instances to improve performance
 */
@SuppressWarnings( "NullableProblems" )
@Component
@ServiceExport( WeavingHook.class )
public class JavassistTxWeavingHook implements WeavingHook {

    private static final String BEGIN_TX_CODE =
            "{\n" +
            "   String $mosaicTxName = \"___TX_NAME___\";\n" +
            "   org.mosaic.server.transaction.Transactions.begin( $mosaicTxName, this );\n" +
            "}\n";

    private static final String COMMIT_TX_CODE = "org.mosaic.server.transaction.Transactions.finish();";

    private static final String ROLLBACK_TX_CODE =
            "{\n" +
            "   if( $e instanceof org.springframework.transaction.CannotCreateTransactionException ) {\n" +
            "       throw $e;" +
            "   } else {\n" +
            "       org.mosaic.server.transaction.Transactions.rollback();\n" +
            "       throw $e;\n" +
            "   };\n" +
            "}\n";

    private static final Logger LOG = LoggerFactory.getBundleLogger( JavassistTxWeavingHook.class );

    private static final Set<String> IGNORED_BUNDLES = new HashSet<>( Arrays.asList(
            "org.mosaic.api", "org.mosaic.server.api"
    ) );

    private final String orgSpringframeworkTransactionPackageVersion;

    private final String orgMosaicServerTransactionPackageVersion;

    private BundleContext bundleContext;

    public JavassistTxWeavingHook() {
        this.orgSpringframeworkTransactionPackageVersion = getBundle( TransactionStatus.class ).getVersion().toString();
        this.orgMosaicServerTransactionPackageVersion = getBundle( Transactions.class ).getVersion().toString();
    }

    @ContextRef
    public void setBundleContext( BundleContext bundleContext ) {
        this.bundleContext = bundleContext;

    }

    @Override
    public void weave( WovenClass wovenClass ) {
        if( wovenClass.getBundleWiring().getBundle().getBundleId() == this.bundleContext.getBundle().getBundleId() ) {

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
                        "org.mosaic.server.transaction;version:=\"" + this.orgSpringframeworkTransactionPackageVersion + "\",",
                        "org.springframework.transaction;version:=\"" + this.orgMosaicServerTransactionPackageVersion + "\""
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

                Object annotation = method.getAnnotation( Transactional.class );
                if( annotation != null ) {
                    LOG.trace( "Weaving method '{}' in class '{}' of bundle '{}'", methodName, wovenClassName, BundleUtils.toString( wovenBundle ) );

                    // add code that will run before actual source code and start/join transactions
                    String txName = wovenClassName + "." + methodName;
                    method.insertBefore( BEGIN_TX_CODE.replace( "___TX_NAME___", txName ) );

                    // add code to commit transaction. this code runs after the actual method's source code, and
                    // assumes that everything went well (no exceptions). if exceptions occur, this code won't run
                    method.insertAfter( COMMIT_TX_CODE );

                    // add code that catches any exception, and rolls back the transaction, unless it hasn't started
                    // yet (checks for CannotCreateTransactionException)
                    method.addCatch( ROLLBACK_TX_CODE, classPool.get( "java.lang.Exception" ), "$e" );

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
