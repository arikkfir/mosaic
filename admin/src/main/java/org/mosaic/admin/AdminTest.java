package org.mosaic.admin;

import java.io.IOException;
import javax.annotation.Nonnull;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.Measure;
import org.mosaic.shell.annotation.Command;
import org.mosaic.web.handler.annotation.Controller;
import org.mosaic.web.handler.annotation.WebAppFilter;
import org.mosaic.web.request.WebResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Bean
public class AdminTest
{
    private static final Logger LOG = LoggerFactory.getLogger( AdminTest.class );

    @Command(name = "test")
    @Measure
    public void testCommand()
    {
        LOG.info( "Test test" );
    }

    @Controller( "/a" )
    @WebAppFilter( "application.name=='test'" )
    public void a( @Nonnull WebResponse response ) throws IOException
    {
        response.getCharacterBody().append( "Hello controller 'a'!" );
    }

    @Controller("/b")
    @WebAppFilter("application.name=='test'")
    public void b( @Nonnull WebResponse response ) throws IOException
    {
        response.getCharacterBody().append( "Hello controller 'b'!" );
    }
}
