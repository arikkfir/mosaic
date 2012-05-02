package org.mosaic.server.shell.impl.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author arik
 */
public class LfToCrLfFilterOutputStream extends FilterOutputStream
{

    private boolean lastWasCr;

    public LfToCrLfFilterOutputStream( OutputStream out )
    {
        super( out );
    }

    @Override
    public void write( int b ) throws IOException
    {
        if( !lastWasCr && b == '\n' )
        {
            out.write( '\r' );
            out.write( '\n' );
        }
        else
        {
            out.write( b );
        }
        lastWasCr = b == '\r';
    }

}
