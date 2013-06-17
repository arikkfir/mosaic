package org.mosaic.util.mail.impl;

import javax.annotation.Nonnull;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.mosaic.util.mail.MailAddress;

/**
 * @author arik
 */
public class MailAddressImpl implements MailAddress
{
    @Nonnull
    private final InternetAddress address;

    MailAddressImpl( @Nonnull String address ) throws AddressException
    {
        this.address = new InternetAddress( address, true );
    }

    @Override
    public String getAddress()
    {
        return this.address.getAddress();
    }

    @Override
    public String getName()
    {
        return this.address.getPersonal();
    }
}
