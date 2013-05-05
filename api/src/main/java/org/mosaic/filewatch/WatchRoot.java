package org.mosaic.filewatch;

import java.nio.file.Path;
import javax.annotation.Nonnull;
import org.mosaic.Server;

/**
 * @author arik
 */
public enum WatchRoot
{
    HOME,
    APPS,
    ETC,
    LIB,
    WORK;

    public Path getPath( @Nonnull Server server )
    {
        switch( this )
        {
            case APPS:
                return server.getApps();
            case ETC:
                return server.getEtc();
            case LIB:
                return server.getLib();
            case WORK:
                return server.getWork();
            default:
                return server.getHome();
        }
    }
}
