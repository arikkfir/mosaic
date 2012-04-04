package org.mosaic.server.boot.impl.publish.spring;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.osgi.util.BundleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerResolver;
import org.springframework.core.io.UrlResource;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static org.springframework.core.io.support.PropertiesLoaderUtils.loadProperties;

/**
 * @author arik
 */
public class OsgiSpringNamespacePlugin implements EntityResolver, NamespaceHandlerResolver, SynchronousBundleListener {

    private static final Logger LOG = LoggerFactory.getLogger( OsgiSpringNamespacePlugin.class );

    private final BundleContext bundleContext;

    private final Map<Long, Properties> springSchemasCache = new ConcurrentHashMap<>();

    private final Map<Long, Properties> springHandlersCache = new ConcurrentHashMap<>();

    public OsgiSpringNamespacePlugin( BundleContext bundleContext ) {
        this.bundleContext = bundleContext;

        Bundle[] bundles = this.bundleContext.getBundles();
        if( bundles != null ) {
            for( Bundle bundle : bundles ) {
                if( bundle.getState() == Bundle.RESOLVED || bundle.getState() == Bundle.ACTIVE ) {
                    processResolvedBundle( bundle );
                }
            }
        }
    }

    @Override
    public void bundleChanged( BundleEvent event ) {
        Bundle bundle = event.getBundle();
        if( event.getType() == BundleEvent.RESOLVED ) {

            processResolvedBundle( bundle );

        } else if( event.getType() == BundleEvent.UNRESOLVED ) {

            processUnresolvedBundles( bundle );

        }
    }

    @Override
    public NamespaceHandler resolve( String namespaceUri ) {

        for( Map.Entry<Long, Properties> entry : this.springHandlersCache.entrySet() ) {

            String handlerClassName = entry.getValue().getProperty( namespaceUri );
            if( handlerClassName != null ) {
                Bundle bundle = this.bundleContext.getBundle( entry.getKey() );
                if( bundle != null ) {

                    try {
                        Class<?> handlerClass = bundle.loadClass( handlerClassName );
                        if( !NamespaceHandler.class.isAssignableFrom( handlerClass ) ) {
                            throw new FatalBeanException( "Class [" + handlerClassName + "] for namespace [" + namespaceUri + "] does not implement the [" + NamespaceHandler.class.getName() + "] interface" );
                        }

                        NamespaceHandler namespaceHandler = ( NamespaceHandler ) BeanUtils.instantiateClass( handlerClass );
                        namespaceHandler.init();
                        return namespaceHandler;

                    } catch( ClassNotFoundException e ) {
                        throw new FatalBeanException( "Could not load namespace handler '" + handlerClassName + "' from bundle '" + bundle + "': " + e.getMessage(), e );
                    }

                }
            }

        }
        return null;
    }

    @Override
    public InputSource resolveEntity( String publicId, String systemId ) throws SAXException, IOException {
        if( systemId != null ) {

            for( Map.Entry<Long, Properties> entry : this.springSchemasCache.entrySet() ) {

                String schemaLocation = entry.getValue().getProperty( systemId );
                if( schemaLocation != null ) {
                    Bundle bundle = this.bundleContext.getBundle( entry.getKey() );
                    if( bundle != null ) {

                        // we use 'getResource' and not 'getEntry' here since this is the bundle that registered the
                        // schemas file - we don't care if it doesn't actually contain the schema itself, it has it in
                        // its package imports otherwise it wouldn't declare it (I think....?)
                        URL schemaUrl = bundle.getResource( schemaLocation );
                        if( schemaUrl != null ) {
                            InputSource source = new InputSource( schemaUrl.openStream() );
                            source.setPublicId( publicId );
                            source.setSystemId( systemId );
                            return source;
                        }

                    }
                }

            }

        }
        return null;
    }

    private void processResolvedBundle( Bundle bundle ) {
        URL springSchemasFile = bundle.getEntry( "META-INF/spring.schemas" );
        if( springSchemasFile != null ) {
            try {

                this.springSchemasCache.put( bundle.getBundleId(), loadProperties( new UrlResource( springSchemasFile ) ) );

            } catch( IOException e ) {

                LOG.warn( "Could not load 'spring.schemas' file from bundle '{}': {}", BundleUtils.toString( bundle ), e.getMessage(), e );

            }
        }

        URL springHandlersFile = bundle.getEntry( "META-INF/spring.handlers" );
        if( springHandlersFile != null ) {
            try {

                this.springHandlersCache.put( bundle.getBundleId(), loadProperties( new UrlResource( springHandlersFile ) ) );

            } catch( IOException e ) {

                LOG.warn( "Could not load 'spring.handlers' file from bundle '{}': {}", BundleUtils.toString( bundle ), e.getMessage(), e );

            }
        }
    }

    private void processUnresolvedBundles( Bundle bundle ) {
        this.springSchemasCache.remove( bundle.getBundleId() );
        this.springHandlersCache.remove( bundle.getBundleId() );
    }

}
