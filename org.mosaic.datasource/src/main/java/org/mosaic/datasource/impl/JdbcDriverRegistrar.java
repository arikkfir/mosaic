package org.mosaic.datasource.impl;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import java.net.URL;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.mosaic.event.EventListener;
import org.mosaic.modules.Component;
import org.mosaic.modules.Module;
import org.mosaic.modules.ModuleEvent;
import org.mosaic.modules.ModuleEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Multimaps.synchronizedMultimap;

/**
 * @author arik
 */
@Component
final class JdbcDriverRegistrar
{
    private static final Logger LOG = LoggerFactory.getLogger( JdbcDriverRegistrar.class );

    @Nonnull
    private final Multimap<Long, DriverEntry> drivers = synchronizedMultimap( LinkedListMultimap.<Long, DriverEntry>create( 10 ) );

    @Nonnull
    @Component
    private Module module;

    JdbcDriverRegistrar() throws InstantiationException, IllegalAccessException, SQLException
    {
        this.drivers.put( this.module.getId(), new DriverEntry( org.mariadb.jdbc.Driver.class ) );
    }

    @EventListener
    synchronized void handleModuleEvent( @Nonnull ModuleEvent event )
    {
        if( event.getEventType() == ModuleEventType.ACTIVATED )
        {
            for( URL url : event.getModule().getModuleResources().findResources( "**/*.class" ) )
            {
                String className = url.getPath().replace( "/", "." );
                className = className.substring( 1, className.length() - ".class".length() );
                try
                {
                    Class<?> candidate = event.getModule().getModuleComponents().loadClass( className );
                    if( Driver.class.isAssignableFrom( candidate ) )
                    {
                        this.drivers.put( event.getModule().getId(),
                                          new DriverEntry( candidate.asSubclass( Driver.class ) ) );
                    }
                }
                catch( ClassNotFoundException ignore )
                {
                }
                catch( Exception e )
                {
                    LOG.warn( "Could not instantiate '{}' to register as a JDBC data source driver: {}", className, e.getMessage(), e );
                }
            }
        }
        else
        {
            Collection<DriverEntry> entries = this.drivers.removeAll( event.getModule().getId() );
            if( entries != null )
            {
                for( DriverEntry entry : entries )
                {
                    try
                    {
                        DriverManager.deregisterDriver( entry.driver );
                    }
                    catch( Exception e )
                    {
                        LOG.warn( "[POSSIBLE MEMORY LEAK] Could not unregister JDBC driver '{}': {}", entry.driver, e.getMessage(), e );
                    }
                }
            }
        }
    }

    private class DriverEntry
    {
        @Nonnull
        private final Driver driver;

        private DriverEntry( @Nonnull Class<? extends Driver> driverClass )
                throws IllegalAccessException, InstantiationException, SQLException
        {
            this.driver = driverClass.newInstance();

            DriverManager.registerDriver( this.driver );
        }
    }
}
