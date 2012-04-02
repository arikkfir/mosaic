package org.mosaic.runner.deploy.lifecycle;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import org.mosaic.runner.util.BundleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class BundleDeployer implements BundleListener {

    private static final String BEAN_ANNOTATION_NAME = "org.mosaic.inject.Bean";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void bundleChanged( BundleEvent event ) {
        Bundle bundle = event.getBundle();
        if( isMosaicBundle( bundle ) ) {

            if( bundle.getHeaders( bundle.getHeaders().get( Constants.BUNDLE_ACTIVATOR ) ) != null ) {

                //
                // mosaic bundles must not have activators
                //
                this.logger.warn( "Bundle '{}-{}[{}]' is a Mosaic bundle, but also has a 'Bundle-Activator' header; Mosaic bundles must have no activator, and therefor will be ignored and treated as a standard bundle." );

            } else if( event.getType() == BundleEvent.INSTALLED ) {

                //
                // track this new mosaic bundle
                //
                startTrackingBundle( event.getBundle() );

            } else if( event.getType() == BundleEvent.UNRESOLVED || event.getType() == BundleEvent.UNINSTALLED ) {

                //
                // stop tracking this mosaic bundle
                //
                stopTrackingBundle( event.getBundle() );

            }
        }
    }

    private void startTrackingBundle( final Bundle bundle ) {
        this.logger.info( "Tracking bundle '{}'", BundleUtils.toString( bundle ) );
        if( 1 == 1 ) {
            return;
        }

        Enumeration<URL> classEntries = bundle.findEntries( "/", "*.class", true );
        if( classEntries == null ) {
            //
            // this bundle has no beans
            //
            return;
        }

        Collection<Class<?>> beanClasses = collectClasses( bundle, BEAN_ANNOTATION_NAME, Collections.list( classEntries ) );
        if( beanClasses.isEmpty() ) {
            //
            // this bundle has no beans
            //
            return;
        }


        Module module = new AbstractModule() {
            @Override
            protected void configure() {

            }
        };
        Injector injector = Guice.createInjector();

        //TODO 3/31/12: track bundle
    }

    private void stopTrackingBundle( Bundle bundle ) {
        this.logger.info( "Un-tracking bundle '{}'", BundleUtils.toString( bundle ) );
        if( 1 == 1 ) {
            return;
        }
        //TODO 3/31/12: untrack bundle
    }

    private boolean isMosaicBundle( Bundle bundle ) {
        return bundle.getHeaders().get( "Mosaic-Bundle" ) != null;
    }

    private Collection<Class<?>> collectClasses( Bundle bundle, String annotationClassName, Collection<URL> classes ) {
        Collection<Class<?>> matches = new LinkedHashSet<>();
        for( URL classUrl : classes ) {
            String path = classUrl.getPath();
            if( path.trim().length() > 0 ) {
                String className = path.replaceAll( "/", "." );
                if( className.startsWith( "/" ) ) {
                    className = className.substring( 1 );
                }
                try {
                    Class<?> cls = bundle.loadClass( className );
                    if( isAnnotationPresent( cls, annotationClassName ) ) {
                        matches.add( cls );
                    }

                } catch( ClassNotFoundException e ) {
                    this.logger.warn( "Could not load class '{}' from bundle '{}': {}",
                                      new Object[] { className, BundleUtils.toString( bundle ), e.getMessage(), e } );
                    return Collections.emptyList();
                }
            }
        }
        return matches;
    }

    private boolean isAnnotationPresent( Class<?> cls, String annotationClassName ) {
        if( cls.isInterface() || Modifier.isAbstract( cls.getModifiers() ) ) {
            return false;
        }

        while( cls != null ) {
            for( Annotation annotation : cls.getAnnotations() ) {
                String annName = annotation.annotationType().getName();
                if( annName.equals( annotationClassName ) ) {
                    return true;
                }
            }
            cls = cls.getSuperclass();
        }
        return false;
    }
}
