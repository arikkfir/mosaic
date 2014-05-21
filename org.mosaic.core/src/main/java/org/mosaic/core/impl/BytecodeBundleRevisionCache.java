package org.mosaic.core.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleRevision;

import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.*;

/**
 * @author arik
 */
class BytecodeBundleRevisionCache
{
    @Nonnull
    private final ServerImpl server;

    @Nonnull
    private final ReadWriteLock lock;

    @Nonnull
    private final BundleRevision bundleRevision;

    @Nonnull
    private final BytecodeCompiler compiler;

    @Nonnull
    private final Path file;

    @Nonnull
    private final Map<String, byte[]> byteCodes = new HashMap<>();

    private boolean dirty;

    BytecodeBundleRevisionCache( @Nonnull ServerImpl server,
                                 @Nonnull BytecodeCompiler compiler,
                                 @Nonnull Path weavingDir,
                                 @Nonnull BundleRevision bundleRevision )
    {
        this.server = server;
        this.lock = this.server.getLock();
        this.bundleRevision = bundleRevision;
        this.compiler = compiler;

        Path bundleDir = weavingDir.resolve( this.bundleRevision.getBundle().getSymbolicName() );
        Path versionDir = bundleDir.resolve( this.bundleRevision.getVersion().toString() );
        this.file = versionDir.resolve( this.bundleRevision.toString() + ".bytes" );
        loadCacheFromFile();
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this )
                             .add( "file", this.file )
                             .toString();
    }

    @Nullable
    byte[] getBytecode( @Nonnull ModuleRevisionImpl moduleRevision, @Nonnull WovenClass wovenClass )
    {
        String className = wovenClass.getClassName();

        this.lock.acquireReadLock();
        try
        {
            byte[] bytes = this.byteCodes.get( className );
            if( bytes == null )
            {
                this.lock.releaseReadLock();
                this.lock.acquireWriteLock();
                try
                {
                    bytes = this.compiler.compile( moduleRevision, wovenClass );
                    if( bytes == null )
                    {
                        bytes = new byte[ 0 ];
                    }
                    this.byteCodes.put( className, bytes );
                    this.dirty = true;
                }
                finally
                {
                    this.lock.releaseWriteLock();
                    this.lock.acquireReadLock();
                }
            }
            return bytes.length == 0 ? null : bytes;
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    void loadCacheFromFile()
    {
        this.lock.acquireWriteLock();
        try
        {
            if( exists( this.file ) )
            {
                this.server.getLogger().trace( "Loading bytecode cache for {} from {}", this.bundleRevision, this.file );

                boolean corrupt = false;
                try( DataInputStream in = new DataInputStream( newInputStream( this.file, READ ) ) )
                {
                    int classCount = in.readInt();
                    for( int i = 0; i < classCount; i++ )
                    {
                        String className = in.readUTF();
                        byte[] bytes = new byte[ in.readInt() ];
                        if( in.read( bytes ) != bytes.length )
                        {
                            corrupt = true;
                            this.byteCodes.clear();
                            break;
                        }
                        this.byteCodes.put( className, bytes );
                    }
                }
                catch( IOException e )
                {
                    this.server.getLogger().error( "Error reading bytecode cache from '{}': {}", this.file, e.getMessage(), e );
                }

                if( corrupt )
                {
                    this.server.getLogger().error( "Bytecode cache file at '{}' is corrupt", this.file );
                    try
                    {
                        Files.delete( this.file );
                    }
                    catch( IOException ignore )
                    {
                    }
                }
            }
            this.dirty = false;
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    void saveCacheToFile()
    {
        if( !this.dirty )
        {
            return;
        }

        this.lock.acquireWriteLock();
        try
        {
            this.server.getLogger().trace( "Saving bytecode cache for {} to {}", this.bundleRevision, this.file );

            try
            {
                Files.createDirectories( this.file.getParent() );
            }
            catch( IOException e )
            {
                this.server.getLogger().error( "Could not create cache file directory at '{}': {}", this.file.getParent(), e.getMessage(), e );
                return;
            }

            try( DataOutputStream out = new DataOutputStream( newOutputStream( this.file, CREATE, WRITE, TRUNCATE_EXISTING ) ) )
            {
                out.writeInt( this.byteCodes.size() );
                for( Map.Entry<String, byte[]> entry : this.byteCodes.entrySet() )
                {
                    out.writeUTF( entry.getKey() );

                    byte[] bytes = entry.getValue();
                    out.writeInt( bytes.length );
                    out.write( bytes );
                }
                out.flush();
                this.dirty = false;
            }
            catch( IOException e )
            {
                this.server.getLogger().error( "Error persisting bytecode cache to '{}': {}", this.file, e.getMessage(), e );
            }
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }
}
