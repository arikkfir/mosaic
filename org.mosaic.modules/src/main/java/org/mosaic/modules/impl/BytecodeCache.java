package org.mosaic.modules.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.pair.Pair;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.weaving.WeavingException;

import static java.nio.file.Files.*;
import static org.mosaic.modules.impl.ModuleWeavingHook.InterceptedMethodInfo;

/**
 * @author arik
 */
final class BytecodeCache
{
    static class WovenBytecode
    {
        final byte[] bytes;

        final Map<Long, Pair<String, String[]>> methodNamesAndParameters;

        WovenBytecode( @Nonnull Path file ) throws IOException
        {
            this.methodNamesAndParameters = new LinkedHashMap<>( 20 );

            try( DataInputStream dataInputStream = new DataInputStream( Files.newInputStream( file ) ) )
            {
                int methodCount = dataInputStream.readInt();
                for( int i = 0; i < methodCount; i++ )
                {
                    long id = dataInputStream.readLong();
                    String methodName = dataInputStream.readUTF();

                    int parameterCount = dataInputStream.readInt();
                    String[] parameterNames = new String[ parameterCount ];
                    for( int j = 0; j < parameterCount; j++ )
                    {
                        parameterNames[ j ] = dataInputStream.readUTF();
                    }
                    this.methodNamesAndParameters.put( id, Pair.of( methodName, parameterNames ) );
                }

                int bytecodeLength = dataInputStream.readInt();
                this.bytes = new byte[ bytecodeLength ];
                if( dataInputStream.read( this.bytes ) != bytecodeLength )
                {
                    throw new IOException( "illegal format of bytecode cache at '" + file + "'" );
                }
            }
        }
    }

    @Nonnull
    private final Path weavingCacheDir;

    BytecodeCache( @Nonnull BundleContext bundleContext )
    {
        String workDirLocation = bundleContext.getProperty( "mosaic.home.work" );
        if( workDirLocation == null )
        {
            throw new IllegalStateException( "could not discover Mosaic work directory from bundle property 'mosaic.home.work'" );
        }

        Path workDir = Paths.get( workDirLocation );
        this.weavingCacheDir = workDir.resolve( "weaving" );
    }

    @Nullable
    WovenBytecode getByteCode( @Nonnull Bundle bundle, @Nonnull String className ) throws IOException
    {
        ensureNextIdFileExists();

        Path bytesCacheFile = findClassBytecodeCacheFile( bundle, className );
        if( exists( bytesCacheFile ) )
        {
            try
            {
                return new WovenBytecode( bytesCacheFile );
            }
            catch( IOException e )
            {
                try
                {
                    deleteIfExists( bytesCacheFile );
                }
                catch( IOException ignore )
                {
                }
                throw e;
            }
        }
        else
        {
            return null;
        }
    }

    void storeBytecode( @Nonnull Bundle bundle,
                        @Nonnull String className,
                        @Nonnull byte[] bytes,
                        @Nonnull Collection<InterceptedMethodInfo> interceptedMethods ) throws IOException
    {
        Path bytesCacheFile = findClassBytecodeCacheFile( bundle, className );

        createDirectories( bytesCacheFile.getParent() );
        try( OutputStream outputStream = Files.newOutputStream( bytesCacheFile ) )
        {
            DataOutputStream dataOutputStream = new DataOutputStream( outputStream );
            dataOutputStream.writeInt( interceptedMethods.size() );
            for( InterceptedMethodInfo interceptedMethodInfo : interceptedMethods )
            {
                dataOutputStream.writeLong( interceptedMethodInfo.id );
                dataOutputStream.writeUTF( interceptedMethodInfo.methodName );
                dataOutputStream.writeInt( interceptedMethodInfo.paramterTypeNames.length );
                for( String paramterTypeName : interceptedMethodInfo.paramterTypeNames )
                {
                    dataOutputStream.writeUTF( paramterTypeName );
                }
            }
            dataOutputStream.writeInt( bytes.length );
            dataOutputStream.write( bytes );
            dataOutputStream.flush();
        }
    }

    @Nonnull
    private Path findClassBytecodeCacheFile( @Nonnull Bundle bundle, @Nonnull String className ) throws IOException
    {
        String bundleLocation = bundle.getLocation();
        if( bundleLocation.startsWith( "file:" ) )
        {
            bundleLocation = bundleLocation.substring( "file:".length() );
        }

        long bundleFileModTime = getLastModifiedTime( Paths.get( bundleLocation ) ).toMillis();
        Path bundleCacheDir = this.weavingCacheDir.resolve( bundle.getSymbolicName() + "-" + bundle.getVersion() + "/" + bundleFileModTime );
        return bundleCacheDir.resolve( className + ".class.bytes" );
    }

    private synchronized void ensureNextIdFileExists()
    {
        Path nextIdFile = this.weavingCacheDir.resolve( "id" );
        if( notExists( nextIdFile ) )
        {
            try
            {
                deletePath( this.weavingCacheDir );
                createDirectories( this.weavingCacheDir );
            }
            catch( IOException e )
            {
                throw new WeavingException( "could not clean cache dir (due to missing next-id file)", e );
            }

            try
            {
                Files.write( nextIdFile, "0".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE );
            }
            catch( IOException e )
            {
                throw new WeavingException( "cannot create next-id file", e );
            }
        }
    }

    private void deletePath( @Nonnull Path path ) throws IOException
    {
        if( Files.isDirectory( path ) )
        {
            Files.walkFileTree( path, new SimpleFileVisitor<Path>()
            {
                @Nonnull
                @Override
                public FileVisitResult visitFile( @Nonnull Path file, @Nonnull BasicFileAttributes attrs )
                        throws IOException
                {
                    Files.delete( file );
                    return FileVisitResult.CONTINUE;
                }

                @Nonnull
                @Override
                public FileVisitResult postVisitDirectory( @Nonnull Path dir, @Nullable IOException exc )
                        throws IOException
                {
                    if( exc != null )
                    {
                        throw exc;
                    }
                    Files.delete( dir );
                    return FileVisitResult.CONTINUE;
                }
            } );
        }
        else if( exists( path ) )
        {
            Files.delete( path );
        }
    }
}
