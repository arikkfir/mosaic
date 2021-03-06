package org.mosaic.development.idea.run.impl;

import com.intellij.execution.*;
import com.intellij.execution.configurations.JavaCommandLineState;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.compiler.CompilationStatusAdapter;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileMoveEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.mosaic.development.idea.make.BuildBundlesTaskFactory;
import org.mosaic.development.idea.run.DeploymentUnit;
import org.mosaic.development.idea.server.MosaicServer;
import org.mosaic.development.idea.server.MosaicServerManager;

import static com.intellij.openapi.util.io.FileUtil.copy;
import static com.intellij.openapi.util.io.FileUtil.copyDirContent;
import static com.intellij.openapi.vfs.VfsUtilCore.virtualToIoFile;
import static java.util.Arrays.asList;
import static java.util.Collections.addAll;

/**
 * @author arik
 */
public class MosaicRunProfileState extends JavaCommandLineState
{
    private static final Logger LOG = Logger.getInstance( MosaicRunProfileState.class );

    @NotNull
    private final MosaicRunConfiguration runConfiguration;

    public MosaicRunProfileState( @NotNull ExecutionEnvironment environment,
                                  @NotNull MosaicRunConfiguration runConfiguration )
    {
        super( environment );
        this.runConfiguration = runConfiguration;
    }

    @Override
    @NotNull
    public ExecutionResult execute( @NotNull final Executor executor, @NotNull ProgramRunner runner )
            throws ExecutionException
    {
        ExecutionResult result = super.execute( executor, runner );

        final ProcessHandler processHandler = result.getProcessHandler();
        if( processHandler != null )
        {
            processHandler.addProcessListener(
                    new MosaicProcessAdapter( this.runConfiguration.getAppsLocation(),
                                              this.runConfiguration.getEtcLocation() )
            );
        }
        return result;
    }

    @Override
    protected JavaParameters createJavaParameters() throws ExecutionException
    {
        JavaParameters params = new JavaParameters();
        params.setDefaultCharset( this.runConfiguration.getProject() );
        params.setWorkingDirectory( getServerLocation() );
        params.setJdk( ProjectRootManager.getInstance( this.runConfiguration.getProject() ).getProjectSdk() );
        params.getVMParametersList().addParametersString( this.runConfiguration.getVmOptions() );
        params.getVMParametersList().add( "-XX:-OmitStackTraceInFastThrow" );
        params.getVMParametersList().add( "-XX:+PrintCommandLineFlags" );
        params.getVMParametersList().add( "-XX:-UseSplitVerifier" );
        params.getVMParametersList().addProperty( "dev", "true" );
        params.getVMParametersList().addProperty( "com.sun.management.jmxremote.port", this.runConfiguration.getJmxPort() + "" );
        params.getVMParametersList().addProperty( "com.sun.management.jmxremote.authenticate", "false" );
        params.getVMParametersList().addProperty( "networkaddress.cache.ttl", "0" );
        params.getVMParametersList().addProperty( "networkaddress.cache.negative.ttl", "0" );
        params.getVMParametersList().addProperty( "java.net.preferIPv4Stack", "true" );
        params.setMainClass( "org.mosaic.launcher.Mosaic" );
        params.getClassPath().add( getLauncherJar() );

        File systemDir = new File( PathManager.getSystemPath() );
        File mosaicPluginDir = new File( systemDir, "mosaic" );
        File runDir = getServerDir( mosaicPluginDir, "run-" + this.runConfiguration.getName() );
        File apps = getServerDir( runDir, "apps" );
        File etc = getServerDir( runDir, "etc" );
        cleanAndCopyDirContents( new File( getServerLocation(), "apps" ), getServerDir( runDir, "apps" ) );
        cleanAndCopyDirContents( new File( getServerLocation(), "etc" ), getServerDir( runDir, "etc" ) );
        createOriginalJarLinks( getServerDir( runDir, "lib" ) );
        createDeployedJarLinks( getServerDir( runDir, "lib" ) );

        params.getVMParametersList().addProperty( "mosaic.home", getServerLocation().getAbsolutePath() );
        params.getVMParametersList().addProperty( "mosaic.home.apps", apps.getAbsolutePath() );
        params.getVMParametersList().addProperty( "mosaic.home.etc", etc.getAbsolutePath() );
        params.getVMParametersList().addProperty( "mosaic.home.lib", getServerDir( runDir, "lib" ).getAbsolutePath() );
        params.getVMParametersList().addProperty( "mosaic.home.logs", getServerDir( runDir, "logs" ).getAbsolutePath() );
        params.getVMParametersList().addProperty( "mosaic.home.work", getServerDir( runDir, "work" ).getAbsolutePath() );

        PathMacroManager pathMacroManager = PathMacroManager.getInstance( this.runConfiguration.getProject() );
        try
        {
            String srcAppsLocation = pathMacroManager.expandPath( this.runConfiguration.getAppsLocation() );
            String srcEtcLocation = pathMacroManager.expandPath( this.runConfiguration.getEtcLocation() );
            copyDirContent( new File( srcAppsLocation ), getServerDir( runDir, "apps" ) );
            copyDirContent( new File( srcEtcLocation ), getServerDir( runDir, "etc" ) );
        }
        catch( IOException e )
        {
            throw new ExecutionException( "Could not copy apps/etc locations: " + e.getMessage(), e );
        }

        // extensions
        for( RunConfigurationExtension ext : Extensions.getExtensions( RunConfigurationExtension.EP_NAME ) )
        {
            ext.updateJavaParameters( this.runConfiguration, params, getRunnerSettings() );
        }

        return params;
    }

    @NotNull
    @Override
    protected OSProcessHandler startProcess() throws ExecutionException
    {
        OSProcessHandler handler = super.startProcess();
        JavaRunConfigurationExtensionManager.getInstance().attachExtensionsToProcess(
                this.runConfiguration, handler, getRunnerSettings() );
        return handler;
    }

    @NotNull
    private File getServerLocation() throws CantRunException
    {
        String serverName = this.runConfiguration.getServerName();
        if( serverName == null )
        {
            throw new CantRunException( "No Mosaic server selected" );
        }
        MosaicServer server = MosaicServerManager.getInstance().getServer( serverName );
        if( server == null )
        {
            throw new CantRunException( "Could not find Mosaic server named '" + serverName + "'" );
        }
        File serverLocation = new File( server.getLocation() );
        if( !serverLocation.exists() || !serverLocation.isDirectory() )
        {
            throw new CantRunException( "Server location does not exist, or is not a directory" );
        }
        return serverLocation;
    }

    @NotNull
    private File getLauncherJar() throws CantRunException
    {
        File binDir = new File( getServerLocation(), "bin" );
        if( !binDir.exists() || !binDir.isDirectory() )
        {
            throw new CantRunException( "'bin' directory in server location does not exist, or is not a directory" );
        }
        return new File( binDir, "org.mosaic.launcher.jar" );
    }

    @NotNull
    private File getServerDir( @NotNull File parent, @NotNull String name ) throws ExecutionException
    {
        File dir = new File( parent, name );
        try
        {
            Files.createDirectories( dir.toPath() );
            return dir;
        }
        catch( IOException e )
        {
            throw new ExecutionException( "Cannot create directory '" + dir.getAbsolutePath() + "': " + e.getMessage(), e );
        }
    }

    private void createOriginalJarLinks( File lib ) throws ExecutionException
    {
        try
        {
            final List<String> libs = new LinkedList<>();
            Files.walkFileTree( new File( getServerLocation(), "lib" ).toPath(), new SimpleFileVisitor<Path>()
            {
                @NotNull
                @Override
                public FileVisitResult visitFile( @NotNull Path file, @NotNull BasicFileAttributes attrs )
                        throws IOException
                {
                    String lcname = file.toString().toLowerCase();
                    if( lcname.endsWith( ".jar" ) || lcname.endsWith( ".jars" ) )
                    {
                        libs.add( file.toString() );
                    }
                    return FileVisitResult.CONTINUE;
                }
            } );
            Files.write( new File( lib, "original.jars" ).toPath(), libs, Charset.forName( "UTF-8" ) );
        }
        catch( IOException e )
        {
            Logger.getInstance( getClass() ).error( "Could not create Mosaic home: " + e.getMessage(), e );
            throw new ExecutionException( "Could not create Mosaic home: " + e.getMessage(), e );
        }
    }

    private void createDeployedJarLinks( File lib ) throws ExecutionException
    {
        try
        {
            List<String> libs = new LinkedList<>();

            DeploymentUnit[] units = this.runConfiguration.getDeploymentUnits();
            if( units != null )
            {
                for( DeploymentUnit unit : units )
                {
                    addAll( libs, unit.getFilePaths() );
                }
            }

            Files.write( new File( lib, "deployed.jars" ).toPath(), libs, Charset.forName( "UTF-8" ) );
        }
        catch( IOException e )
        {
            Logger.getInstance( getClass() ).error( "Could not create Mosaic home: " + e.getMessage(), e );
            throw new ExecutionException( "Could not create Mosaic home: " + e.getMessage(), e );
        }
    }

    private void cleanAndCopyDirContents( @NotNull File src, @NotNull File dst ) throws ExecutionException
    {
        try
        {
            FileUtil.delete( dst );
            FileUtil.createDirectory( dst );
            if( src.isDirectory() )
            {
                FileUtil.copyDir( src, dst );
            }
        }
        catch( IOException e )
        {
            throw new ExecutionException( "Could not copy '" + src.getAbsolutePath() + "' to '" + dst.getAbsolutePath() + "': " + e.getMessage(), e );
        }
    }

    private class MosaicProcessAdapter extends ProcessAdapter
    {
        private final MyCompilationStatusAdapter compilationStatusAdapter = new MyCompilationStatusAdapter();

        private final VirtualFileAdapter appsMonitor;

        private final VirtualFileAdapter etcMonitor;

        private MosaicProcessAdapter( String srcAppsDir, String srcEtcDir ) throws CantRunException
        {
            if( srcAppsDir != null )
            {
                this.appsMonitor = new UserProvisioningMonitor( Paths.get( srcAppsDir ),
                                                                new File( getServerLocation(), "apps" ).toPath() );
            }
            else
            {
                this.appsMonitor = null;
            }
            if( srcEtcDir != null )
            {
                this.etcMonitor = new UserProvisioningMonitor( Paths.get( srcEtcDir ),
                                                               new File( getServerLocation(), "etc" ).toPath() );
            }
            else
            {
                this.etcMonitor = null;
            }
        }

        @Override
        public void startNotified( ProcessEvent event )
        {
            Project project = MosaicRunProfileState.this.runConfiguration.getProject();
            CompilerManager.getInstance( project ).addCompilationStatusListener( this.compilationStatusAdapter );

            if( this.appsMonitor != null )
            {
                LocalFileSystem.getInstance().addVirtualFileListener( this.appsMonitor );
            }
            if( this.etcMonitor != null )
            {
                LocalFileSystem.getInstance().addVirtualFileListener( this.etcMonitor );
            }
        }

        @Override
        public void processWillTerminate( ProcessEvent event, boolean willBeDestroyed )
        {
            if( this.appsMonitor != null )
            {
                LocalFileSystem.getInstance().removeVirtualFileListener( this.appsMonitor );
            }
            if( this.etcMonitor != null )
            {
                LocalFileSystem.getInstance().removeVirtualFileListener( this.etcMonitor );
            }

            Project project = MosaicRunProfileState.this.runConfiguration.getProject();
            CompilerManager.getInstance( project ).removeCompilationStatusListener( this.compilationStatusAdapter );
        }
    }

    private class MyCompilationStatusAdapter extends CompilationStatusAdapter
    {
        @Override
        public void compilationFinished( boolean aborted,
                                         int errors,
                                         int warnings,
                                         @NotNull CompileContext compileContext )
        {
            if( !aborted && errors <= 0 )
            {
                Project project = compileContext.getProject();

                // get list of compiled modules, and sorted list of all modules (depending before dependant in sorted list)
                List<Module> affectedModules = asList( compileContext.getCompileScope().getAffectedModules() );
                List<Module> modulesToBuild = new LinkedList<>( asList( ModuleManager.getInstance( project ).getSortedModules() ) );

                // keep only modules that were compiled, but in a sorted way
                Iterator<Module> iterator = modulesToBuild.iterator();
                while( iterator.hasNext() )
                {
                    Module module = iterator.next();
                    if( !affectedModules.contains( module ) )
                    {
                        iterator.remove();
                    }
                }

                // build in background
                BuildBundlesTaskFactory buildBundlesTaskFactory = BuildBundlesTaskFactory.getInstance( project );
                Task.Backgroundable task = buildBundlesTaskFactory.createBuildBundlesTask( modulesToBuild );
                ProgressManager.getInstance().run( task );
            }
        }
    }

    private class UserProvisioningMonitor extends VirtualFileAdapter
    {
        private final Path sourceDirectory;

        private final Path targetDirectory;

        public UserProvisioningMonitor( Path sourceDirectory, Path targetDirectory )
        {
            this.sourceDirectory = sourceDirectory;
            this.targetDirectory = targetDirectory;
        }

        @Override
        public void fileCreated( VirtualFileEvent event )
        {
            copyEventFile( event );
        }

        @Override
        public void contentsChanged( VirtualFileEvent event )
        {
            copyEventFile( event );
        }

        @Override
        public void fileDeleted( VirtualFileEvent event )
        {
            File deletedSourceFile = virtualToIoFile( event.getFile() );
            File targetFileToDelete = getTargetFile( deletedSourceFile );
            if( targetFileToDelete != null )
            {
                if( !targetFileToDelete.delete() )
                {
                    LOG.error( "Could not delete file '" + targetFileToDelete + "'" );
                }
            }
        }

        @Override
        public void fileMoved( VirtualFileMoveEvent event )
        {
            // delete old file if its in source etc dir
            File deletedFile = new File( virtualToIoFile( event.getOldParent() ), event.getFileName() );
            File targetFileToDelete = getTargetFile( deletedFile );
            if( targetFileToDelete != null )
            {
                if( !targetFileToDelete.delete() )
                {
                    LOG.error( "Could not delete file '" + targetFileToDelete + "'" );
                }
            }

            // created / update new file if its in source etc dir
            copyEventFile( event );
        }

        private void copyEventFile( VirtualFileEvent event )
        {
            File sourceFile = virtualToIoFile( event.getFile() );
            File targetFile = getTargetFile( sourceFile );
            if( targetFile != null )
            {
                try
                {
                    if( sourceFile.isDirectory() )
                    {
                        if( !targetFile.mkdirs() )
                        {
                            LOG.error( "Could not create directory '" + targetFile + "'" );
                        }
                        else
                        {
                            copyDirContent( sourceFile, targetFile );
                        }
                    }
                    else
                    {
                        copy( sourceFile, targetFile );
                    }
                }
                catch( IOException e )
                {
                    LOG.error( "Could not copy file '" + sourceFile + "' to '" + targetFile + "': " + e.getMessage(), e );
                }
            }
        }

        private File getTargetFile( File sourceFile )
        {
            String path = sourceFile.getAbsolutePath();
            if( path.startsWith( this.sourceDirectory.toString() ) )
            {
                String relativePath = path.substring( this.sourceDirectory.toString().length() );
                String newPath = this.targetDirectory.toString() + "/" + relativePath;
                return new File( newPath );
            }
            else
            {
                return null;
            }
        }
    }
}
