package org.mosaic.cms;

import java.util.Collection;
import org.mosaic.cms.support.Named;
import org.mosaic.cms.support.Secured;
import org.mosaic.util.collection.MapAccessor;

/**
 * @author arik
 */
public interface DataProvider extends Named, Secured
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
