package org.mosaic.admin.impl;

import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.mosaic.util.logging.Trace;
import org.mosaic.web.HttpRequest;
import org.mosaic.web.handler.InterceptorChain;
import org.mosaic.web.handler.annotation.Controller;
import org.mosaic.web.handler.annotation.Get;
import org.mosaic.web.handler.annotation.Interceptor;
import org.mosaic.web.handler.annotation.Service;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class AdminAgent
{
    private static final Logger LOG = LoggerFactory.getLogger( AdminAgent.class );

    public static class Person
    {
        public String firstName;

        public String lastName;

        public String type;

        public Person()
        {
        }

        public Person( String firstName, String lastName, String type )
        {
            this.firstName = firstName;
            this.lastName = lastName;
            this.type = type;
        }
    }

    @Get
    @Service( "/services/arik" )
    @Trace
    public Person myService( HttpRequest request )
    {
        return new Person( "Arik", "Kfir", "Service" );
    }

    @Get
    @Controller( "/controllers/arik" )
    @Trace
    public Person myController( HttpRequest request )
    {
        return new Person( "Arik", "Kfir", "Controller" );
    }

    @Get
    @Interceptor( "/controllers/arik" )
    public Object interceptMe( HttpRequest request, InterceptorChain chain ) throws Exception
    {
        Object result = chain.next();
        LOG.info( "Handler returned: {}", result );
        return chain.next();
    }
}
