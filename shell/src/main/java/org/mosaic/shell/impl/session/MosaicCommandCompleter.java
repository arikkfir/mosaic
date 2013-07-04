package org.mosaic.shell.impl.session;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jline.console.completer.Completer;
import org.mosaic.lifecycle.Module;
import org.mosaic.lifecycle.ModuleManager;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.BeanRef;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.mosaic.shell.impl.command.CommandExecutor;
import org.mosaic.shell.impl.command.CommandManager;

import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.Collections2.transform;

/**
 * @author arik
 */
@Bean
public class MosaicCommandCompleter implements Completer
{
    @Nonnull
    private ModuleManager moduleManager;

    @Nonnull
    private CommandManager commandsManager;

    @ServiceRef
    public void setModuleManager( @Nonnull ModuleManager moduleManager )
    {
        this.moduleManager = moduleManager;
    }

    @BeanRef
    public void setCommandsManager( @Nonnull CommandManager commandsManager )
    {
        this.commandsManager = commandsManager;
    }

    @Override
    public int complete( String buffer, int cursor, List<CharSequence> candidates )
    {
        int completionAnchorIndex = -1;

        // text up to the cursor
        String text = buffer == null || cursor < 0 ? "" : buffer.substring( 0, cursor );
        int firstSpace = text.indexOf( ' ' );
        if( firstSpace < 0 )
        {
            // find all commands starting with this text
            for( CommandExecutor adapter : this.commandsManager.getCommandExecutorsStartingWithPrefix( text ) )
            {
                candidates.add( adapter.getCommand().getName() + " " );
            }
            completionAnchorIndex = 0;
        }
        else
        {
            CommandExecutor command = this.commandsManager.getCommand( text.substring( 0, firstSpace ) );
            if( command != null )
            {
                String args = text.substring( firstSpace + 1 );
                int argsCursor = cursor - firstSpace - 1;

                int currentWordStart = 0;
                int index = args.indexOf( ' ' );
                while( index >= 0 && index < argsCursor )
                {
                    currentWordStart = index;
                    index = args.indexOf( ' ', index + 1 );
                }

                int currentWordEnd = argsCursor - currentWordStart;
                final String bundlePrefix = args.substring( currentWordStart, currentWordEnd );
                candidates.addAll(
                        transform(
                                filter(
                                        this.moduleManager.getModules(),
                                        new Predicate<Module>()
                                        {
                                            @Override
                                            public boolean apply( @Nullable Module input )
                                            {
                                                return input != null && input.getName().startsWith( bundlePrefix );
                                            }
                                        }
                                ),
                                new Function<Module, String>()
                                {
                                    @Nullable
                                    @Override
                                    public String apply( @Nullable Module input )
                                    {
                                        if( input != null )
                                        {
                                            return input.getName();
                                        }
                                        else
                                        {
                                            return "";
                                        }
                                    }
                                }
                        )
                );
                completionAnchorIndex = firstSpace + 1;
            }
        }

        // return
        return completionAnchorIndex;
    }
}
