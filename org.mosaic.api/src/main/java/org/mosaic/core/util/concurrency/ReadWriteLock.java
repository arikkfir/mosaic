package org.mosaic.core.util.concurrency;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public class ReadWriteLock
{
    @Nonnull
    private final String name;

    private final long defaultTimeout;

    @Nonnull
    private final TimeUnit defaultTimeUnit;

    @Nonnull
    private final MosaicReentrantReadWriteLock lock = new MosaicReentrantReadWriteLock( false );

    public ReadWriteLock( @Nonnull String name )
    {
        this( name, 30, TimeUnit.SECONDS );
    }

    public ReadWriteLock( @Nonnull String name, long defaultTimeout, @Nonnull TimeUnit defaultTimeUnit )
    {
        this.name = name;
        this.defaultTimeout = defaultTimeout;
        this.defaultTimeUnit = defaultTimeUnit;
    }

    public void acquireReadLock()
    {
        acquireReadLock( this.defaultTimeout, this.defaultTimeUnit );
    }

    public void acquireReadLock( long timeout, @Nonnull TimeUnit timeUnit )
    {
        try
        {
            if( !this.lock.readLock().tryLock( timeout, timeUnit ) )
            {
                StringBuilder msg = new StringBuilder( 1000 );

                msg.append( "could not acquire read lock '" ).append( this.name ).append( "' for " ).append( timeout ).append( " " ).append( timeUnit ).append( "\n" );
                msg.append( "\n" );

                Thread owner = this.lock.getOwner();
                if( owner == null )
                {
                    msg.append( "No thread holding the write lock is!\n" );
                }
                else
                {
                    msg.append( "Thread holding the write lock is: " ).append( owner.getName() ).append( " [" ).append( owner.getId() ).append( "]\n" );
                    for( StackTraceElement traceElement : owner.getStackTrace() )
                    {
                        msg.append( "\tat " ).append( traceElement );
                    }
                }
                throw new LockException( msg.toString() );
            }
        }
        catch( InterruptedException e )
        {
            throw new LockException( "interrupted while trying to acquire read lock '" + this.name + "' for " + timeout + " " + timeUnit );
        }
    }

    public void releaseReadLock()
    {
        this.lock.readLock().unlock();
    }

    public void acquireWriteLock()
    {
        acquireWriteLock( this.defaultTimeout, this.defaultTimeUnit );
    }

    public void acquireWriteLock( long timeout, @Nonnull TimeUnit timeUnit )
    {
        try
        {
            if( !this.lock.writeLock().tryLock( timeout, timeUnit ) )
            {
                StringBuilder msg = new StringBuilder( 1000 );

                msg.append( "could not acquire write lock '" ).append( this.name ).append( "' for " ).append( timeout ).append( " " ).append( timeUnit ).append( "\n" );
                msg.append( "\n" );

                Thread owner = this.lock.getOwner();
                if( owner == null )
                {
                    msg.append( "No thread holding the write lock is!\n" );
                }
                else
                {
                    msg.append( "Thread holding the write lock is: " ).append( owner.getName() ).append( " [" ).append( owner.getId() ).append( "]\n" );
                    for( StackTraceElement traceElement : owner.getStackTrace() )
                    {
                        msg.append( "\tat " ).append( traceElement );
                    }
                }
                throw new LockException( msg.toString() );
            }
        }
        catch( InterruptedException e )
        {
            throw new LockException( "could not acquire write lock '" + this.name + "' for " + timeout + " " + timeUnit );
        }
    }

    public void releaseWriteLock()
    {
        this.lock.writeLock().unlock();
    }

    public <T> T read( @Nonnull Callable<T> function )
    {
        return read( function, this.defaultTimeout, this.defaultTimeUnit );
    }

    public <T> T read( @Nonnull Callable<T> function, long timeout, @Nonnull TimeUnit timeUnit )
    {
        acquireReadLock( timeout, timeUnit );
        try
        {
            return function.call();
        }
        catch( RuntimeException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            throw new RuntimeException( e );
        }
        finally
        {
            releaseReadLock();
        }
    }

    public void read( @Nonnull Runnable function )
    {
        read( function, this.defaultTimeout, this.defaultTimeUnit );
    }

    public void read( @Nonnull Runnable function, long timeout, @Nonnull TimeUnit timeUnit )
    {
        acquireReadLock( timeout, timeUnit );
        try
        {
            function.run();
        }
        catch( RuntimeException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            throw new RuntimeException( e );
        }
        finally
        {
            releaseReadLock();
        }
    }

    public <T> T write( @Nonnull Callable<T> function )
    {
        int readHoldCount = 0;
        while( this.lock.getReadHoldCount() > 0 )
        {
            releaseReadLock();
            readHoldCount++;
        }

        try
        {
            return write( function, this.defaultTimeout, this.defaultTimeUnit );
        }
        finally
        {
            for( int i = 0; i < readHoldCount; i++ )
            {
                acquireReadLock();
            }
        }
    }

    public <T> T write( @Nonnull Callable<T> function, long timeout, @Nonnull TimeUnit timeUnit )
    {
        int readHoldCount = 0;
        while( this.lock.getReadHoldCount() > 0 )
        {
            releaseReadLock();
            readHoldCount++;
        }

        try
        {
            acquireWriteLock( timeout, timeUnit );
            try
            {
                return function.call();
            }
            catch( RuntimeException e )
            {
                throw e;
            }
            catch( Exception e )
            {
                throw new RuntimeException( e );
            }
            finally
            {
                releaseWriteLock();
            }
        }
        finally
        {
            for( int i = 0; i < readHoldCount; i++ )
            {
                acquireReadLock();
            }
        }
    }

    public void write( @Nonnull Runnable function )
    {
        write( function, this.defaultTimeout, this.defaultTimeUnit );
    }

    public void write( @Nonnull Runnable function, long timeout, @Nonnull TimeUnit timeUnit )
    {
        int readHoldCount = 0;
        while( this.lock.getReadHoldCount() > 0 )
        {
            releaseReadLock();
            readHoldCount++;
        }

        try
        {
            acquireWriteLock( timeout, timeUnit );
            try
            {
                function.run();
            }
            catch( RuntimeException e )
            {
                throw e;
            }
            catch( Exception e )
            {
                throw new RuntimeException( e );
            }
            finally
            {
                releaseWriteLock();
            }
        }
        finally
        {
            for( int i = 0; i < readHoldCount; i++ )
            {
                acquireReadLock();
            }
        }
    }

    private class MosaicReentrantReadWriteLock extends ReentrantReadWriteLock
    {
        private MosaicReentrantReadWriteLock( boolean fair )
        {
            super( fair );
        }

        @Override
        public Thread getOwner()
        {
            return super.getOwner();
        }

        @Override
        public Collection<Thread> getQueuedReaderThreads()
        {
            return super.getQueuedReaderThreads();
        }

        @Override
        public Collection<Thread> getQueuedThreads()
        {
            return super.getQueuedThreads();
        }

        @Override
        public Collection<Thread> getQueuedWriterThreads()
        {
            return super.getQueuedWriterThreads();
        }
    }
}
