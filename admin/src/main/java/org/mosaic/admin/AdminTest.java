package org.mosaic.admin;

import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.Measure;
import org.mosaic.shell.annotation.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Bean
public class AdminTest
{
    private static final Logger LOG = LoggerFactory.getLogger( AdminTest.class );

    @Command( name = "test" )
    @Measure
    public void testCommand()
    {
        LOG.info( "Test test" );
    }
}
