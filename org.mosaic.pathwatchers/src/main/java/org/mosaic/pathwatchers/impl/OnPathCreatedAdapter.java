package org.mosaic.pathwatchers.impl;

import java.nio.file.Path;
import javax.annotation.Nonnull;
import org.mosaic.modules.Adapter;
import org.mosaic.modules.MethodEndpoint;
import org.mosaic.modules.Service;
import org.mosaic.pathwatchers.OnPathCreated;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.resource.PathWatcher;

/**
 * @author arik
 */
@Adapter( PathWatcher.class )
public class OnPathCreatedAdapter extends AbstractPathWatcherAdapter<OnPathCreated>
{
    @Service
    public OnPathCreatedAdapter( @Nonnull MethodEndpoint<OnPathCreated> endpoint )
    {
        super( endpoint );
    }

    @Override
    public void pathCreated( @Nonnull Path path, @Nonnull MapEx<String, Object> context )
    {
        if( !this.disabled )
        {
            invoke( path, context );
        }
    }
}
