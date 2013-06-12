package org.mosaic.idea.runner.task;

import com.intellij.execution.BeforeRunTask;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.module.ModulePointerManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.RootPolicy;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

/**
 * @author arik
 */
public class BuildMosaicModulesBeforeRunTask extends BeforeRunTask<BuildMosaicModulesBeforeRunTask>
{
    private final Project project;

    private List<ModulePointer> modulesToBuild;

    public BuildMosaicModulesBeforeRunTask( Project project )
    {
        super( BuildMosaicModulesBeforeRunTasksProvider.ID );
        this.project = project;
    }

    @Nullable
    public List<ModulePointer> getModulesToBuild()
    {
        return this.modulesToBuild;
    }

    public void setModulesToBuild( @Nullable List<Module> modulesToBuild )
    {
        if( modulesToBuild != null && !modulesToBuild.isEmpty() )
        {
            final ModulePointerManager modulePointerManager = ModulePointerManager.getInstance( this.project );

            Set<ModulePointer> finalModulePointers = new LinkedHashSet<>();
            for( Module module : modulesToBuild )
            {
                finalModulePointers.add( modulePointerManager.create( module ) );
                finalModulePointers.addAll(
                        ModuleRootManager.getInstance( module )
                                         .orderEntries()
                                         .withoutSdk()
                                         .withoutLibraries()
                                         .withoutModuleSourceEntries()
                                         .process( new RootPolicy<List<ModulePointer>>()
                                         {
                                             @Override
                                             public List<ModulePointer> visitModuleOrderEntry( ModuleOrderEntry moduleOrderEntry,
                                                                                               List<ModulePointer> modulePointers )
                                             {
                                                 Module module = moduleOrderEntry.getModule();
                                                 if( module != null )
                                                 {
                                                     modulePointers.add( modulePointerManager.create( module ) );
                                                 }
                                                 return modulePointers;
                                             }
                                         }, new LinkedList<ModulePointer>() ) );
            }
            this.modulesToBuild = new LinkedList<>( finalModulePointers );
        }
        else
        {
            this.modulesToBuild = null;
        }
    }

    @Override
    public void readExternal( Element element )
    {
        super.readExternal( element );

        List moduleElements = element.getChildren( "module" );
        if( !moduleElements.isEmpty() )
        {
            ModulePointerManager modulePointerManager = ModulePointerManager.getInstance( this.project );
            for( Object moduleElementObject : moduleElements )
            {
                String moduleName = ( ( Element ) moduleElementObject ).getAttributeValue( "name" );
                if( moduleName != null )
                {
                    ModulePointer modulePointer = modulePointerManager.create( moduleName );
                    if( this.modulesToBuild == null )
                    {
                        this.modulesToBuild = new LinkedList<>();
                        this.modulesToBuild.add( modulePointer );
                    }
                    else if( !this.modulesToBuild.contains( modulePointer ) )
                    {
                        this.modulesToBuild.add( modulePointer );
                    }
                }
            }
        }
    }

    @Override
    public void writeExternal( Element element )
    {
        super.writeExternal( element );
        if( this.modulesToBuild != null )
        {
            for( ModulePointer modulePointer : this.modulesToBuild )
            {
                Element moduleElement = new Element( "module" );
                moduleElement.setAttribute( "name", modulePointer.getModuleName() );
                element.addContent( moduleElement );
            }
        }
    }

    @Override
    public BeforeRunTask clone()
    {
        final BuildMosaicModulesBeforeRunTask task = ( BuildMosaicModulesBeforeRunTask ) super.clone();
        if( this.modulesToBuild != null )
        {
            task.modulesToBuild = new LinkedList<>( this.modulesToBuild );
        }
        else
        {
            task.modulesToBuild = null;
        }
        return task;
    }

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
        if( !super.equals( o ) )
        {
            return false;
        }

        BuildMosaicModulesBeforeRunTask that = ( BuildMosaicModulesBeforeRunTask ) o;

        if( modulesToBuild != null ? !modulesToBuild.equals( that.modulesToBuild ) : that.modulesToBuild != null )
        {
            return false;
        }
        if( !project.equals( that.project ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + project.hashCode();
        result = 31 * result + ( modulesToBuild != null ? modulesToBuild.hashCode() : 0 );
        return result;
    }
}
