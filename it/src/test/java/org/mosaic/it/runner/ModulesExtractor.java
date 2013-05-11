package org.mosaic.it.runner;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.annotation.Nonnull;

import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.*;

/**
 * @author arik
 */
public class ModulesExtractor
{
    @Nonnull
    private final LoadingCache<String, Path> modules;

    public ModulesExtractor()
    {
        this.modules = CacheBuilder.newBuilder().build( new CacheLoader<String, Path>()
        {
            @Override
            public Path load( String key )
            {
                return createModuleJarFile( key );
            }
        } );
    }

    @Nonnull
    public Path getModule( @Nonnull String name )
    {
        return this.modules.getUnchecked( name );
    }

    @Nonnull
    private Path createModuleJarFile( @Nonnull String key )
    {
        Path jarFile;
        try
        {
            jarFile = createTempFile( key, ".jar" );
        }
        catch( IOException e )
        {
            throw new IllegalStateException( "Could not create temporary JAR file for module '" + key + "': " + e.getMessage(), e );
        }

        try( OutputStream out = Files.newOutputStream( jarFile, CREATE, TRUNCATE_EXISTING, WRITE ) )
        {
            Path modulePath = getModuleRoot( key );
            Path rootPath = modulePath.getParent().getParent().getParent().getParent().getParent();

            JarOutputStream jarOutputStream = new JarOutputStream( out, getModuleManifest( modulePath ) );
            for( Path resource : getModuleResources( modulePath ) )
            {
                if( !"MANIFEST.MF".equalsIgnoreCase( resource.getFileName().toString() ) && isRegularFile( resource ) )
                {
                    Path relativePath = rootPath.relativize( resource );
                    jarOutputStream.putNextEntry( new JarEntry( relativePath.toString() ) );
                    copy( resource, jarOutputStream );
                }
            }
            jarOutputStream.closeEntry();
            jarOutputStream.finish();
            jarOutputStream.flush();
        }
        catch( IOException e )
        {
            throw new IllegalStateException( "Could not create temporary JAR file for module '" + key + "': " + e.getMessage(), e );
        }
        return jarFile;
    }

    private Manifest getModuleManifest( @Nonnull Path modulePath )
    {
        Manifest manifest = new Manifest();

        Path manifestPath = modulePath.resolve( "MANIFEST.MF" );
        if( !exists( manifestPath ) )
        {
            throw new IllegalStateException( "Could not find manifest at '" + manifestPath + "'" );
        }
        else if( !isRegularFile( manifestPath ) )
        {
            throw new IllegalStateException( "Manifest at '" + manifestPath + "' is not a file" );
        }
        else if( !isReadable( manifestPath ) )
        {
            throw new IllegalStateException( "Could not read manifest at '" + manifestPath + "'" );
        }
        else
        {
            try( InputStream is = newInputStream( manifestPath, StandardOpenOption.READ ) )
            {
                manifest.read( is );
                return manifest;
            }
            catch( IOException e )
            {
                throw new IllegalStateException( "Could not read manifest from '" + manifestPath + "': " + e.getMessage(), e );
            }
        }
    }

    private Path getModuleRoot( String moduleName )
    {
        URL markerResUrl = getClass().getClassLoader().getResource( "org/mosaic/it/modules/modules.txt" );
        if( markerResUrl == null )
        {
            throw new IllegalStateException( "Could not extract test modules for deployment: could not find modules resources" );
        }
        else if( !markerResUrl.getProtocol().equalsIgnoreCase( "file" ) )
        {
            throw new IllegalStateException( "Unsupported resource protocol for: " + markerResUrl );
        }

        String markerResPath = markerResUrl.getPath();
        Path modulesPath = Paths.get( markerResPath.substring( 0, markerResPath.length() - "/modules.txt".length() ) );
        return modulesPath.resolve( moduleName );
    }

    private SortedSet<Path> getModuleResources( @Nonnull Path modulePath )
    {
        final String pattern = "glob:" + modulePath + "/**";
        final PathMatcher resourceMatcher = FileSystems.getDefault().getPathMatcher( pattern );
        final SortedSet<Path> resources = new TreeSet<>();
        try
        {
            Files.walkFileTree( modulePath, new SimpleFileVisitor<Path>()
            {
                @Nonnull
                @Override
                public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException
                {
                    if( resourceMatcher.matches( file ) )
                    {
                        resources.add( file );
                    }
                    return FileVisitResult.CONTINUE;
                }
            } );
            return resources;
        }
        catch( IOException e )
        {
            throw new IllegalStateException( "Could not iterate directory '" + modulePath + "': " + e.getMessage(), e );
        }
    }
}
