package org.mosaic.development.idea.server.impl;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.JComponent;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mosaic.development.idea.server.MosaicServer;
import org.mosaic.development.idea.server.MosaicServerManager;

import static java.nio.file.Files.*;

/**
 * @author arik
 */
@State(
        name = "org.mosaic.development.idea.server.impl.MosaicServerManagerImpl",
        storages = { @Storage(file = "$APP_CONFIG$/mosaicServerManager.xml") }
)
public class MosaicServerManagerImpl extends MosaicServerManager
        implements Configurable, PersistentStateComponent<MosaicServerManagerImpl.State>
{
    public static class State
    {
        public Map<String, String> servers = new ConcurrentHashMap<>();
    }

    @NotNull
    private final Map<String, MosaicServerImpl> servers = new ConcurrentHashMap<>();

    private MosaicServerManagerForm form;

    @Override
    public void initComponent()
    {
        // no-op
    }

    @Override
    public void disposeComponent()
    {
        // no-op
    }

    @NotNull
    @Override
    public String getComponentName()
    {
        return getClass().getSimpleName();
    }

    @NotNull
    @Override
    public Collection<? extends MosaicServer> getServers()
    {
        return this.servers.values();
    }

    @Override
    @Nullable
    public MosaicServerImpl getServer( @NotNull String name )
    {
        return this.servers.get( name );
    }

    @Override
    @NotNull
    public MosaicServer addServer( @NotNull String name, @NotNull String location )
    {
        MosaicServerImpl server = this.servers.get( name );
        if( server == null )
        {
            server = new MosaicServerImpl( name, location );
            this.servers.put( name, server );
        }
        else
        {
            server.setLocation( location );
        }
        return server;
    }

    @Nls
    @Override
    public String getDisplayName()
    {
        return "Mosaic";
    }

    @Nullable
    @Override
    public String getHelpTopic()
    {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent()
    {
        if( this.form == null )
        {
            this.form = new MosaicServerManagerForm();
        }
        return this.form.getContainer();
    }

    @Override
    public boolean isModified()
    {
        return this.form.isModified( this.servers );
    }

    @Override
    public void apply() throws ConfigurationException
    {
        Map<String, String> newServers = this.form.getServers();
        Iterator<Map.Entry<String, MosaicServerImpl>> iterator = this.servers.entrySet().iterator();
        while( iterator.hasNext() )
        {
            Map.Entry<String, MosaicServerImpl> entry = iterator.next();
            String name = entry.getKey();

            if( newServers.containsKey( name ) )
            {
                entry.getValue().setLocation( newServers.get( name ) );
            }
            else
            {
                iterator.remove();
            }
        }

        for( Map.Entry<String, String> entry : newServers.entrySet() )
        {
            if( !this.servers.containsKey( entry.getKey() ) )
            {
                this.servers.put( entry.getKey(), new MosaicServerImpl( entry.getKey(), entry.getValue() ) );
            }
        }
    }

    @Override
    public void reset()
    {
        this.form.setServers( this.servers );
    }

    @Override
    public void disposeUIResources()
    {
        if( this.form != null )
        {
            this.form = null;
        }
    }

    @Nullable
    @Override
    public State getState()
    {
        State state = new State();
        for( MosaicServerImpl server : this.servers.values() )
        {
            state.servers.put( server.getName(), server.getLocation() );
        }
        return state;
    }

    @Override
    public void loadState( State state )
    {
        for( Map.Entry<String, String> entry : state.servers.entrySet() )
        {
            MosaicServerImpl server = this.servers.get( entry.getKey() );
            if( server == null )
            {
                server = new MosaicServerImpl( entry.getKey(), entry.getValue() );
                this.servers.put( server.getName(), server );
            }
            else
            {
                server.setLocation( entry.getValue() );
            }
        }
    }

    private class MosaicServerImpl extends MosaicServer
    {
        @NotNull
        private final String name;

        @NotNull
        private String location;

        private MosaicServerImpl( @NotNull String name, @NotNull String location )
        {
            this.name = name;
            this.location = location;
        }

        @NotNull
        @Override
        public String getName()
        {
            return this.name;
        }

        @NotNull
        @Override
        public String getLocation()
        {
            return this.location;
        }

        public void setLocation( @NotNull String location )
        {
            this.location = location;
        }

        @Nullable
        @Override
        public String getVersion()
        {
            Path path = Paths.get( this.location );
            if( notExists( path ) || !isDirectory( path ) )
            {
                return null;
            }

            Path versionFile = path.resolve( "version" );
            if( notExists( versionFile ) || !isRegularFile( versionFile ) )
            {
                return null;
            }

            String version;
            try
            {
                version = new String( Files.readAllBytes( versionFile ), "UTF-8" ).trim();
                return version.isEmpty() ? null : version;
            }
            catch( IOException e )
            {
                return null;
            }
        }
    }
}
