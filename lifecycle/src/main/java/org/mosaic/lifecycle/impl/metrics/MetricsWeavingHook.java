package org.mosaic.lifecycle.impl.metrics;

import java.util.Map;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import javax.annotation.Nonnull;
import org.mosaic.lifecycle.Module;
import org.mosaic.lifecycle.annotation.Measure;
import org.mosaic.lifecycle.spi.MetricsSpi;
import org.mosaic.util.weaving.impl.BaseWeavingHook;
import org.mosaic.util.weaving.impl.JavassistClassPoolManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.weaving.WovenClass;

/**
 * @author arik
 */
public class MetricsWeavingHook extends BaseWeavingHook
{
    public MetricsWeavingHook( @Nonnull BundleContext bundleContext,
                               @Nonnull JavassistClassPoolManager classPoolManager )
    {
        super( bundleContext, classPoolManager );
    }

    @Override
    protected void weave( @Nonnull WovenClass wovenClass,
                          @Nonnull CtClass ctClass,
                          @Nonnull Map<String, String> dynamicImports )
            throws NotFoundException, CannotCompileException, ClassNotFoundException
    {
        for( CtMethod ctMethod : ctClass.getDeclaredMethods() )
        {
            String ctMethodLongName = ctMethod.getName() + Descriptor.toString( ctMethod.getSignature() );

            if( ctMethod.hasAnnotation( Measure.class ) )
            {
                // add 'start timer' code
                String beforeCode = String.format(
                        "{                                                                                                          \n" +
                        "   MetricsTimer timer = MetricsSpi.getTimer( %s.class, \"%s\" );                                           \n" +
                        "   if( timer != null )                                                                                     \n" +
                        "   {                                                                                                       \n" +
                        "      timer.startTimer();                                                                                  \n" +
                        "   }                                                                                                       \n" +
                        "}                                                                                                          \n",
                        ctMethod.getDeclaringClass().getName(), ctMethodLongName
                );
                beforeCode = beforeCode.replace( "MetricsTimer", Module.MetricsTimer.class.getName() );
                beforeCode = beforeCode.replace( "MetricsSpi", MetricsSpi.class.getName() );
                ctMethod.insertBefore( beforeCode );

                // add 'stop timer' code
                String afterCode = String.format(
                        "{                                                                                                          \n" +
                        "   MetricsTimer timer = MetricsSpi.getTimer( %s.class, \"%s\" );                                           \n" +
                        "   if( timer != null )                                                                                     \n" +
                        "   {                                                                                                       \n" +
                        "      timer.stopTimer();                                                                                   \n" +
                        "   }                                                                                                       \n" +
                        "}                                                                                                          \n",
                        ctMethod.getDeclaringClass().getName(), ctMethodLongName
                );
                afterCode = afterCode.replace( "MetricsTimer", Module.MetricsTimer.class.getName() );
                afterCode = afterCode.replace( "MetricsSpi", MetricsSpi.class.getName() );
                ctMethod.insertAfter( afterCode, true );

                // import required packages
                dynamicImports.put( Module.MetricsTimer.class.getPackage().getName(), "[1,2)" );
                dynamicImports.put( MetricsSpi.class.getPackage().getName(), "[1,2)" );
            }
        }
    }
}
