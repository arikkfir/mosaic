package org.mosaic.development.idea.make.impl;

import com.intellij.execution.ExecutionHelper;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.continuation.ModalityIgnorantBackgroundableTask;
import java.io.File;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenProjectProblem;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.server.MavenEmbedderWrapper;
import org.jetbrains.idea.maven.server.MavenServerExecutionResult;
import org.jetbrains.idea.maven.server.MavenServerManager;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;
import org.mosaic.development.idea.facet.OsgiBundleFacet;
import org.mosaic.development.idea.messages.BundleMessageView;

import static com.intellij.openapi.vfs.VfsUtil.findFileByIoFile;
import static com.intellij.openapi.vfs.VfsUtilCore.virtualToIoFile;
import static java.util.Arrays.asList;
import static org.mosaic.development.idea.util.impl.Util.findLatestFileModificationTime;
import static org.mosaic.development.idea.util.impl.Util.getBundleFileName;

/**
 * @author arik
 */
final class BuildBundlesBackgroundable extends ModalityIgnorantBackgroundableTask
{
    private static final Logger LOG = Logger.getInstance( BuildBundlesBackgroundable.class );

    @NotNull
    private final Project project;

    @NotNull
    private final Module[] modules;

    private final Runnable onCompletion;

    private final Runnable onSuccess;

    private final Runnable onFailure;

    private final Runnable onCancel;

    private boolean success;

    BuildBundlesBackgroundable( @NotNull Project project, @NotNull List<Module> modules, Runnable onCompletion )
    {
        this( project, modules, onCompletion, null, null, null );
    }

    BuildBundlesBackgroundable( @NotNull Project project,
                                @NotNull List<Module> modules,
                                Runnable onCompletion,
                                Runnable onSuccess,
                                Runnable onFailure,
                                Runnable onCancel )
    {
        super( project, "Building OSGi bundles", false );
        this.project = project;
        this.modules = modules.toArray( new Module[ modules.size() ] );
        this.onCompletion = onCompletion;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
        this.onCancel = onCancel;
    }

    @Override
    protected final void runImpl( @NotNull ProgressIndicator indicator )
    {
        BundleMessageView.SERVICE.getInstance( this.project ).clear();

        ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
        progressIndicator.setIndeterminate( true );

        this.success = false;
        for( Module module : this.modules )
        {
            // stop if canceled
            if( progressIndicator.isCanceled() )
            {
                return;
            }

            // check that module is still a valid Maven module
            MavenProject mavenProject = MavenProjectsManager.getInstance( module.getProject() ).findProject( module );
            if( mavenProject != null && shouldBuild( module, mavenProject ) )
            {
                progressIndicator.setText( "Building bundle for module " + module.getName() );
                MavenEmbedderWrapper embedder = MavenServerManager.getInstance().createEmbedder( module.getProject(), false );
                try
                {
                    if( !build( module, mavenProject, embedder ) )
                    {
                        return;
                    }
                }
                catch( Exception e )
                {
                    BundleMessageView.SERVICE.getInstance( module.getProject() ).showError(
                            module.getName(),
                            new ExecutionHelper.FakeNavigatable(),
                            "Bundle build failure: " + e.getMessage()
                    );
                    return;
                }
                finally
                {
                    embedder.release();
                    ProgressManager.getInstance().getProgressIndicator().setText2( "Done" );
                }
            }
        }
        this.success = true;
    }

    @Override
    protected void doInAwtIfSuccess()
    {
        if( this.success )
        {
            if( this.onSuccess != null )
            {
                this.onSuccess.run();
            }
        }
        else
        {
            if( this.onFailure != null )
            {
                this.onFailure.run();
            }
        }

        if( this.onCompletion != null )
        {
            this.onCompletion.run();
        }
    }

    @Override
    protected void doInAwtIfFail( Exception e )
    {
        LOG.error( "Could not build OSGi bundles: " + e.getMessage(), e );
        BundleMessageView.SERVICE.getInstance( this.project ).showError(
                "General",
                new ExecutionHelper.FakeNavigatable(),
                e.getMessage()
        );

        if( this.onFailure != null )
        {
            this.onFailure.run();
        }

        if( this.onCompletion != null )
        {
            this.onCompletion.run();
        }
    }

    @Override
    protected void doInAwtIfCancel()
    {
        BundleMessageView.SERVICE.getInstance( this.project ).showError(
                "General",
                new ExecutionHelper.FakeNavigatable(),
                "User canceled OSGi bundles build."
        );

        if( this.onCancel != null )
        {
            this.onCancel.run();
        }

        if( this.onCompletion != null )
        {
            this.onCompletion.run();
        }
    }

    private boolean shouldBuild( @NotNull Module module, @NotNull MavenProject mavenProject )
    {
        OsgiBundleFacet facet = OsgiBundleFacet.getInstance( module );
        if( facet == null )
        {
            return false;
        }

        // find build directory - if not exists, continue to building the bundle
        File buildDirectory = new File( mavenProject.getBuildDirectory() );
        if( !buildDirectory.exists() )
        {
            // no build dir -> build needed
            return true;
        }

        // find bundle JAR file - if not exists, continue to building the bundle
        File jarFile = new File( buildDirectory, getBundleFileName( mavenProject ) );
        if( !jarFile.exists() )
        {
            // no bundle jar file -> build needed
            return true;
        }

        // check if POM file is older than the bundle JAR file - if newer, continue to building the bundle
        long bundleModificationTime = jarFile.lastModified();
        if( virtualToIoFile( mavenProject.getFile() ).lastModified() >= bundleModificationTime )
        {
            // POM file modified after bundle was last modified -> build needed
            return true;
        }

        // find output directory (classes) - if not exists, continue to building the bundle
        File outputDirectory = new File( mavenProject.getOutputDirectory() );
        if( !outputDirectory.exists() )
        {
            // no output directory (classes) -> build needed
            return true;
        }

        // check if there are classes newer than the bundle - if not, there is no need to build the bundle
        // we add 1sec bcz the bundle modtime might be equal to the file modified last in it, in which case no need to rebuild it
        VirtualFile outputDirectoryVFile = findFileByIoFile( outputDirectory, true );
        return outputDirectoryVFile == null || bundleModificationTime + 500 < findLatestFileModificationTime( outputDirectoryVFile );
    }

    private boolean build( @NotNull Module module,
                           @NotNull MavenProject mavenProject,
                           @NotNull MavenEmbedderWrapper embedder )
            throws MavenProcessCanceledException
    {
        // run "bundle:bundle" goal, and add "intellij" profile, in case project wants to do some
        // custom stuff when inside intellij (if profile does not exist in project it's still ok)
        List<String> profiles = asList( "intellij" );
        List<String> goals = asList( "bundle:bundle" );

        ProgressManager.getInstance().getProgressIndicator().setText2( "Running Felix Bundle Plugin..." );
        MavenServerExecutionResult result = embedder.execute( mavenProject.getFile(), profiles, goals );
        for( MavenProjectProblem problem : result.problems )
        {
            BundleMessageView.SERVICE.getInstance( module.getProject() ).showError(
                    module.getName(),
                    new ExecutionHelper.FakeNavigatable(),
                    "Maven problem encountered: " + problem.getDescription()
            );
        }
        return result.problems.isEmpty();
    }
}
