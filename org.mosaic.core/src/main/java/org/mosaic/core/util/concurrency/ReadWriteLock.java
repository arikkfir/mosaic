package org.mosaic.core.util.concurrency;

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
    private final java.util.concurrent.locks.ReadWriteLock lock = new ReentrantReadWriteLock();

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
                throw new LockException( "could not acquire read lock '" + this.name + "' for " + timeout + " " + timeUnit );
            }
        }
        catch( InterruptedException e )
        {
            throw new LockException( "could not acquire read lock '" + this.name + "' for " + timeout + " " + timeUnit );
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
                throw new LockException( "could not acquire write lock '" + this.name + "' for " + timeout + " " + timeUnit );
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
        return write( function, this.defaultTimeout, this.defaultTimeUnit );
    }

    public <T> T write( @Nonnull Callable<T> function, long timeout, @Nonnull TimeUnit timeUnit )
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

    public void write( @Nonnull Runnable function )
    {
        write( function, this.defaultTimeout, this.defaultTimeUnit );
    }

    public void write( @Nonnull Runnable function, long timeout, @Nonnull TimeUnit timeUnit )
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
}
