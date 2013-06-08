package org.mosaic.idea.module.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VfsUtil;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.execution.SoutMavenConsole;
import org.jetbrains.idea.maven.model.MavenProjectProblem;
import org.jetbrains.idea.maven.model.MavenWorkspaceMap;
import org.jetbrains.idea.maven.project.MavenConsole;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.server.MavenEmbedderWrapper;
import org.jetbrains.idea.maven.server.MavenServerExecutionResult;
import org.jetbrains.idea.maven.server.MavenServerManager;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;
import org.jetbrains.idea.maven.utils.MavenProgressIndicator;
import org.mosaic.idea.module.actions.ModuleBuildException;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
public class ModuleFacet extends Facet<ModuleFacetConfiguration>
{
    private static final Logger LOG = Logger.getInstance( ModuleFacet.class );

    public static ModuleFacet getInstance( Module module )
    {
        return FacetManager.getInstance( module ).getFacetByType( ModuleFacetType.TYPE_ID );
    }

    // moduleA -> (depends) -> moduleB -> (depends) -> moduleC
    //      this means that  'getIn(moduleB)' returns moduleA
    //      this means that 'getOut(moduleB)' returns moduleC
    // Graph<Module> graph = ModuleManager.getInstance( project ).moduleGraph();

    public static List<ModuleFacet> getBuildableModules( Project project )
    {
        AccessToken lock = ApplicationManager.getApplication().acquireReadActionLock();
        try
        {
            List<ModuleFacet> facets = new LinkedList<>();
            for( Module module : asList( ModuleManager.getInstance( project ).getSortedModules() ) )
            {
                ModuleFacet facet = getInstance( module );
                if( facet != null && !facets.contains( facet ) )
                {
                    facets.add( facet );
                }
            }
            return facets;
        }
        finally
        {
            lock.finish();
        }
    }

    public static List<ModuleFacet> getBuildableModules( List<Module> modules )
    {
        AccessToken lock = ApplicationManager.getApplication().acquireReadActionLock();
        try
        {
            List<ModuleFacet> facets = new LinkedList<>();
            for( Module module : modules )
            {
                ModuleFacet facet = getInstance( module );
                if( facet != null && !facets.contains( facet ) )
                {
                    facets.add( facet );
                }
            }
            return facets;
        }
        finally
        {
            lock.finish();
        }
    }

    public ModuleFacet( @NotNull FacetType facetType,
                        @NotNull Module module,
                        @NotNull String name,
                        @NotNull ModuleFacetConfiguration configuration,
                        Facet underlyingFacet )
    {
        super( facetType, module, name, configuration, underlyingFacet );
    }

    public boolean build()
    {
        final Computable<Boolean> writeAction = new Computable<Boolean>()
        {
            @Override
            public Boolean compute()
            {
                try
                {
                    buildInternal();
                    return true;
                }
                catch( ModuleBuildException e )
                {
                    LOG.warn( "Module build exception: " + e.getMessage(), e );
                    BuildMessages.getInstance( getProject() ).showError(
                            getModule(),
                            getModule().getModuleFile(),
                            e.getMessage()
                    );
                    return false;
                }
                catch( Exception e )
                {
                    LOG.warn( "Module build exception: " + e.getMessage(), e );
                    BuildMessages.getInstance( getProject() ).showError(
                            getModule(),
                            getModule().getModuleFile(),
                            "Could not build module: " + e.getMessage()
                    );
                    return false;
                }
            }
        };

        if( ApplicationManager.getApplication().isDispatchThread() )
        {
            return ApplicationManager.getApplication().runWriteAction( writeAction );
        }
        else
        {
            final AtomicBoolean result = new AtomicBoolean( true );
            ApplicationManager.getApplication().invokeAndWait( new Runnable()
            {
                @Override
                public void run()
                {
                    result.set( ApplicationManager.getApplication().runWriteAction( writeAction ) );
                }
            }, ModalityState.any() );
            return result.get();
        }
    }

    private void buildInternal() throws MavenProcessCanceledException
    {
        MavenProject mavenProject = MavenProjectsManager.getInstance( getProject() ).findProject( getModule() );
        if( mavenProject == null )
        {
            throw new ModuleBuildException( "Module no longer a Maven project", getModule() );
        }

        Path pomFile = VfsUtil.virtualToIoFile( mavenProject.getFile() ).toPath();
        if( !Files.exists( pomFile ) )
        {
            throw new ModuleBuildException( "Module no longer a Maven project", getModule() );
        }

        try
        {
            Path bundleFile = Paths.get( mavenProject.getBuildDirectory() ).resolve( getBundleFileName( mavenProject ) );
            if( Files.exists( bundleFile ) )
            {
                long bundleFileModFile = Files.getLastModifiedTime( bundleFile ).toMillis();
                long pomFileModFile = Files.getLastModifiedTime( pomFile ).toMillis();
                if( bundleFileModFile > pomFileModFile )
                {
                    Path outputDir = Paths.get( mavenProject.getOutputDirectory() );
                    if( Files.exists( outputDir ) )
                    {
                        long latestResourceModTime = findLatestFileModificationTime( outputDir );
                        if( bundleFileModFile + 1000 >= latestResourceModTime )
                        {
                            return;
                        }
                    }
                }
            }
        }
        catch( IOException e )
        {
            BuildMessages.getInstance( getProject() ).showError(
                    getModule(),
                    new OpenFileDescriptor( getProject(), mavenProject.getFile(), 1, 1 ),
                    "Could not scan for modifications - forced build"
            );
        }

        // build bundle
        MavenEmbedderWrapper embedder = MavenServerManager.getInstance().createEmbedder( getProject(), false );
        try
        {
            ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
            if( progressIndicator == null )
            {
                progressIndicator = new EmptyProgressIndicator();
            }

            MavenConsole console = new SoutMavenConsole();
            MavenWorkspaceMap workspaceMap = new MavenWorkspaceMap();
            MavenProgressIndicator indicator = new MavenProgressIndicator( progressIndicator );
            embedder.customizeForStrictResolve( workspaceMap, console, indicator );

            Collection<String> profiles = new LinkedHashSet<>();
            profiles.addAll( MavenProjectsManager.getInstance( getProject() ).getExplicitProfiles() );
            profiles.add( "intellij" );

            MavenServerExecutionResult result = embedder.execute( mavenProject.getFile(), profiles, asList( "bundle:bundle" ) );
            if( !result.problems.isEmpty() )
            {
                for( MavenProjectProblem problem : result.problems )
                {
                    BuildMessages.getInstance( getProject() ).showError(
                            getModule(),
                            new OpenFileDescriptor( getProject(), mavenProject.getFile(), 1, 1 ),
                            problem.getDescription()
                    );
                }
                throw new ModuleBuildException( "Could not build module (see problems above)", getModule() );
            }
        }
        finally
        {
            embedder.release();
        }
    }

    private long findLatestFileModificationTime( Path outputDirectory ) throws IOException
    {
        final AtomicLong highestClassModificationTime = new AtomicLong( 0 );
        Files.walkFileTree( outputDirectory, new SimpleFileVisitor<Path>()
        {
            @Override
            @NotNull
            public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException
            {
                long modificationStamp = attrs.lastModifiedTime().toMillis();
                if( modificationStamp > highestClassModificationTime.get() )
                {
                    highestClassModificationTime.set( modificationStamp );
                }
                return FileVisitResult.CONTINUE;
            }
        } );
        return highestClassModificationTime.get();
    }

    private String getBundleFileName( MavenProject mavenProject )
    {
        String extension = mavenProject.getPackaging();
        if( extension.isEmpty() || "bundle".equals( extension ) || "pom".equals( extension ) )
        {
            extension = "jar"; // just in case maven gets confused
        }
        return mavenProject.getFinalName() + '.' + extension;
    }

    private Project getProject()
    {
        return getModule().getProject();
    }
}
