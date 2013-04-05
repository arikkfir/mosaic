package org.mosaic.shell;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface Command
{
    @Nonnull
    String getName();

    @Nonnull
    String getLabel();

    @Nullable
    String getDescription();

    void describe( @Nonnull OptionsBuilder optionsBuilder ) throws CommandDefinitionException;

    void execute( @Nonnull Console console, @Nonnull Options options ) throws Exception;
}
