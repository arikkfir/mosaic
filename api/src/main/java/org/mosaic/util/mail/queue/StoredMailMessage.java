package org.mosaic.util.mail.queue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.mosaic.util.mail.Attachment;
import org.mosaic.util.mail.MailAddress;
import org.mosaic.web.net.MediaType;

/**
 * @author arik
 */
public class StoredMailMessage
{
    @Nonnull
    private final Object id;

    @Nonnull
    private final MailAddress from;

    @Nonnull
    private final List<MailAddress> to;

    @Nonnull
    private final List<MailAddress> cc;

    @Nonnull
    private final List<MailAddress> bcc;

    @Nonnull
    private final String subject;

    @Nonnull
    private final MediaType contentType;

    @Nonnull
    private final String contents;

    @Nonnull
    private final List<Attachment> attachments;

    @Nonnull
    private final Map<String, Object> context;

    public StoredMailMessage( @Nonnull Object id,
                              @Nonnull MailAddress from,
                              @Nonnull List<MailAddress> to,
                              @Nonnull List<MailAddress> cc,
                              @Nonnull List<MailAddress> bcc,
                              @Nonnull String subject,
                              @Nonnull MediaType contentType,
                              @Nonnull String contents,
                              @Nonnull List<Attachment> attachments,
                              @Nonnull Map<String, Object> context )
    {
        this.id = id;
        this.from = from;
        this.to = to;
        this.cc = cc;
        this.bcc = bcc;
        this.subject = subject;
        this.contentType = contentType;
        this.contents = contents;
        this.attachments = attachments;
        this.context = context;
    }

    @Nonnull
    public Object getId()
    {
        return this.id;
    }

    @Nonnull
    public MailAddress getFrom()
    {
        return this.from;
    }

    @Nonnull
    public List<MailAddress> getTo()
    {
        return this.to;
    }

    @Nonnull
    public List<MailAddress> getCc()
    {
        return this.cc;
    }

    @Nonnull
    public List<MailAddress> getBcc()
    {
        return this.bcc;
    }

    @Nonnull
    public String getSubject()
    {
        return this.subject;
    }

    @Nonnull
    public MediaType getContentType()
    {
        return this.contentType;
    }

    @Nonnull
    public String getContents()
    {
        return this.contents;
    }

    @Nonnull
    public Collection<Attachment> getAttachments()
    {
        return this.attachments;
    }

    @Nonnull
    public Map<String, Object> getContext()
    {
        return this.context;
    }
}
