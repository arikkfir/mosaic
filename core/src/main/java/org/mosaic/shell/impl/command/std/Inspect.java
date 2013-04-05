package org.mosaic.shell.impl.command.std;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import javax.annotation.Nonnull;
import org.mosaic.lifecycle.Module;
import org.mosaic.lifecycle.ModuleManager;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.mosaic.shell.Console;
import org.mosaic.shell.annotation.*;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.leftPad;

/**
 * @author arik
 */
@Bean
public class Inspect
{
    private ModuleManager moduleManager;

    @ServiceRef
    public void setModuleManager( ModuleManager moduleManager )
    {
        this.moduleManager = moduleManager;
    }

    @Command(name = "inspect-module", label = "Inspect module(s)", desc = "Inspects and show information about installed modules.")
    public void inspectModule( @Nonnull Console console,

                               @Option @Alias("e") @Desc("use exact matching of module names")
                               boolean exact,

                               @Option @Alias("h") @Desc("show module headers")
                               boolean showHeaders,

                               @Option @Alias("s") @Desc("show module service declarations and usages")
                               boolean showServices,

                               @Option @Alias("p") @Desc("show module package imports and exports")
                               boolean showPackages,

                               @Option @Alias("c") @Desc("show module content")
                               boolean showContents,

                               @Nonnull @Arguments String... moduleNames ) throws IOException
    {
        Collection<Module> knownModules = this.moduleManager.getModules();
        for( String moduleName : moduleNames )
        {
            for( Module module : knownModules )
            {
                if( matches( module, moduleName, exact ) )
                {
                    displayModuleInfo( console, module );

                    if( showHeaders )
                    {
                        displayModuleHeaders( console, module );
                    }

                    if( showServices )
                    {
                        displayModuleServices( console, module );
                    }

                    if( showPackages )
                    {
                        displayModulePackages( console, module );
                    }

                    if( showContents )
                    {
                        displayModuleContents( console, module );
                    }
                }
            }
        }
    }

    private void displayModuleInfo( @Nonnull Console console, @Nonnull Module module ) throws IOException
    {
        console.println();
        console.println( "GENERAL INFORMATION" );
        console.print( 8, leftPad( "ID", 30 ) ).print( ": " ).println( module.getId() );
        console.print( 8, leftPad( "Name", 30 ) ).print( ": " ).println( module.getName() );
        console.print( 8, leftPad( "Version", 30 ) ).print( ": " ).println( module.getVersion() );
        console.print( 8, leftPad( "Location", 30 ) ).print( ": " ).println( module.getPath() );
        console.print( 8, leftPad( "State", 30 ) ).print( ": " ).println( capitalize( module.getState().name().toLowerCase() ) );
        console.print( 8, leftPad( "Modification time", 30 ) ).print( ": " ).println( new Date( module.getLastModified() ) );
    }

    private void displayModuleHeaders( @Nonnull Console console, @Nonnull Module module ) throws IOException
    {
        console.println();
        console.println( "HEADERS" );
        for( Map.Entry<String, String> entry : module.getHeaders().entrySet() )
        {
            String headerName = entry.getKey();
            if( !"Import-Package".equals( headerName ) && !"Ignore-Package".equals( headerName ) )
            {
                console.print( 8, leftPad( headerName, 30 ) ).print( ": " ).println( entry.getValue() );
            }
        }
    }

    private void displayModuleServices( @Nonnull Console console, @Nonnull Module module ) throws IOException
    {
        console.println();
        console.println( "EXPORTED SERVICES" );
        Collection<Module.ServiceExport> exportedServices = module.getExportedServices();
        for( Module.ServiceExport export : exportedServices )
        {
            console.println( 8, "----------------------------------------------------------------------------------" );
            console.println( 8, export.getType() );

            console.println( 8, "Properties:" );
            for( Map.Entry<String, Object> entry : export.getProperties().entrySet() )
            {
                String propertyName = entry.getKey();
                console.print( 10, leftPad( propertyName, 30 ) ).print( ": " ).println( entry.getValue() );
            }

            Collection<Module> consumers = export.getConsumers();
            if( consumers.isEmpty() )
            {
                console.println( 8, "Not used by any module" );
            }
            else
            {
                console.println( 8, "Used by:" );
                for( Module consumer : consumers )
                {
                    console.println( 30, consumer );
                }
            }
        }
        if( !exportedServices.isEmpty() )
        {
            console.println( 8, "----------------------------------------------------------------------------------" );
        }

        // TODO arik: show imported services
    }

    private void displayModulePackages( @Nonnull Console console, @Nonnull Module module ) throws IOException
    {
        // TODO arik: implement displayModulePackages([console, module])
    }

    private void displayModuleContents( @Nonnull Console console, @Nonnull Module module ) throws IOException
    {
        console.println();
        console.println( "CONTENTS" );
        for( String resource : module.getResources() )
        {
            console.println( 8, resource );
        }
    }

    private boolean matches( Module module, String moduleName, boolean exact )
    {
        if( exact && module.getName().equalsIgnoreCase( moduleName ) )
        {
            return true;
        }
        else if( !exact && module.getName().toLowerCase().contains( moduleName.toLowerCase() ) )
        {
            return true;
        }
        else
        {
            try
            {
                int moduleId = Integer.parseInt( moduleName );
                if( moduleId == module.getId() )
                {
                    return true;
                }
            }
            catch( NumberFormatException ignore )
            {
            }
        }
        return false;
    }
}
