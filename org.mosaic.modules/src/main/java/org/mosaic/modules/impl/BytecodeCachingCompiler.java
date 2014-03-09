package org.mosaic.modules.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleRevision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.StandardOpenOption.*;

/**
 * @author arik
 */
class BytecodeCachingCompiler extends BytecodeCompiler
{
    private static final Logger LOG = LoggerFactory.getLogger( BytecodeCachingCompiler.class );

    @Nonnull
    private final BytecodeCompiler compiler;

    BytecodeCachingCompiler( @Nonnull BytecodeCompiler compiler )
    {
        this.compiler = compiler;
    }

    @Nullable
    @Override
    byte[] compile( @Nonnull WovenClass wovenClass )
    {
        byte[] bytes;
        try
        {
            bytes = load( wovenClass );
        }
        catch( IOException e )
        {
            LOG.warn( "Could not load cached bytecode for '{}': {}", wovenClass.getClassName(), e.getMessage(), e );
            bytes = null;
        }

        if( bytes == null )
        {
            bytes = this.compiler.compile( wovenClass );
            if( bytes != null )
            {
                try
                {
                    store( wovenClass, bytes );
                }
                catch( IOException e )
                {
                    LOG.warn( "Could not cache bytecode for '{}': {}", wovenClass.getClassName(), e.getMessage(), e );
                }
            }
        }

        return bytes;
    }

    @Nullable
    private byte[] load( @Nonnull WovenClass wovenClass ) throws IOException
    {
        Path bytesFile = getClassBytesFile( wovenClass );
        if( Files.notExists( bytesFile ) )
        {
            return null;
        }

        long bytesFileModTime = Files.getLastModifiedTime( bytesFile ).toMillis();
        if( getBundleModificationTime( wovenClass.getBundleWiring().getRevision() ) > bytesFileModTime )
        {
            Files.delete( bytesFile );
            return null;
        }

        return Files.readAllBytes( bytesFile );
    }

    private void store( @Nonnull WovenClass wovenClass, @Nonnull byte[] bytes ) throws IOException
    {
        Path bytesFile = getClassBytesFile( wovenClass );
        Files.createDirectories( bytesFile.getParent() );
        Files.write( bytesFile, bytes, CREATE, TRUNCATE_EXISTING, WRITE );
    }

    private long getBundleModificationTime( @Nonnull BundleRevision revision ) throws IOException
    {
        String bundleLocation = revision.getBundle().getLocation();
        if( bundleLocation.startsWith( "file:" ) )
        {
            bundleLocation = bundleLocation.substring( "file:".length() );
        }
        Path bundleFile = Paths.get( bundleLocation );
        return Files.getLastModifiedTime( bundleFile ).toMillis();
    }

    @Nonnull
    private Path getClassBytesFile( @Nonnull WovenClass wovenClass )
    {
        BundleRevision bundleRevision = wovenClass.getBundleWiring().getRevision();
        Path workDir = Activator.getWorkPath();
        Path weavingDir = workDir.resolve( "weaving" );
        Path bundleDir = weavingDir.resolve( bundleRevision.getBundle().getSymbolicName() );
        Path versionDir = bundleDir.resolve( bundleRevision.getVersion().toString() );
        return versionDir.resolve( wovenClass.getClassName() + ".bytes" );
    }
}
