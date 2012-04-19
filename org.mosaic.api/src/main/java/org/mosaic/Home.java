package org.mosaic;

import java.nio.file.Path;

/**
 * @author arik
 */
public interface Home {

    Path getHome();

    Path getBoot();

    Path getDeploy();

    Path getEtc();

    Path getServer();

    Path getWork();

}
