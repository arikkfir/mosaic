package org.mosaic.database.tx.impl.weaving;

import java.util.Map;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javax.annotation.Nonnull;
import org.mosaic.database.tx.annotation.ReadOnly;
import org.mosaic.database.tx.annotation.ReadWrite;
import org.mosaic.database.tx.spi.TransactionSpi;
import org.mosaic.util.weaving.impl.BaseWeavingHook;
import org.mosaic.util.weaving.impl.JavassistClassPoolManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.weaving.WovenClass;

/**
 * @author arik
 */
public class TransactionWeavingHook extends BaseWeavingHook
{
    public TransactionWeavingHook( @Nonnull BundleContext bundleContext,
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

            // check method return value if it is annotated with @Nonnull
            boolean readOnly = ctMethod.hasAnnotation( ReadOnly.class );
            boolean readWrite = ctMethod.hasAnnotation( ReadWrite.class );
            if( readOnly && readWrite )
            {
                throw new IllegalStateException( "Method '" + ctMethodLongName + "' has both @ReadOnly and @ReadWrite" );
            }
            else if( readOnly || readWrite )
            {
                // add tx-begin code
                String beginCode = String.format( "TransactionSpi.begin( %s, %s );", ctMethodLongName, readOnly );
                beginCode = beginCode.replace( "TransactionSpi", TransactionSpi.class.getName() );
                ctMethod.insertBefore( beginCode );

                // add tx-apply code
                String applyCode = String.format( "TransactionSpi.apply();" );
                applyCode = applyCode.replace( "TransactionSpi", TransactionSpi.class.getName() );
                ctMethod.insertAfter( applyCode );

                // add tx-fail code
                String failCode = String.format( "TransactionSpi.fail( $e );" +
                                                 "throw $e;" );
                failCode = failCode.replace( "TransactionSpi", TransactionSpi.class.getName() );
                for( CtClass exceptionType : ctMethod.getExceptionTypes() )
                {
                    ctMethod.addCatch( failCode, exceptionType, "$e" );
                }
                ctMethod.addCatch( failCode, findCtClass( wovenClass, RuntimeException.class ), "$e" );

                // import required packages
                dynamicImports.put( TransactionSpi.class.getPackage().getName(), "[1,2)" );
            }
        }
    }
}
