package org.mosaic.development.idea.util;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;

import static com.intellij.openapi.vfs.VfsUtilCore.virtualToIoFile;

/**
 * @author arik
 */
public class Util
{
    @NotNull
    public static String getBundleFileName( @NotNull MavenProject mavenProject )
    {
        String extension = mavenProject.getPackaging();
        if( extension.isEmpty() || "bundle".equals( extension ) || "pom".equals( extension ) )
        {
            extension = "jar"; // just in case maven gets confused
        }
        return mavenProject.getFinalName() + '.' + extension;
    }

    public static long findLatestFileModificationTime( @NotNull VirtualFile outputDirectory )
    {
        final AtomicLong highestClassModificationTime = new AtomicLong( 0 );
        VfsUtil.visitChildrenRecursively( outputDirectory, new VirtualFileVisitor<Long>()
        {
            @Override
            public boolean visitFile( @NotNull VirtualFile file )
            {
                long modificationStamp = virtualToIoFile( file ).lastModified();
                if( modificationStamp > highestClassModificationTime.get() )
                {
                    highestClassModificationTime.set( modificationStamp );
                }
                return true;
            }
        } );
        return highestClassModificationTime.get();
    }
}
