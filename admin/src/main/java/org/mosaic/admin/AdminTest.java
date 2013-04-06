package org.mosaic.admin;

import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.Measure;
import org.mosaic.shell.annotation.Command;

/**
 * @author arik
 */
@Bean
public class AdminTest
{
    @Command( name = "test" )
    @Measure
    public void testCommand()
    {
        System.out.println( "Test test" );
    }
}
