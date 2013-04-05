package org.mosaic.shell;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface OptionsBuilder
{
    @Nonnull
    Option add( @Nonnull String shortName );

    @Nonnull
    Option require( @Nonnull String shortName );

    @Nonnull
    OptionsBuilder withExtraArguments( @Nonnull String description );

    interface Option
    {
        @Nonnull
        Option withRequiredArgument();

        @Nonnull
        Option withDescription( @Nonnull String description );

        @Nonnull
        Option withAlias( @Nonnull String alias );
    }
}
