package org.mosaic.security.localusers.impl;

import javax.annotation.Nonnull;
import org.apache.commons.codec.digest.DigestUtils;
import org.mosaic.security.credentials.Password;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class PasswordImpl implements Password
{
    private static final Logger LOG = LoggerFactory.getLogger( PasswordImpl.class );

    @Nonnull
    private final char[] password;

    @Nonnull
    private final PasswordEncryption passwordEncryption;

    public PasswordImpl( @Nonnull String password, @Nonnull PasswordEncryption passwordEncryption )
    {
        this.password = password.toCharArray();
        this.passwordEncryption = passwordEncryption;
    }

    @Nonnull
    @Override
    public char[] getPassword()
    {
        return this.password;
    }

    @Override
    public boolean check( @Nonnull char[] password )
    {
        switch( this.passwordEncryption )
        {
            case MD2:
                password = DigestUtils.md2Hex( new String( password ) ).toCharArray();
                break;
            case MD5:
                password = DigestUtils.md5Hex( new String( password ) ).toCharArray();
                break;
            case SHA1:
                password = DigestUtils.sha1Hex( new String( password ) ).toCharArray();
                break;
            case SHA256:
                password = DigestUtils.sha256Hex( new String( password ) ).toCharArray();
                break;
            case SHA384:
                password = DigestUtils.sha384Hex( new String( password ) ).toCharArray();
                break;
            case SHA512:
                password = DigestUtils.sha512Hex( new String( password ) ).toCharArray();
                break;
            case PLAIN:
                break;
            default:
                LOG.warn( "Unknown password encryption '{}'", this.passwordEncryption );
                return false;
        }
        return new String( this.password ).equals( new String( password ) );
    }
}
