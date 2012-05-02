/**
 * Copyright (c) 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.mosaic.server.shell.impl.auth;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import org.apache.mina.util.Base64;

/**
 * Copied from Eclipse git bundle to enable Mosaic SSH shell. Many thanks to the bright people, Gunnar in particular as
 * the author of this class, for providing this under the Eclipse license.
 *
 * @author Gunnar Wagenknecht
 */
public class AuthorizedKeys
{

    public static enum KeyType
    {
        RSA, DSA
    }

    public static class ParseKeyException extends IOException
    {

        public ParseKeyException( final String message, final Throwable cause )
        {
            super( message, cause );
        }

    }

    private static final String PREFIX_KEY_TYPE = "ssh-";

    private static final String PREFIX_KEY_TYPE_DSA = "ssh-dsa ";

    private static final String PREFIX_KEY_TYPE_RSA = "ssh-rsa ";

    private static final String NEWLINE = "\n";

    private static byte[] asBytes( final String string )
    {
        try
        {
            return string.getBytes( "UTF-8" );
        }
        catch( final UnsupportedEncodingException e )
        {
            return string.getBytes( );
        }
    }

    private static String asString( final byte[] bytes )
    {
        try
        {
            return new String( bytes, "UTF-8" );
        }
        catch( final UnsupportedEncodingException e )
        {
            return new String( bytes );
        }
    }

    private final List<PublicKey> keys;

    public AuthorizedKeys( Path authorizedKeysFile ) throws IOException
    {

        List<PublicKey> keys = new ArrayList<>( );
        try( Scanner scanner = new Scanner( authorizedKeysFile ) )
        {

            // configure to read line by line
            scanner.useDelimiter( NEWLINE );

            // read each line, corresponding to a key
            int lineNumber = 0;
            while( scanner.hasNext( ) )
            {
                lineNumber++;

                // get line (without leading and trailing blanks)
                String line = scanner.next( ).trim( );

                // ignore blank line and comments
                if( ( line.length( ) == 0 ) || ( line.charAt( 0 ) == '#' ) )
                {
                    continue;
                }

                // read key
                try
                {
                    keys.add( readPublicKey( line ) );
                }
                catch( final Exception e )
                {
                    throw new ParseKeyException( "Line " + lineNumber + ": " + e.getMessage( ), e );
                }
            }

        }
        this.keys = Collections.unmodifiableList( keys );
    }

    public List<PublicKey> getKeys( )
    {
        return keys;
    }

    private BigInteger readBigInteger( ByteBuffer buffer )
    {
        byte[] bytes = new byte[ buffer.getInt( ) ];
        buffer.get( bytes );
        return new BigInteger( bytes );
    }

    private PublicKey readPublicKey( String line ) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException
    {
        // [options] <type> <base64> <comment>
        KeyType type;
        byte[] key;

        // skip options (if any)
        if( !line.startsWith( PREFIX_KEY_TYPE ) )
        {
            final int keyTypeStart = line.indexOf( PREFIX_KEY_TYPE );
            if( keyTypeStart == -1 )
            {
                throw new IOException( "missing key type" );
            }
            line = line.substring( keyTypeStart );
        }

        // key type
        if( line.startsWith( PREFIX_KEY_TYPE_DSA ) )
        {
            line = line.substring( PREFIX_KEY_TYPE_DSA.length( ) );
            type = KeyType.DSA;
        }
        else if( line.startsWith( PREFIX_KEY_TYPE_RSA ) )
        {
            line = line.substring( PREFIX_KEY_TYPE_RSA.length( ) );
            type = KeyType.RSA;
        }
        else
        {
            throw new IOException( "unsupported key type" );
        }

        // key
        int keyEndIdx = line.indexOf( ' ' );
        if( keyEndIdx != -1 )
        {
            key = Base64.decodeBase64( asBytes( line.substring( 0, keyEndIdx ) ) );
        }
        else
        {
            key = Base64.decodeBase64( asBytes( line ) );
        }

        // wrap key into byte buffer
        ByteBuffer buffer = ByteBuffer.wrap( key );

        // skip key type
        readString( buffer );

        // parse key
        switch( type )
        {
            case RSA:
                // exponent + modulus
                BigInteger pubExp = readBigInteger( buffer );
                BigInteger mod = readBigInteger( buffer );
                return KeyFactory.getInstance( KeyType.RSA.name( ) ).generatePublic( new RSAPublicKeySpec( mod, pubExp ) );
            case DSA:
                // p + q+ g + y
                BigInteger p = readBigInteger( buffer );
                BigInteger q = readBigInteger( buffer );
                BigInteger g = readBigInteger( buffer );
                BigInteger y = readBigInteger( buffer );
                return KeyFactory.getInstance( KeyType.DSA.name( ) ).generatePublic( new DSAPublicKeySpec( y, p, q, g ) );
            default:
                throw new IOException( "not implemented: " + type );
        }
    }

    private String readString( ByteBuffer buffer )
    {
        final int len = buffer.getInt( );
        final byte[] bytes = new byte[ len ];
        buffer.get( bytes );
        return asString( bytes );
    }
}
