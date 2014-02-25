package org.mosaic.development.idea.run;

import com.intellij.diagnostic.logging.LogConfigurationPanel;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mosaic.development.idea.server.MosaicServer;
import org.mosaic.development.idea.server.MosaicServerManager;

/**
 * @author arik
 */
public class MosaicRunConfiguration extends RunConfigurationBase
{
    private String serverName;

    private int jmxPort = 7040;

    private String appsLocation;

    private String etcLocation;

    private String vmOptions;

    private DeploymentUnit[] deploymentUnits;

    public MosaicRunConfiguration( Project project,
                                   ConfigurationFactory factory,
                                   String name )
    {
        super( project, factory, name );
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor()
    {
        SettingsEditorGroup<MosaicRunConfiguration> group = new SettingsEditorGroup<>();
        group.addEditor( "Configuration", new GeneralConfigurationSettingsEditor() );
        group.addEditor( "Deployment", new DeploymentSettingsEditor( getProject() ) );
        JavaRunConfigurationExtensionManager.getInstance().appendEditors( this, group );
        group.addEditor( ExecutionBundle.message( "logs.tab.title" ), new LogConfigurationPanel<MosaicRunConfiguration>() );
        return group;
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException
    {
        // check server exists
        if( this.serverName == null || this.serverName.trim().isEmpty() )
        {
            throw new RuntimeConfigurationError( "Please select a Mosaic server instance." );
        }
        else
        {
            MosaicServer server = MosaicServerManager.getInstance().getServer( this.serverName );
            if( server == null )
            {
                throw new RuntimeConfigurationError( "No such Mosaic server: " + this.serverName );
            }

            File location = new File( server.getLocation() );
            if( !location.exists() )
            {
                throw new RuntimeConfigurationError( "Mosaic server directory at '" + location.getPath() + "' does not exist" );
            }
            else if( !location.isDirectory() )
            {
                throw new RuntimeConfigurationError( "Mosaic server directory at '" + location.getPath() + "' is not an actual directory" );
            }
        }

        // if apps directory specified, check if it exists and is an actual directory
        if( this.appsLocation != null && !this.appsLocation.trim().isEmpty() )
        {
            File dir = new File( this.appsLocation );
            if( !dir.exists() )
            {
                throw new RuntimeConfigurationError( "Location of applications directory does not exist" );
            }
            else if( !dir.isDirectory() )
            {
                throw new RuntimeConfigurationError( "Location of applications directory is not an actual directory" );
            }
        }

        // if etc directory specified, check if it exists and is an actual directory
        if( this.etcLocation != null && !this.etcLocation.trim().isEmpty() )
        {
            File dir = new File( this.etcLocation );
            if( !dir.exists() )
            {
                throw new RuntimeConfigurationError( "Location of configurations directory does not exist" );
            }
            else if( !dir.isDirectory() )
            {
                throw new RuntimeConfigurationError( "Location of configurations directory is not an actual directory" );
            }
        }

        // WARNINGS - MUST BE CHECKED ONLY AFTER ABOVE "ERROR" CHECKS ARE DONE

        // if no apps dir specified, warn user about it
        if( this.appsLocation == null || this.appsLocation.trim().isEmpty() )
        {
            throw new RuntimeConfigurationWarning( "Please select location of applications directory." );
        }

        // if no etc dir specified, warn user about it
        if( this.etcLocation == null || this.etcLocation.trim().isEmpty() )
        {
            throw new RuntimeConfigurationWarning( "Please select location of configurations directory." );
        }

        // if no deployment units specified, warn user about it
        if( this.deploymentUnits == null || this.deploymentUnits.length == 0 )
        {
            throw new RuntimeConfigurationWarning( "No deployment units selected." );
        }
    }

    @Nullable
    @Override
    public RunProfileState getState( @NotNull Executor executor, @NotNull ExecutionEnvironment env )
            throws ExecutionException
    {
        MosaicRunProfileState state = new MosaicRunProfileState( env, this );
        state.setConsoleBuilder( TextConsoleBuilderFactory.getInstance().createBuilder( getProject() ) );
        return state;
    }

    @Override
    public void readExternal( Element element ) throws InvalidDataException
    {
        super.readExternal( element );

        this.serverName = element.getAttributeValue( "serverName" );

        String jmxPortValue = element.getAttributeValue( "jmxPort" );
        try
        {
            this.jmxPort = jmxPortValue == null ? 7040 : Integer.parseInt( jmxPortValue );
        }
        catch( NumberFormatException e )
        {
            this.jmxPort = 7040;
        }

        this.appsLocation = element.getAttributeValue( "appsLocation" );
        this.etcLocation = element.getAttributeValue( "etcLocation" );
        this.vmOptions = element.getAttributeValue( "vmOptions" );

        Element unitsElt = element.getChild( "units" );
        if( unitsElt != null )
        {
            List<DeploymentUnit> units = new LinkedList<>();
            for( Element unitElt : unitsElt.getChildren( "unit" ) )
            {
                DeploymentUnit.Type type = DeploymentUnit.Type.valueOf( unitElt.getAttributeValue( "type" ) );
                units.add( DeploymentUnit.unit( type, unitElt.getAttributeValue( "value" ) ) );
            }
            this.deploymentUnits = units.toArray( new DeploymentUnit[ units.size() ] );
        }
        else
        {
            this.deploymentUnits = new DeploymentUnit[ 0 ];
        }
    }

    @Override
    public void writeExternal( Element element ) throws WriteExternalException
    {
        super.writeExternal( element );

        element.setAttribute( "serverName", this.serverName );
        element.setAttribute( "jmxPort", this.jmxPort + "" );
        element.setAttribute( "appsLocation", this.appsLocation );
        element.setAttribute( "etcLocation", this.etcLocation );
        element.setAttribute( "vmOptions", this.vmOptions );

        if( this.deploymentUnits != null && this.deploymentUnits.length > 0 )
        {
            Element unitsElt = new Element( "units" );
            for( DeploymentUnit unit : this.deploymentUnits )
            {
                Element unitElt = new Element( "unit" );
                unitElt.setAttribute( "type", unit.getType().name() );
                unitElt.setAttribute( "value", unit.getName() );
                unitsElt.addContent( unitElt );
            }
            element.addContent( unitsElt );
        }
    }

    public String getServerName()
    {
        return serverName;
    }

    public void setServerName( String serverName )
    {
        this.serverName = serverName;
    }

    public int getJmxPort()
    {
        return jmxPort;
    }

    public void setJmxPort( int jmxPort )
    {
        this.jmxPort = jmxPort;
    }

    public String getAppsLocation()
    {
        return appsLocation;
    }

    public void setAppsLocation( String appsLocation )
    {
        this.appsLocation = appsLocation;
    }

    public String getEtcLocation()
    {
        return etcLocation;
    }

    public void setEtcLocation( String etcLocation )
    {
        this.etcLocation = etcLocation;
    }

    public String getVmOptions()
    {
        return vmOptions;
    }

    public void setVmOptions( String vmOptions )
    {
        this.vmOptions = vmOptions;
    }

    public DeploymentUnit[] getDeploymentUnits()
    {
        return this.deploymentUnits;
    }

    public void setDeploymentUnits( DeploymentUnit[] deploymentUnits )
    {
        this.deploymentUnits = deploymentUnits;
    }
}
