package org.mosaic.util.mail.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.annotation.BeanRef;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.mosaic.util.mail.MailManager;
import org.mosaic.util.mail.queue.MailQueue;
import org.mosaic.util.mail.queue.impl.MemoryMailQueue;

/**
 * @author arik
 */
@Service( MailManager.class )
public class MailManagerImpl implements MailManager
{
    @Nonnull
    private MemoryMailQueue memoryMailQueue;

    @Nullable
    private MailQueue queueService;

    @BeanRef
    public void setMemoryMailQueue( @Nonnull MemoryMailQueue memoryMailQueue )
    {
        this.memoryMailQueue = memoryMailQueue;
    }

    @ServiceRef( required = false )
    public void setQueueService( @Nullable MailQueue queueService )
    {
        this.queueService = queueService;
    }

    @Nonnull
    @Override
    public InitialMailMessage createMailMessage( @Nonnull Object id )
    {
        return new MailMessageImpl( this.queueService == null ? this.memoryMailQueue : this.queueService, id );
    }
}
