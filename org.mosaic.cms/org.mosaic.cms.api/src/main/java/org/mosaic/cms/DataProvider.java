package org.mosaic.cms;

import java.util.Collection;
import org.mosaic.util.collection.MapAccessor;

/**
 * @author arik
 */
public interface DataProvider
{
    interface Parameters extends MapAccessor<String, String>
    {
        String getName();

        Parameters getChild( String name );

        Collection<Parameters> getChildren();

        Collection<Parameters> getChildren( String name );
    }

    String getType();

    boolean isRequired();

    Object getData( Parameters parameters ) throws Exception;
}
