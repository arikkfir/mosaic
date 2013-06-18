package org.mosaic.idea.module.actions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.mosaic.idea.module.facet.BuildMessages;
import org.mosaic.idea.module.facet.ModuleFacet;

/**
 * @author arik
 */
public class BuildModulesTask extends Task.Modal
{
    private static final Logger LOG = Logger.getInstance( BuildModulesTask.class );

    private final List<Module> modules;

    private boolean successful;

    public BuildModulesTask( @NotNull Project project )
    {
        this( project, ModuleFacet.findMosaicModules( project ) );
    }

    public BuildModulesTask( @NotNull Project project, @NotNull List<Module> modules )
    {
        super( project, "Building Mosaic modules", true );
        this.modules = modules;
    }

    public boolean isSuccessful()
    {
        return this.successful;
    }

    @Override
    public void run( @NotNull ProgressIndicator indicator )
    {
        LOG.info( "Building mosaic modules for: " + this.modules );

        BuildMessages.getInstance( getProject() ).clearBundleMessages();

        this.successful = true;
        if( this.modules.isEmpty() )
        {
            return;
        }

        try
        {
            indicator.setIndeterminate( false );
            indicator.setFraction( 0d );
            indicator.setText( "Building Mosaic modules..." );
            for( int i = 0; i < this.modules.size(); i++ )
            {
                ModuleFacet facet = ModuleFacet.getInstance( this.modules.get( i ) );
                if( indicator.isCanceled() )
                {
                    this.successful = false;
                    return;
                }

                indicator.setText2( "[" + facet.getModule().getName() + "]" );
                this.successful = this.successful && facet.build();
                indicator.setFraction( ( i + 1 ) / this.modules.size() );
            }
        }
        catch( Exception e )
        {
            this.successful = false;
            throw e;
        }
    }
}
