package org.mosaic.util.mail.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.internet.AddressException;
import org.mosaic.util.mail.Attachment;
import org.mosaic.util.mail.MailAddress;
import org.mosaic.util.mail.MailManager;
import org.mosaic.util.mail.queue.MailQueue;
import org.mosaic.util.mail.queue.StoredMailMessage;
import org.mosaic.web.net.MediaType;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * @author arik
 */
public class MailMessageImpl implements MailManager.InitialMailMessage,
                                        MailManager.OriginatingMailMessage,
                                        MailManager.AddressedMailMessage,
                                        MailManager.PopulatedMailMessage
{
    @Nonnull
    private final MailQueue queue;

    @Nonnull
    private final Object id;

    @Nullable
    private MailAddress from;

    @Nullable
    private List<MailAddress> to;

    @Nullable
    private List<MailAddress> cc;

    @Nullable
    private List<MailAddress> bcc;

    @Nullable
    private String subject;

    @Nullable
    private Map<String, Object> context;

    @Nullable
    private MediaType contentType;

    @Nullable
    private String content;

    @Nullable
    private List<Attachment> attachments;

    public MailMessageImpl( @Nonnull MailQueue queue, @Nonnull Object id )
    {
        this.queue = queue;
        this.id = id;
    }

    @Nonnull
    @Override
    public MailManager.OriginatingMailMessage from( @Nonnull String from )
    {
        this.from = createMailAddress( from );
        return this;
    }

    @Nonnull
    @Override
    public MailManager.AddressedMailMessage to( @Nonnull String... addresses )
    {
        this.to = createMailAddresses( addresses );
        return this;
    }

    @Override
    public MailManager.AddressedMailMessage to( @Nonnull Collection<String> addresses )
    {
        this.to = createMailAddresses( addresses );
        return this;
    }

    @Nonnull
    @Override
    public MailManager.AddressedMailMessage cc( String... addresses )
    {
        this.cc = createMailAddresses( addresses );
        return this;
    }

    @Nonnull
    @Override
    public MailManager.AddressedMailMessage bcc( String... addresses )
    {
        this.bcc = createMailAddresses( addresses );
        return this;
    }

    @Nonnull
    @Override
    public MailManager.AddressedMailMessage withSubject( @Nonnull String subject )
    {
        this.subject = subject;
        return this;
    }

    @Nonnull
    @Override
    public MailManager.AddressedMailMessage withContext( @Nonnull Map<String, Object> context )
    {
        if( this.context == null )
        {
            this.context = new HashMap<>();
        }
        this.context.putAll( context );
        return this;
    }

    @Nonnull
    @Override
    public MailManager.AddressedMailMessage withContext( @Nonnull String key, @Nonnull Object value )
    {
        if( this.context == null )
        {
            this.context = new HashMap<>();
        }
        this.context.put( key, value );
        return this;
    }

    @Nonnull
    @Override
    public MailManager.PopulatedMailMessage withTextContent( @Nonnull String text )
    {
        this.contentType = MediaType.PLAIN_TEXT;
        this.content = text;
        return this;
    }

    @Nonnull
    @Override
    public MailManager.PopulatedMailMessage withHtmlContent( @Nonnull String html )
    {
        this.contentType = new MediaType( "text", "html" );
        this.content = html;
        return this;
    }

    @Nonnull
    @Override
    public MailManager.PopulatedMailMessage withAttachment( @Nonnull String id, @Nonnull Path resource )
    {
        if( this.attachments == null )
        {
            this.attachments = new LinkedList<>();
        }
        this.attachments.add( new Attachment( id, resource ) );
        return this;
    }

    @Nonnull
    @Override
    public MailManager.PopulatedMailMessage withAttachments( Attachment... attachment )
    {
        if( this.attachments == null )
        {
            this.attachments = new LinkedList<>();
        }
        this.attachments.addAll( asList( attachment ) );
        return this;
    }

    @Nonnull
    @Override
    public MailManager.PopulatedMailMessage withAttachment( @Nonnull String id, @Nonnull URL resource )
    {
        if( this.attachments == null )
        {
            this.attachments = new LinkedList<>();
        }

        Path tempFile;
        try( InputStream is = resource.openStream() )
        {
            tempFile = Files.createTempFile( "mosaic.mail.", id );
            Files.copy( is, tempFile, REPLACE_EXISTING );
            this.attachments.add( new Attachment( id, tempFile ) );
            return this;
        }
        catch( IOException e )
        {
            throw new IllegalStateException( "Could not copy resource '" + resource + "' to a temporary file: " + e.getMessage(), e );
        }
    }

    @Nonnull
    @Override
    public MailManager.PopulatedMailMessage withAttachment( @Nonnull String id, @Nonnull InputStream inputStream )
    {
        if( this.attachments == null )
        {
            this.attachments = new LinkedList<>();
        }

        Path tempFile;
        try
        {
            tempFile = Files.createTempFile( "mosaic.mail.", id );
            Files.copy( inputStream, tempFile, REPLACE_EXISTING );
            this.attachments.add( new Attachment( id, tempFile ) );
            return this;
        }
        catch( IOException e )
        {
            throw new IllegalStateException( "Could not copy resource from given input stream to a temporary file: " + e.getMessage(), e );
        }
    }

    @Override
    public void send()
    {
        if( this.from == null )
        {
            throw new IllegalStateException( "Message \"from\" address has not been set" );
        }
        else if( this.to == null )
        {
            throw new IllegalStateException( "Message \"to\" addresses have not been set" );
        }
        this.queue.sendMessage( new StoredMailMessage( this.id,
                                                       this.from,
                                                       this.to,
                                                       this.cc == null ? Collections.<MailAddress>emptyList() : this.cc,
                                                       this.bcc == null ? Collections.<MailAddress>emptyList() : this.bcc,
                                                       this.subject == null ? "" : this.subject,
                                                       this.contentType == null ? MediaType.PLAIN_TEXT : this.contentType,
                                                       this.content == null ? "" : this.content,
                                                       this.attachments == null ? Collections.<Attachment>emptyList() : this.attachments,
                                                       this.context == null ? Collections.<String, Object>emptyMap() : this.context ) );
    }

    @Nonnull
    private List<MailAddress> createMailAddresses( @Nonnull String... addresses )
    {
        return createMailAddresses( asList( addresses ) );
    }

    @Nonnull
    private List<MailAddress> createMailAddresses( @Nonnull Collection<String> addresses )
    {
        List<MailAddress> mailAddresses = new LinkedList<>();
        for( String addr : addresses )
        {
            mailAddresses.add( createMailAddress( addr ) );
        }
        return unmodifiableList( mailAddresses );
    }

    @Nonnull
    private MailAddressImpl createMailAddress( @Nonnull String addr )
    {
        MailAddressImpl mailAddress;
        try
        {
            mailAddress = new MailAddressImpl( addr );
        }
        catch( AddressException e )
        {
            throw new IllegalArgumentException( "Illegal E-mail address '" + addr + "' (" + e.getMessage() + ")", e );
        }
        return mailAddress;
    }
}
