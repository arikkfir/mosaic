package org.mosaic.development.idea.run;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.mosaic.development.idea.facet.OsgiBundleFacet;

/**
 * @author arik
 */
public abstract class DeploymentUnit
{
    private static final String[] EMPTY_STRINGS_ARRAY = new String[ 0 ];

    private static class ModuleDeploymentUnit extends DeploymentUnit
    {
        private ModuleDeploymentUnit( @NotNull String name )
        {
            super( Type.MODULE, name );
        }

        @NotNull
        @Override
        public String[] getFilePaths( @NotNull Project project )
        {
            ModuleManager moduleManager = ModuleManager.getInstance( project );
            Module module = moduleManager.findModuleByName( getName() );
            if( module != null )
            {
                OsgiBundleFacet facet = OsgiBundleFacet.getInstance( module );
                if( facet != null )
                {
                    String bundlePath = facet.getBundlePath();
                    if( bundlePath != null )
                    {
                        return new String[] { bundlePath };
                    }
                }
            }
            return EMPTY_STRINGS_ARRAY;
        }
    }

    private static class ProjectLibraryFileDeploymentUnit extends DeploymentUnit
    {
        private ProjectLibraryFileDeploymentUnit( @NotNull String libraryName )
        {
            super( Type.PROJECT_LIBRARY, libraryName );
        }

        @NotNull
        @Override
        public String[] getFilePaths( @NotNull Project project )
        {
            LibraryTable libraryTable = ProjectLibraryTable.getInstance( project );
            Library library = libraryTable.getLibraryByName( getName() );
            if( library != null )
            {
                List<String> paths = new LinkedList<>();
                for( VirtualFile file : library.getFiles( OrderRootType.CLASSES ) )
                {
                    paths.add( file.getPath() );
                }
                return paths.toArray( new String[ paths.size() ] );
            }
            return EMPTY_STRINGS_ARRAY;
        }
    }

    private static class FileDeploymentUnit extends DeploymentUnit
    {
        private FileDeploymentUnit( @NotNull String path )
        {
            super( Type.FILE, path );
        }

        @NotNull
        @Override
        public String[] getFilePaths( @NotNull Project project )
        {
            return new String[] { getName() };
        }
    }

    public static enum Type
    {
        MODULE,
        PROJECT_LIBRARY,
        FILE
    }

    public static DeploymentUnit module( @NotNull Module module )
    {
        return new ModuleDeploymentUnit( module.getName() );
    }

    public static DeploymentUnit projectLibrary( @NotNull Library library )
    {
        String libraryName = library.getName();
        if( libraryName == null )
        {
            throw new IllegalArgumentException( "empty library name: " + library );
        }
        else
        {
            return new ProjectLibraryFileDeploymentUnit( libraryName );
        }
    }

    public static DeploymentUnit file( @NotNull VirtualFile file )
    {
        return new FileDeploymentUnit( file.getPath() );
    }

    public static DeploymentUnit file( @NotNull String filePath )
    {
        return new FileDeploymentUnit( filePath );
    }

    public static DeploymentUnit file( @NotNull File file )
    {
        return new FileDeploymentUnit( file.getPath() );
    }

    public static DeploymentUnit unit( @NotNull Type type, @NotNull String value )
    {
        switch( type )
        {
            case MODULE:
                return new ModuleDeploymentUnit( value );
            case PROJECT_LIBRARY:
                return new ProjectLibraryFileDeploymentUnit( value );
            case FILE:
                return new FileDeploymentUnit( value );
            default:
                throw new IllegalArgumentException( "Unknown type: " + type );
        }
    }

    @NotNull
    private final Type type;

    @NotNull
    private final String name;

    private DeploymentUnit( @NotNull Type type, @NotNull String name )
    {
        this.type = type;
        this.name = name;
    }

    @NotNull
    public Type getType()
    {
        return this.type;
    }

    @NotNull
    public String getName()
    {
        return this.name;
    }

    @NotNull
    public abstract String[] getFilePaths( @NotNull Project project );

    @SuppressWarnings( "RedundantIfStatement" )
    @Override
    public boolean equals( Object o )
    {
        if( this == o )
        {
            return true;
        }
        if( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        DeploymentUnit that = ( DeploymentUnit ) o;

        if( !name.equals( that.name ) )
        {
            return false;
        }
        if( type != that.type )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = type.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }
}
