package org.mosaic.util.mail;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface MailManager
{
    @Nonnull
    InitialMailMessage createMailMessage( @Nonnull Object id );

    interface InitialMailMessage
    {
        @Nonnull
        OriginatingMailMessage from( @Nonnull String address );
    }

    interface OriginatingMailMessage
    {
        @Nonnull
        AddressedMailMessage to( @Nonnull String... addresses );

        AddressedMailMessage to( @Nonnull Collection<String> addresses );
    }

    interface AddressedMailMessage
    {
        @Nonnull
        AddressedMailMessage cc( String... addresses );

        @Nonnull
        AddressedMailMessage bcc( String... addresses );

        @Nonnull
        AddressedMailMessage withSubject( @Nonnull String subject );

        @Nonnull
        AddressedMailMessage withContext( @Nonnull Map<String, Object> context );

        @Nonnull
        AddressedMailMessage withContext( @Nonnull String key, @Nonnull Object value );

        @Nonnull
        PopulatedMailMessage withTextContent( @Nonnull String text );

        @Nonnull
        PopulatedMailMessage withHtmlContent( @Nonnull String html );
    }

    interface PopulatedMailMessage
    {
        @Nonnull
        PopulatedMailMessage withAttachment( @Nonnull String id, @Nonnull Path resource );

        @Nonnull
        PopulatedMailMessage withAttachments( Attachment... attachment );

        @Nonnull
        PopulatedMailMessage withAttachment( @Nonnull String id, @Nonnull URL resource );

        @Nonnull
        PopulatedMailMessage withAttachment( @Nonnull String id, @Nonnull InputStream inputStream );

        void send();
    }
}
