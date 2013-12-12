package org.mosaic.console.impl.commands;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.console.Command;
import org.mosaic.console.Console;
import org.mosaic.console.spi.CommandManager;
import org.mosaic.console.util.table.SimpleColumn;
import org.mosaic.console.util.table.TablePrinter;
import org.mosaic.modules.*;
import org.mosaic.util.collections.MapEx;

import static com.google.common.base.Strings.padStart;
import static com.google.common.base.Strings.repeat;

/**
 * @author arik
 */
@Component
final class ModuleCommands
{
    @Nonnull
    @Component
    private CommandManager commandManager;

    @Command(synopsis = "list modules",
             description = "This command will list modules, possibly filtered by a given filter.")
    void list( @Nonnull
               Console console,

               @Nullable
               @Command.Option(names = { "s", "sort" },
                               defaultValue = "id",
                               synopsis = "select how modules will be sorted, either 'id', 'name' or 'version'",
                               description = "")
               String sort,

               @Nullable
               @Command.Arg(synopsis = "module names filter",
                            description = "A simple glob pattern to filter modules. Examples: 'mymodule*' or 'org.mycomp.*'.")
               String filter ) throws IOException
    {
        @SuppressWarnings("unchecked")
        TablePrinter<Module> table = new TablePrinter<>(
                console,
                new SimpleColumn<Module>( "ID", 5 )
                {
                    @Nullable
                    @Override
                    public String getValue( @Nonnull Module module )
                    {
                        return module.getId() + "";
                    }
                },
                new SimpleColumn<Module>( "Name", 60 )
                {
                    @Nullable
                    @Override
                    public String getValue( @Nonnull Module module )
                    {
                        return module.getName();
                    }
                },
                new SimpleColumn<Module>( "Version", 20 )
                {
                    @Nullable
                    @Override
                    public String getValue( @Nonnull Module module )
                    {
                        return module.getVersion().toString();
                    }
                },
                new SimpleColumn<Module>( "State", 15 )
                {
                    @Nullable
                    @Override
                    public String getValue( @Nonnull Module module )
                    {
                        return module.getState() + "";
                    }
                }
        );

        for( Module module : new ModuleMatcher().matchModules( sort, filter ) )
        {
            table.print( module );
        }
        table.endTable();
    }

    @Command(synopsis = "start modules",
             description = "This command will attempt to start the given module(s), if they have not already been started.")
    void start( @Nonnull
                Console console,

                @Nullable
                @Command.Arg(synopsis = "module names filter",
                             description = "The command will attempt to start all modules matching this filter." +
                                           "The filter is a simple glob pattern - such as 'mymodule*' or 'org.mycomp.*'.")
                String filter ) throws IOException
    {
        for( Module module : new ModuleMatcher().matchModules( null, filter ) )
        {
            try
            {
                module.startModule();
            }
            catch( ModuleStartException e )
            {
                console.printStackTrace( e );
            }
        }
    }

    @Command(synopsis = "stop modules",
             description = "This command will stop the given module(s).")
    void stop( @Nonnull
               Console console,

               @Nullable
               @Command.Arg(synopsis = "module names filter",
                            description = "The command will attempt to stop all modules matching this filter." +
                                          "The filter is a simple glob pattern - such as 'mymodule*' or 'org.mycomp.*'.")
               String filter ) throws IOException
    {
        for( Module module : new ModuleMatcher().matchModules( null, filter ) )
        {
            try
            {
                module.stopModule();
            }
            catch( ModuleStartException e )
            {
                console.printStackTrace( e );
            }
        }
    }

    @Command(synopsis = "inspect modules",
             description = "This command will inspect the given module(s).")
    void inspect( @Nonnull
                  Console console,

                  @Command.Option(names = { "a", "all" },
                                  synopsis = "show headers, packages, services and contents",
                                  description = "show all possible information on matching module(s)",
                                  defaultValue = "false")
                  boolean showAll,

                  @Command.Option(names = { "h", "headers" },
                                  synopsis = "show headers",
                                  description = "whether to show manifest headers (default is false)",
                                  defaultValue = "false")
                  boolean showHeaders,

                  @Command.Option(names = { "p", "packages" },
                                  synopsis = "show package requirements and capabilities",
                                  description = "whether to show packages this module imports and exports (default is false)",
                                  defaultValue = "false")
                  boolean showPackages,

                  @Command.Option(names = { "s", "services" },
                                  synopsis = "show services obtained and exported by this module",
                                  description = "whether to show services this module imports and exports (default is false)",
                                  defaultValue = "false")
                  boolean showServices,

                  @Command.Option(names = { "c", "contents" },
                                  synopsis = "show module contents",
                                  description = "whether to show module contents and files (default is false)",
                                  defaultValue = "false")
                  boolean showContents,

                  @Nullable
                  @Command.Arg(synopsis = "module names filter",
                               description = "The command will inspect all modules matching this filter." +
                                             "The filter is a simple glob pattern - such as 'mymodule*' or 'org.mycomp.*'.")
                  String filter ) throws IOException
    {
        for( Module module : new ModuleMatcher().matchModules( null, filter ) )
        {
            inspectModule( module,
                           console,
                           showAll || showHeaders,
                           showAll || showPackages,
                           showAll || showServices,
                           showAll || showContents );
        }
    }

    private void inspectModule( @Nonnull Module module,
                                @Nonnull Console console,
                                boolean showHeaders,
                                boolean showPackages,
                                boolean showServices,
                                boolean showContents ) throws IOException
    {
        console.println().println( "GENERAL INFORMATION" );
        console.println( "{}{}: {}", repeat( " ", 8 ), padStart( "ID", 30, ' ' ), module.getId() );
        console.println( "{}{}: {}", repeat( " ", 8 ), padStart( "Name", 30, ' ' ), module.getName() );
        console.println( "{}{}: {}", repeat( " ", 8 ), padStart( "Version", 30, ' ' ), module.getVersion() );
        console.println( "{}{}: {}", repeat( " ", 8 ), padStart( "Location", 30, ' ' ), module.getPath() );
        console.println( "{}{}: {}", repeat( " ", 8 ), padStart( "State", 30, ' ' ), module.getState() );
        console.println( "{}{}: {}", repeat( " ", 8 ), padStart( "Modification time", 30, ' ' ), module.getLastModified() );

        if( showHeaders )
        {
            console.println().println( "HEADERS" );
            for( Map.Entry<String, String> entry : module.getHeaders().entrySet() )
            {
                String headerName = entry.getKey();
                if( !"Import-Package".equals( headerName ) && !"Ignore-Package".equals( headerName ) )
                {
                    console.println( "{}{}: {}", repeat( " ", 8 ), padStart( headerName, 30, ' ' ), entry.getValue() );
                }
            }
        }

        if( showPackages )
        {
            console.println().println( "PACKAGE REQUIREMENTS" );
            for( ModuleWiring.PackageRequirement requirement : module.getModuleWiring().getPackageRequirements() )
            {
                console.println( "{}{} {} {}",
                                 repeat( " ", 8 ),
                                 requirement.getPackageName(),
                                 requirement.getFilter(),
                                 requirement.isOptional() ? "(optional)" : "" );
                Module provider = requirement.getProvider();
                if( provider != null )
                {
                    console.println( "{}Satisfied with version {} from {}",
                                     repeat( " ", 12 ),
                                     requirement.getVersion(),
                                     provider );
                }
            }

            console.println().println( "EXPORTED PACKAGES" );
            for( ModuleWiring.PackageCapability capability : module.getModuleWiring().getPackageCapabilities() )
            {
                console.println( "{}{}-{}", repeat( " ", 8 ), capability.getPackageName(), capability.getVersion() );
                Collection<Module> consumers = capability.getConsumers();
                if( !consumers.isEmpty() )
                {
                    for( Module consumer : consumers )
                    {
                        console.println( "{}Used by: {}", repeat( " ", 12 ), consumer );
                    }
                }
            }
        }

        if( showServices )
        {
            console.println().println( "SERVICE REQUIREMENTS" );
            for( ModuleWiring.ServiceRequirement requirement : module.getModuleWiring().getServiceRequirements() )
            {
                console.println( "{}{}", repeat( " ", 8 ), requirement.getType().getName() );
                console.println( "{}Filter: {}", repeat( " ", 12 ), requirement.getFilter() );
                for( ServiceReference<?> reference : requirement.getReferences() )
                {
                    console.println( "{}[{}] from {}", repeat( " ", 12 ), reference.getId(), reference.getProvider() );
                }
            }

            console.println().println( "EXPORTED SERVICES" );
            for( ModuleWiring.ServiceCapability serviceInfo : module.getModuleWiring().getServiceCapabilities() )
            {
                printServiceInfo( console, serviceInfo, true );
            }
        }

        if( showContents )
        {
            console.println().println( "CONTENTS" );
            for( URL resource : module.getModuleResources().findResources( "/**" ) )
            {
                console.println( "{}{}", repeat( " ", 8 ), resource.getPath() );
            }
        }
    }

    private void printServiceInfo( @Nonnull Console console,
                                   @Nonnull ModuleWiring.ServiceCapability capability,
                                   boolean printConsumers ) throws IOException
    {
        console.println( "{}{}[{}]", repeat( " ", 8 ), capability.getType().getName(), capability.getId() );

        MapEx<String, Object> properties = capability.getProperties();
        if( properties.isEmpty() )
        {
            console.println( "{}This service has no properties.", repeat( " ", 12 ) );
        }
        else
        {
            console.println( "{}Properties:", repeat( " ", 12 ) );
            for( Map.Entry<String, Object> entry : properties.entrySet() )
            {
                console.println( "{}{}: {}", repeat( " ", 16 ), padStart( entry.getKey(), 15, ' ' ), entry.getValue() );
            }
        }

        if( printConsumers )
        {
            Collection<Module> consumers = capability.getConsumers();
            if( consumers.isEmpty() )
            {
                console.println( "{}No module consumes this service.", repeat( " ", 12 ) );
            }
            else
            {
                console.println( "{}Consumers:", repeat( " ", 12 ) );
                for( Module consumer : consumers )
                {
                    console.println( "{}{}", repeat( " ", 16 ), consumer );
                }
            }
        }
    }
}
