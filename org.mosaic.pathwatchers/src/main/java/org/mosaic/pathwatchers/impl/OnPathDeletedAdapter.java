package org.mosaic.pathwatchers.impl;

import java.nio.file.Path;
import javax.annotation.Nonnull;
import org.mosaic.modules.Adapter;
import org.mosaic.modules.MethodEndpoint;
import org.mosaic.modules.Service;
import org.mosaic.pathwatchers.OnPathDeleted;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.resource.PathWatcher;

/**
 * @author arik
 */
@Adapter( PathWatcher.class )
public class OnPathDeletedAdapter extends AbstractPathWatcherAdapter<OnPathDeleted>
{
    @Service
    public OnPathDeletedAdapter( @Nonnull MethodEndpoint<OnPathDeleted> endpoint )
    {
        super( endpoint );
    }

    @Override
    public void pathDeleted( @Nonnull Path path, @Nonnull MapEx<String, Object> context )
    {
        if( !this.disabled )
        {
            invoke( path, context );
        }
    }
}
