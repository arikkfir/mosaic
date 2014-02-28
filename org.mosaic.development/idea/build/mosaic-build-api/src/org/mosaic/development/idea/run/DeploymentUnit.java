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
import org.jetbrains.annotations.Nullable;
import org.mosaic.development.idea.facet.OsgiBundleFacet;

/**
 * @author arik
 */
public abstract class DeploymentUnit
{
    private static final String[] EMPTY_STRINGS_ARRAY = new String[ 0 ];

    public static DeploymentUnit module( @NotNull Module module )
    {
        return new ModuleDeploymentUnit( module );
    }

    public static DeploymentUnit projectLibrary( @NotNull Project project, @NotNull Library library )
    {
        String libraryName = library.getName();
        if( libraryName == null )
        {
            throw new IllegalArgumentException( "empty library name: " + library );
        }
        else
        {
            return new ProjectLibraryFileDeploymentUnit( project, libraryName );
        }
    }

    public static DeploymentUnit file( @NotNull Project project, @NotNull VirtualFile file )
    {
        return new FileDeploymentUnit( project, file.getPath() );
    }

    public static DeploymentUnit file( @NotNull Project project, @NotNull String filePath )
    {
        return new FileDeploymentUnit( project, filePath );
    }

    public static DeploymentUnit file( @NotNull Project project, @NotNull File file )
    {
        return new FileDeploymentUnit( project, file.getPath() );
    }

    public static DeploymentUnit unit( @NotNull Project project, @NotNull Type type, @NotNull String value )
    {
        switch( type )
        {
            case MODULE:
                return new ModuleDeploymentUnit( project, value );
            case PROJECT_LIBRARY:
                return new ProjectLibraryFileDeploymentUnit( project, value );
            case FILE:
                return new FileDeploymentUnit( project, value );
            default:
                throw new IllegalArgumentException( "Unknown type: " + type );
        }
    }

    public static class ModuleDeploymentUnit extends DeploymentUnit
    {
        private ModuleDeploymentUnit( @NotNull Project project, @NotNull String name )
        {
            super( project, Type.MODULE, name );
        }

        private ModuleDeploymentUnit( @NotNull Module module )
        {
            super( module.getProject(), Type.MODULE, module.getName() );
        }

        @Nullable
        public Module getModule()
        {
            return ModuleManager.getInstance( this.project ).findModuleByName( getName() );
        }

        @NotNull
        @Override
        public String[] getFilePaths()
        {
            ModuleManager moduleManager = ModuleManager.getInstance( this.project );
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

    public static class ProjectLibraryFileDeploymentUnit extends DeploymentUnit
    {
        private ProjectLibraryFileDeploymentUnit( @NotNull Project project, @NotNull String libraryName )
        {
            super( project, Type.PROJECT_LIBRARY, libraryName );
        }

        @NotNull
        @Override
        public String[] getFilePaths()
        {
            LibraryTable libraryTable = ProjectLibraryTable.getInstance( this.project );
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

    public static class FileDeploymentUnit extends DeploymentUnit
    {
        private FileDeploymentUnit( @NotNull Project project, @NotNull String path )
        {
            super( project, Type.FILE, path );
        }

        @NotNull
        @Override
        public String[] getFilePaths()
        {
            return new String[] { getName() };
        }
    }

    @NotNull
    protected final Project project;

    @NotNull
    private final Type type;

    @NotNull
    private final String name;

    private DeploymentUnit( @NotNull Project project, @NotNull Type type, @NotNull String name )
    {
        this.project = project;
        this.type = type;
        this.name = name;
    }

    @NotNull
    public Project getProject()
    {
        return this.project;
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
    public abstract String[] getFilePaths();

    @SuppressWarnings("RedundantIfStatement")
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

    public static enum Type
    {
        MODULE,
        PROJECT_LIBRARY,
        FILE
    }
}
