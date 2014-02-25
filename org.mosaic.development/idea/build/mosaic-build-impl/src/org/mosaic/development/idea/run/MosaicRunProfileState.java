package org.mosaic.development.idea.run;

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
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.mosaic.development.idea.server.MosaicServer;
import org.mosaic.development.idea.server.MosaicServerManager;

/**
 * @author arik
 */
public class MosaicRunProfileState extends JavaCommandLineState
{
    @NotNull
    private final MosaicRunConfiguration runConfiguration;

    public MosaicRunProfileState( @NotNull ExecutionEnvironment environment,
                                  @NotNull MosaicRunConfiguration runConfiguration )
    {
        super( environment );
        this.runConfiguration = runConfiguration;
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
        params.getVMParametersList().addProperty( "com.sun.management.jmxremote.port", this.runConfiguration.getJmxPort() + "" );
        params.getVMParametersList().addProperty( "com.sun.management.jmxremote.authenticate", "false" );
        params.getVMParametersList().addProperty( "networkaddress.cache.ttl", "0" );
        params.getVMParametersList().addProperty( "networkaddress.cache.negative.ttl", "0" );
        params.getVMParametersList().addProperty( "java.net.preferIPv4Stack", "true" );
        params.setMainClass( "org.mosaic.launcher.Mosaic" );
        params.getClassPath().add( getLauncherJar() );

        File systemDir = new File( PathManager.getSystemPath() );
        File mosaicPluginDir = new File( systemDir, "mosaic" );
        File runDir = new File( mosaicPluginDir, "run-" + this.runConfiguration.getName() );
        File apps = new File( runDir, "apps" );
        File etc = new File( runDir, "etc" );
        File lib = new File( runDir, "lib" );
        File logs = new File( runDir, "logs" );
        File work = new File( runDir, "work" );
        try
        {
            Files.createDirectories( apps.toPath() );
            Files.createDirectories( etc.toPath() );
            Files.createDirectories( lib.toPath() );
            Files.createDirectories( logs.toPath() );
            Files.createDirectories( work.toPath() );

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

        params.getVMParametersList().addProperty( "mosaic.home", getServerLocation().getAbsolutePath() );
        params.getVMParametersList().addProperty( "mosaic.home.apps", apps.getAbsolutePath() );
        params.getVMParametersList().addProperty( "mosaic.home.etc", etc.getAbsolutePath() );
        params.getVMParametersList().addProperty( "mosaic.home.lib", lib.getAbsolutePath() );
        params.getVMParametersList().addProperty( "mosaic.home.logs", logs.getAbsolutePath() );
        params.getVMParametersList().addProperty( "mosaic.home.work", work.getAbsolutePath() );

        // extensions
        for( RunConfigurationExtension ext : Extensions.getExtensions( RunConfigurationExtension.EP_NAME ) )
        {
            ext.updateJavaParameters( this.runConfiguration, params, getRunnerSettings() );
        }

        // TODO: custom locations synchronizer
        PathMacroManager pathMacroManager = PathMacroManager.getInstance( this.runConfiguration.getProject() );
        File srcAppsLocation = new File( pathMacroManager.expandPath( this.runConfiguration.getAppsLocation() ) );
        File srcEtcLocation = new File( pathMacroManager.expandPath( this.runConfiguration.getEtcLocation() ) );

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

    @Override
    @NotNull
    public ExecutionResult execute( @NotNull final Executor executor, @NotNull ProgramRunner runner )
            throws ExecutionException
    {
        ExecutionResult result = super.execute( executor, runner );

        final ProcessHandler processHandler = result.getProcessHandler();
        if( processHandler != null )
        {
            processHandler.addProcessListener( new MosaicProcessAdapter() );
        }
        return result;
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

    private class MosaicProcessAdapter extends ProcessAdapter
    {
        @Override
        public void startNotified( ProcessEvent event )
        {
            super.startNotified( event );
        }

        @Override
        public void onTextAvailable( ProcessEvent event, Key outputType )
        {
            super.onTextAvailable( event, outputType );
        }

        @Override
        public void processWillTerminate( ProcessEvent event, boolean willBeDestroyed )
        {
            super.processWillTerminate( event, willBeDestroyed );
        }

        @Override
        public void processTerminated( ProcessEvent event )
        {
            super.processTerminated( event );
        }
    }
}
