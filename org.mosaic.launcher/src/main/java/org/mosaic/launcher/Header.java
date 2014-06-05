package org.mosaic.launcher;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;
import static java.util.Collections.list;
import static java.util.stream.Collectors.toList;

/**
 * @author arik
 */
final class Header
{
    private static final Logger LOG = LoggerFactory.getLogger( Header.class );

    public static void printHeader( Map<String, ?> properties )
    {
        // load logo
        List<String> logoLines;
        try
        {
            try( Reader reader = new InputStreamReader( Header.class.getResource( "/logo.txt" ).openStream(), "UTF-8" ) )
            {
                StringBuilder buffer = new StringBuilder( 10000 );

                char[] chars = new char[ 1024 * 8 ];
                int read = reader.read( chars );
                while( read != -1 )
                {
                    buffer.append( chars, 0, read );
                    read = reader.read( chars );
                }

                logoLines =
                        list( new StringTokenizer( buffer.toString(), "\n", false ) )
                                .stream()
                                .map( Object::toString )
                                .collect( toList() );
            }
        }
        catch( IOException e )
        {
            throw SystemError.bootstrapError( "Incomplete Mosaic installation - could not read from 'logo.txt' file.", e );
        }

        // create header
        List<String> infoLines = asList(
                "Mosaic server version....{}",
                "------------------------------------------------------------------------------",
                "Home.....................{}",
                "Apps.....................{}",
                "Bin......................{}",
                "Etc......................{}",
                "Lib......................{}",
                "Logs.....................{}",
                "Schemas..................{}",
                "Work.....................{}"
        );

        // unify header and logo lines
        StringBuilder buffer = new StringBuilder( 5000 );
        buffer.append( "\n\n********************************************************************************************************************\n\n" );

        int i = 0;
        while( i < logoLines.size() || i < infoLines.size() )
        {
            String logoLine = i < logoLines.size() ? logoLines.get( i ) : "";
            while( logoLine.length() < 34 )
            {
                logoLine += " ";
            }

            String infoLine = i < infoLines.size() ? infoLines.get( i ) : "";
            buffer.append( logoLine ).append( infoLine ).append( "\n" );
            i++;
        }
        buffer.append( "\n********************************************************************************************************************\n" );

        LOG.warn( buffer.toString(),
                  properties.get( "org.mosaic.version" ),
                  properties.get( "org.mosaic.home" ),
                  properties.get( "org.mosaic.home.apps" ),
                  properties.get( "org.mosaic.home.bin" ),
                  properties.get( "org.mosaic.home.etc" ),
                  properties.get( "org.mosaic.home.lib" ),
                  properties.get( "org.mosaic.home.logs" ),
                  properties.get( "org.mosaic.home.schemas" ),
                  properties.get( "org.mosaic.home.work" ) );
    }

    private Header()
    {
    }
}
