package org.mosaic.server.shell.commands.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.mosaic.lifecycle.ServiceExport;
import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.service.ListenerHook;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
@ServiceExport( ListenerHook.class )
public class InspectionListenerHook implements ListenerHook
{

    private final Map<Bundle, List<String>> requirements = new WeakHashMap<>();

    public List<String> getServiceRequirements( Bundle bundle )
    {
        return this.requirements.get( bundle );
    }

    @Override
    public synchronized void added( Collection<ListenerInfo> listeners )
    {
        for( ListenerInfo info : listeners )
        {
            Bundle bundle = info.getBundleContext().getBundle();

            List<String> reqs = this.requirements.get( bundle );
            if( reqs == null )
            {
                reqs = new CopyOnWriteArrayList<>();
                this.requirements.put( bundle, reqs );
            }

            reqs.add( info.getFilter() );
        }
    }

    @Override
    public synchronized void removed( Collection<ListenerInfo> listeners )
    {
        for( ListenerInfo info : listeners )
        {
            Bundle bundle = info.getBundleContext().getBundle();

            List<String> reqs = this.requirements.get( bundle );
            if( reqs != null )
            {
                reqs.remove( info.getFilter() );
                if( reqs.isEmpty() )
                {
                    this.requirements.remove( bundle );
                }
            }
        }
    }
}
