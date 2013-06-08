package org.mosaic.idea.module.actions;

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
    private final List<ModuleFacet> moduleFacets;

    private boolean successful;

    public BuildModulesTask( @NotNull Project project )
    {
        this( project, ModuleFacet.getBuildableModules( project ) );
    }

    public BuildModulesTask( @NotNull Project project, @NotNull List<ModuleFacet> moduleFacets )
    {
        super( project, "Building Mosaic modules", true );
        this.moduleFacets = moduleFacets;
    }

    public boolean isSuccessful()
    {
        return this.successful;
    }

    @Override
    public void run( @NotNull ProgressIndicator indicator )
    {
        BuildMessages.getInstance( getProject() ).clearBundleMessages();

        this.successful = true;
        if( this.moduleFacets.isEmpty() )
        {
            return;
        }

        try
        {
            indicator.setIndeterminate( false );
            indicator.setFraction( 0d );
            indicator.setText( "Building Mosaic modules..." );
            for( int i = 0; i < moduleFacets.size(); i++ )
            {
                ModuleFacet facet = moduleFacets.get( i );
                if( indicator.isCanceled() )
                {
                    this.successful = false;
                    return;
                }

                indicator.setText2( "[" + facet.getModule().getName() + "]" );
                this.successful = this.successful && facet.build();
                indicator.setFraction( ( i + 1 ) / moduleFacets.size() );
            }
        }
        catch( Exception e )
        {
            this.successful = false;
            throw e;
        }
    }
}
