package org.mosaic.shell;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface Command
{
    int SUCCESS = 0;

    int ILLEGAL_USAGE = -1;

    int INSUFFICIENT_CONSOLE_SPACE = -2;

    int INTERNAL_ERROR = -99;

    @Nonnull
    String getName();

    @Nonnull
    String getLabel();

    @Nullable
    String getDescription();

    void describe( @Nonnull OptionsBuilder optionsBuilder ) throws CommandDefinitionException;

    int execute( @Nonnull Console console, @Nonnull Options options ) throws Exception;
}
