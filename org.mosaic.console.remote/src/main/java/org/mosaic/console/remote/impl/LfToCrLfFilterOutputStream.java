package org.mosaic.console.remote.impl;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

final class LfToCrLfFilterOutputStream extends FilterOutputStream
{
    private boolean lastWasCr;

    LfToCrLfFilterOutputStream( OutputStream out )
    {
        super( out );
    }

    @Override
    public final void write( int b ) throws IOException
    {
        if( this.lastWasCr || b != '\n' )
        {
            this.out.write( b );
        }
        else
        {
            this.out.write( '\r' );
            this.out.write( '\n' );
        }
        this.lastWasCr = b == '\r';
    }
}
