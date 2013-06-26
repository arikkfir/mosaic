package org.mosaic.admin;

import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.Measure;
import org.mosaic.shell.annotation.Command;
import org.mosaic.web.handler.annotation.Controller;
import org.mosaic.web.handler.annotation.WebAppFilter;
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

    @Controller( "/" )
    @WebAppFilter( "application.name=='test'" )
    public void testController()
    {
        System.out.println( "hello from test webapp" );
    }
}
