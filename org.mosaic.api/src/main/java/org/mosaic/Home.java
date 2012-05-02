package org.mosaic;

import java.nio.file.Path;

/**
 * @author arik
 */
public interface Home
{

    @SuppressWarnings( "UnusedDeclaration" )
    Path getHome();

    @SuppressWarnings( "UnusedDeclaration" )
    Path getBoot();

    Path getEtc();

    Path getWork();

}
