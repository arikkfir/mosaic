package org.mosaic.event.impl;

import java.util.Dictionary;
import java.util.Hashtable;
import javax.annotation.Nonnull;
import org.mosaic.event.EventAdmin;
import org.mosaic.event.EventProperty;
import org.mosaic.modules.Service;
import org.osgi.service.event.Event;

/**
 * @author arik
 */
@Service
public final class EventAdminImpl implements EventAdmin
{
    @Service
    @Nonnull
    private org.osgi.service.event.EventAdmin eventAdmin;

    @Override
    public final void postEvent( @Nonnull Object event, @Nonnull EventProperty... properties )
    {
        this.eventAdmin.postEvent( createOsgiEvent( event, properties ) );
    }

    @Override
    public final void sendEvent( @Nonnull Object event, @Nonnull EventProperty... properties )
    {
        this.eventAdmin.sendEvent( createOsgiEvent( event, properties ) );
    }

    private Event createOsgiEvent( @Nonnull Object event, @Nonnull EventProperty[] properties )
    {
        Dictionary<String, Object> eventProperties = new Hashtable<>( properties.length );
        eventProperties.put( "mosaicEvent", event );

        for( EventProperty property : properties )
        {
            eventProperties.put( property.getKey(), property.getValue() );
        }

        return new Event( event.getClass().getName().replace( '.', '/' ), eventProperties );
    }
}
