package org.mosaic.launcher;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
final class Header
{
    private static final Logger LOG = LoggerFactory.getLogger( Header.class );

    public static void printHeader( @Nonnull Mosaic mosaic )
    {
        // load logo
        List<String> logoLines;
        try
        {
            try( Reader reader = new InputStreamReader( Header.class.getResource( "logo.txt" ).openStream(), "UTF-8" ) )
            {
                StringBuilder buffer = new StringBuilder( 10000 );

                char[] chars = new char[ 1024 * 8 ];
                int read = reader.read( chars );
                while( read != -1 )
                {
                    buffer.append( chars, 0, read );
                    read = reader.read( chars );
                }

                List<Object> lines = Collections.list( new StringTokenizer( buffer.toString(), "\n", false ) );

                logoLines = new LinkedList<>();
                for( Object line : lines )
                {
                    logoLines.add( line.toString() );
                }
            }
        }
        catch( IOException e )
        {
            throw SystemError.bootstrapError( "Incomplete Mosaic installation - could not read from 'logo.txt' file.", e );
        }

        // create header
        List<String> infoLines = asList(
                "Mosaic server, version...{}",
                "Development mode.........{}",
                "-----------------------------------------------------------------",
                "Home.....................{}",
                "Apps.....................{}",
                "Boot.....................{}",
                "Configurations (etc).....{}",
                "Bundles (lib)............{}",
                "Logs.....................{}",
                "Work.....................{}"
        );

        // unify header and logo lines
        StringBuilder buffer = new StringBuilder( 5000 );
        buffer.append( "\n\n****************************************************************************************************************\n\n" );

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
        buffer.append( "\n****************************************************************************************************************\n" );

        LOG.warn( buffer.toString(),
                  mosaic.getConfiguration().getVersion(),
                  mosaic.getConfiguration().isDevMode() ? "ON" : "off",
                  mosaic.getConfiguration().getHome(),
                  mosaic.getConfiguration().getApps(),
                  mosaic.getConfiguration().getBoot(),
                  mosaic.getConfiguration().getEtc(),
                  mosaic.getConfiguration().getLib(),
                  mosaic.getConfiguration().getLogs(),
                  mosaic.getConfiguration().getWork() );
    }

    private Header()
    {
    }
}
