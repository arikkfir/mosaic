package org.mosaic.lifecycle.impl.util;

import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.DP;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * @author arik
 */
public abstract class ServiceUtils
{
    public static long getId( @Nonnull ServiceReference<?> ref )
    {
        Object idValue = ref.getProperty( Constants.SERVICE_ID );
        if( idValue == null )
        {
            throw new IllegalArgumentException( "Service reference has no service ID property!" );
        }
        return ( ( Number ) idValue ).longValue();
    }

    public static int getRanking( @Nonnull ServiceReference<?> ref )
    {
        Object rankingValue = ref.getProperty( Constants.SERVICE_RANKING );
        if( rankingValue == null )
        {
            return 0;
        }
        else
        {
            return ( ( Number ) rankingValue ).intValue();
        }
    }

    public static Map<String, Object> getProperties( @Nonnull ServiceReference<?> serviceReference )
    {
        Map<String, Object> properties = new HashMap<>();
        for( String property : serviceReference.getPropertyKeys() )
        {
            properties.put( property, serviceReference.getProperty( property ) );
        }
        return properties;
    }

    public static <T> ServiceRegistration<T> register( @Nonnull BundleContext bundleContext,
                                                       @Nonnull Class<T> type,
                                                       @Nonnull T service,
                                                       @Nonnull DP... dictionaryEntries )
    {
        Dictionary<String, Object> dict = null;
        if( dictionaryEntries.length > 0 )
        {
            dict = new Hashtable<>( dictionaryEntries.length );
            for( DP dp : dictionaryEntries )
            {
                dict.put( dp.getKey(), dp.getValue() );
            }
        }
        return bundleContext.registerService( type, service, dict );
    }

    public static ServiceRegistration<?> register( @Nonnull BundleContext bundleContext,
                                                   @Nonnull Object service,
                                                   @Nonnull Collection<DP> dictionaryEntries,
                                                   @Nonnull Class<?>... type )
    {
        String[] classNames = new String[ type.length ];
        for( int i = 0; i < type.length; i++ )
        {
            classNames[ i ] = type[ i ].getName();
        }
        return register( bundleContext, service, dictionaryEntries, classNames );
    }

    public static ServiceRegistration<?> register( @Nonnull BundleContext bundleContext,
                                                   @Nonnull Object service,
                                                   @Nonnull Collection<DP> dictionaryEntries,
                                                   @Nonnull String... classNames )
    {
        Dictionary<String, Object> dict = new Hashtable<>( 5 );
        dict.put( "instanceClass", service.getClass() );
        for( DP dp : dictionaryEntries )
        {
            dict.put( dp.getKey(), dp.getValue() );
        }
        return bundleContext.registerService( classNames, service, dict );
    }

    public static <T> ServiceRegistration<T> update( @Nonnull ServiceRegistration<T> registration,
                                                     @Nonnull DP... dictionaryEntries )
    {
        return update( registration, Arrays.asList( dictionaryEntries ) );
    }

    public static <T> ServiceRegistration<T> update( @Nonnull ServiceRegistration<T> registration,
                                                     @Nonnull Collection<DP> dictionaryEntries )
    {
        Dictionary<String, Object> dict = new Hashtable<>( dictionaryEntries.size() );
        for( DP dp : dictionaryEntries )
        {
            dict.put( dp.getKey(), dp.getValue() );
        }
        registration.setProperties( dict );
        return registration;
    }

    public static <T> ServiceRegistration<T> unregister( @Nullable ServiceRegistration<T> registration )
    {
        if( registration != null )
        {
            try
            {
                registration.unregister();
            }
            catch( Exception ignore )
            {
                // ignore this - does not matter
            }
        }
        return null;
    }
}
