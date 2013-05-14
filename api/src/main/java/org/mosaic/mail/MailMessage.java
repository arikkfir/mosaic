package org.mosaic.mail;

import java.util.Collection;
import javax.annotation.Nonnull;
import org.mosaic.web.net.MediaType;

/**
 * @author arik
 */
public interface MailMessage
{
    int getId();

    @Nonnull
    String getMessageId();

    @Nonnull
    MailAddress getFrom();

    @Nonnull
    Collection<MailAddress> getTo();

    @Nonnull
    Collection<MailAddress> getCc();

    @Nonnull
    Collection<MailAddress> getBcc();

    @Nonnull
    String getSubject();

    @Nonnull
    MediaType getContentType();

    @Nonnull
    String getContents();

    @Nonnull
    Collection<Attachment> getAttachments();
}
