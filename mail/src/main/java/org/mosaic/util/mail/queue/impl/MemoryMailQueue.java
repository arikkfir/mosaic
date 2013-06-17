package org.mosaic.util.mail.queue.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.Configurable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.mail.MailAddress;
import org.mosaic.util.mail.queue.MailQueue;
import org.mosaic.util.mail.queue.StoredMailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Bean
public class MemoryMailQueue implements MailQueue, Runnable
{
    private static final Logger LOG = LoggerFactory.getLogger( MemoryMailQueue.class );

    @Nonnull
    private final Deque<StoredMailMessage> messages = new ConcurrentLinkedDeque<>();

    @Nullable
    private ScheduledExecutorService executor;

    @Nonnull
    private Properties smtpConfiguration = new Properties();

    @Configurable("mail")
    public void configureSmtp( @Nonnull MapEx<String, String> cfg )
    {
        Properties props = new Properties();
        props.putAll( cfg );
        this.smtpConfiguration = props;
    }

    @Override
    public void sendMessage( @Nonnull StoredMailMessage message )
    {
        this.messages.offer( message );
    }

    @PostConstruct
    public void init()
    {
        this.executor = Executors.newSingleThreadScheduledExecutor( new ThreadFactoryBuilder()
                                                                            .setPriority( Thread.MIN_PRIORITY )
                                                                            .setNameFormat( "memory-mail-queue-%d" )
                                                                            .setDaemon( true )
                                                                            .build() );
        this.executor.scheduleWithFixedDelay( this, 0, 1, TimeUnit.SECONDS );
    }

    @PreDestroy
    public void destroy()
    {
        ScheduledExecutorService executor = this.executor;
        if( executor != null )
        {
            executor.shutdown();
            this.executor = null;
        }
    }

    @Override
    public void run()
    {
        StoredMailMessage message = this.messages.poll();
        if( message != null )
        {
            try
            {
                MimeMessage msg = new MimeMessage( Session.getInstance( this.smtpConfiguration, null ) );
                msg.setFrom( new InternetAddress( message.getFrom().getAddress(), message.getFrom().getName() ) );
                setReceipients( msg, Message.RecipientType.TO, message.getTo() );
                setReceipients( msg, Message.RecipientType.CC, message.getCc() );
                setReceipients( msg, Message.RecipientType.BCC, message.getBcc() );
                msg.setSubject( message.getSubject() );
                msg.setSentDate( new Date() );
                msg.setText( message.getContents(), "html" );
                msg.saveChanges();
                Transport.send( msg );
            }
            catch( Exception e )
            {
                LOG.error( "Could not send mail message '{}' with details:\n" +
                           "    From: {}\n" +
                           "      To: {}\n" +
                           "      cc: {}\n" +
                           "     bcc: {}\n" +
                           " Subject: {}\n" +
                           "Error message was: {}",
                           message.getId(),
                           message.getFrom(),
                           message.getTo(),
                           message.getCc(),
                           message.getBcc(),
                           message.getSubject(),
                           e.getMessage(),
                           e );
            }
        }
    }

    private void setReceipients( @Nonnull MimeMessage msg,
                                 @Nonnull Message.RecipientType recipientType,
                                 @Nullable List<MailAddress> addresses )
            throws UnsupportedEncodingException, MessagingException
    {
        if( addresses != null )
        {
            InternetAddress[] internetAddresses = new InternetAddress[ addresses.size() ];
            for( int i = 0; i < addresses.size(); i++ )
            {
                MailAddress addr = addresses.get( i );
                internetAddresses[ i ] = new InternetAddress( addr.getAddress(), addr.getName() );
            }
            msg.setRecipients( recipientType, internetAddresses );
        }
    }
}
