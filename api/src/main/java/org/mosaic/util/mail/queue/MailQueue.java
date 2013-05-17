package org.mosaic.util.mail.queue;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface MailQueue
{
    void sendMessage( @Nonnull StoredMailMessage message );
}
