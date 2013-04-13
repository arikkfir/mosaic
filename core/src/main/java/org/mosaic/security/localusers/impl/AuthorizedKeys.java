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
package org.mosaic.security.localusers.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.mina.util.Base64;
import org.mosaic.security.credentials.PublicKeys;

/**
 * Copied from Eclipse git bundle to enable Rinku SSH shell. Many thanks to the bright people, Gunnar in particular as
 * the author of this class, for providing this under the Eclipse license.
 *
 * @author Gunnar Wagenknecht
 */
public class AuthorizedKeys implements PublicKeys
{
    private static final String PREFIX_KEY_TYPE = "ssh-";

    private static final String PREFIX_KEY_TYPE_DSA = "ssh-dsa ";

    private static final String PREFIX_KEY_TYPE_RSA = "ssh-rsa ";

    private static final String NEWLINE = "\n";

    private static byte[] asBytes( @Nonnull String string )
    {
        try
        {
            return string.getBytes( "UTF-8" );
        }
        catch( final UnsupportedEncodingException e )
        {
            return string.getBytes();
        }
    }

    private static String asString( @Nonnull byte[] bytes )
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

    public static class ParseKeyException extends IOException
    {
        public ParseKeyException( @Nonnull String message, @Nullable Throwable cause )
        {
            super( message, cause );
        }
    }

    @Nonnull
    private final List<PublicKey> keys;

    public AuthorizedKeys( @Nonnull String authorizedPublicKeys ) throws IOException
    {
        List<PublicKey> keys = new ArrayList<>();
        try( Scanner scanner = new Scanner( authorizedPublicKeys ) )
        {
            // configure to read line by line
            scanner.useDelimiter( NEWLINE );

            // read each line, corresponding to a key
            int lineNumber = 0;
            while( scanner.hasNext() )
            {
                lineNumber++;

                // get line (without leading and trailing blanks)
                String line = scanner.next().trim();

                // ignore blank line and comments
                if( ( line.length() == 0 ) || ( line.charAt( 0 ) == '#' ) )
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
                    throw new ParseKeyException( "Line " + lineNumber + ": " + e.getMessage(), e );
                }
            }
        }
        this.keys = Collections.unmodifiableList( keys );
    }

    @Override
    public Iterator<PublicKey> iterator()
    {
        return this.keys.iterator();
    }

    @Nonnull
    public List<PublicKey> getKeys()
    {
        return keys;
    }

    @Override
    public boolean authorizedFor( @Nonnull PublicKey key ) throws IOException
    {
        for( PublicKey authorizedKey : this.keys )
        {
            if( isSameKey( authorizedKey, key ) )
            {
                return true;
            }
        }
        return false;
    }

    private boolean isSameKey( PublicKey k1, PublicKey k2 ) throws IOException
    {
        if( ( k1 instanceof DSAPublicKey ) && ( k2 instanceof DSAPublicKey ) )
        {
            return isSameDSAKey( ( DSAPublicKey ) k1, ( DSAPublicKey ) k2 );
        }
        else if( ( k1 instanceof RSAPublicKey ) && ( k2 instanceof RSAPublicKey ) )
        {
            return isSameRSAKey( ( RSAPublicKey ) k1, ( RSAPublicKey ) k2 );
        }
        else
        {
            throw new IOException( "Unsupported key types detected! (" + k1 + " and " + k2 + ")" );
        }
    }

    private boolean isSameRSAKey( RSAPublicKey k1, RSAPublicKey k2 )
    {
        return k1.getPublicExponent().equals( k2.getPublicExponent() ) && k1.getModulus().equals( k2.getModulus() );
    }

    @SuppressWarnings( "SimplifiableIfStatement" )
    private boolean isSameDSAKey( DSAPublicKey k1, DSAPublicKey k2 )
    {
        if( !k1.getY().equals( k2.getY() ) )
        {
            return false;
        }
        else if( !k1.getParams().getG().equals( k2.getParams().getG() ) )
        {
            return false;
        }
        else if( !k1.getParams().getP().equals( k2.getParams().getP() ) )
        {
            return false;
        }
        else
        {
            return k1.getParams().getQ().equals( k2.getParams().getQ() );
        }
    }

    @Nonnull
    private PublicKey readPublicKey( @Nonnull String line )
            throws IOException, InvalidKeySpecException, NoSuchAlgorithmException
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
            line = line.substring( PREFIX_KEY_TYPE_DSA.length() );
            type = KeyType.DSA;
        }
        else if( line.startsWith( PREFIX_KEY_TYPE_RSA ) )
        {
            line = line.substring( PREFIX_KEY_TYPE_RSA.length() );
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
                return KeyFactory.getInstance( KeyType.RSA.name() ).generatePublic( new RSAPublicKeySpec( mod, pubExp ) );
            case DSA:
                // p + q+ g + y
                BigInteger p = readBigInteger( buffer );
                BigInteger q = readBigInteger( buffer );
                BigInteger g = readBigInteger( buffer );
                BigInteger y = readBigInteger( buffer );
                return KeyFactory.getInstance( KeyType.DSA.name() ).generatePublic( new DSAPublicKeySpec( y, p, q, g ) );
            default:
                throw new IOException( "not implemented: " + type );
        }
    }

    @Nonnull
    private BigInteger readBigInteger( @Nonnull ByteBuffer buffer )
    {
        byte[] bytes = new byte[ buffer.getInt() ];
        buffer.get( bytes );
        return new BigInteger( bytes );
    }

    private String readString( @Nonnull ByteBuffer buffer )
    {
        final int len = buffer.getInt();
        final byte[] bytes = new byte[ len ];
        buffer.get( bytes );
        return asString( bytes );
    }

    public static enum KeyType
    {
        RSA, DSA
    }
}
