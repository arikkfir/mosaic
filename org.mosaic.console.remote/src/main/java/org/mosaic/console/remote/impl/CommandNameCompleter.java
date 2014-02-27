package org.mosaic.console.remote.impl;

import java.util.List;
import javax.annotation.Nonnull;
import org.mosaic.console.CommandManager;
import org.mosaic.modules.Service;

/**
 * @author arik
 */
final class CommandNameCompleter implements jline.console.completer.Completer
{
    @Nonnull
    @Service
    private CommandManager commandManager;

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates )
    {
        if( buffer == null || buffer.isEmpty() )
        {
            for( CommandManager.CommandDescriptor commandDescriptor : this.commandManager.getCommands() )
            {
                for( String commandName : commandDescriptor.getNames() )
                {
                    candidates.add( commandName );
                }
            }
        }
        else
        {
            for( CommandManager.CommandDescriptor commandDescriptor : this.commandManager.getCommands() )
            {
                for( String commandName : commandDescriptor.getNames() )
                {
                    if( commandName.startsWith( buffer ) )
                    {
                        candidates.add( commandName );
                    }
                }
            }
        }

        if( candidates.size() == 1 )
        {
            candidates.set( 0, candidates.get( 0 ) + " " );
        }

        return candidates.isEmpty() ? -1 : 0;
    }
}
