package org.mosaic.development.idea.make.impl.compiler;

import com.intellij.openapi.compiler.ValidityState;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.util.io.IOUtil;
import gnu.trove.TObjectLongHashMap;
import gnu.trove.TObjectLongProcedure;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import static com.intellij.openapi.vfs.VfsUtil.findFileByIoFile;
import static com.intellij.openapi.vfs.VfsUtilCore.virtualToIoFile;

public class BundleValidityState implements ValidityState
{
    @NotNull
    private final String moduleName;

    @NotNull
    private final String[] filePaths;

    @NotNull
    private final long[] fileTimestamps;

    public BundleValidityState( @NotNull Module module )
    {
        this.moduleName = module.getName();

        final TObjectLongHashMap<String> paths2Timestamps = new TObjectLongHashMap<>();

        MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance( module.getProject() );
        if( mavenProjectsManager != null )
        {
            MavenProject mavenProject = mavenProjectsManager.findProject( module );
            if( mavenProject != null )
            {
                paths2Timestamps.put( virtualToIoFile( mavenProject.getFile() ).getAbsolutePath(),
                                      virtualToIoFile( mavenProject.getFile() ).lastModified() );

                VirtualFile buildDirectory = findFileByIoFile( new File( mavenProject.getBuildDirectory() ), true );
                if( buildDirectory != null )
                {
                    VirtualFile jarFile = buildDirectory.findChild( getBundleFileName( mavenProject ) );
                    if( jarFile != null )
                    {
                        paths2Timestamps.put( virtualToIoFile( jarFile ).getAbsolutePath(),
                                              virtualToIoFile( jarFile ).lastModified() );
                    }
                }

                VirtualFile outputDirectory = findFileByIoFile( new File( mavenProject.getOutputDirectory() ), true );
                if( outputDirectory != null )
                {
                    VfsUtil.visitChildrenRecursively( outputDirectory, new VirtualFileVisitor<Long>()
                    {
                        @Override
                        public boolean visitFile( @NotNull VirtualFile file )
                        {
                            long modificationStamp = virtualToIoFile( file ).lastModified();
                            paths2Timestamps.put( file.getPath(), modificationStamp );
                            return true;
                        }
                    } );
                }
            }
        }

        // we put the paths and timestamps into two arrays for easy serialization
        this.filePaths = new String[ paths2Timestamps.size() ];
        this.fileTimestamps = new long[ paths2Timestamps.size() ];
        paths2Timestamps.forEachEntry( new TObjectLongProcedure<String>()
        {
            private int i = 0;

            @Override
            public boolean execute( String s, long l )
            {
                BundleValidityState.this.filePaths[ i ] = s;
                BundleValidityState.this.fileTimestamps[ i ] = l;
                i++;
                return true;
            }
        } );
    }

    public BundleValidityState( @NotNull DataInput in ) throws IOException
    {
        this.moduleName = IOUtil.readString( in );

        int i = in.readInt();
        this.filePaths = new String[ i ];
        this.fileTimestamps = new long[ i ];
        for( int j = 0; j < i; j++ )
        {
            this.filePaths[ j ] = IOUtil.readString( in );
            this.fileTimestamps[ j ] = in.readLong();
        }
    }

    @Override
    public void save( @NotNull DataOutput out ) throws IOException
    {
        IOUtil.writeString( this.moduleName, out );

        int i = this.filePaths.length;
        out.writeInt( i );
        for( int j = 0; j < i; j++ )
        {
            IOUtil.writeString( this.filePaths[ j ], out );
            out.writeLong( this.fileTimestamps[ j ] );
        }
    }

    @Override
    public boolean equalsTo( ValidityState validityState )
    {
        if( !( validityState instanceof BundleValidityState ) )
        {
            return false;
        }

        BundleValidityState other = ( BundleValidityState ) validityState;
        if( !this.moduleName.equals( other.moduleName ) )
        {
            return false;
        }

        if( this.filePaths.length != other.filePaths.length )
        {
            return false;
        }
        for( int i = 0; i < this.filePaths.length; i++ )
        {
            if( !Comparing.strEqual( this.filePaths[ i ], other.filePaths[ i ] ) ||
                this.fileTimestamps[ i ] != other.fileTimestamps[ i ] )
            {
                return false;
            }
        }

        return true;
    }

    @NotNull
    private static String getBundleFileName( @NotNull MavenProject mavenProject )
    {
        String extension = mavenProject.getPackaging();
        if( extension.isEmpty() || "bundle".equals( extension ) || "pom".equals( extension ) )
        {
            extension = "jar"; // just in case maven gets confused
        }
        return mavenProject.getFinalName() + '.' + extension;
    }
}
