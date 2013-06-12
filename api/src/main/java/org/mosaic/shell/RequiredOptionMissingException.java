package org.mosaic.shell;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class RequiredOptionMissingException extends IllegalUsageException
{
    @Nonnull
    private final String option;

    public RequiredOptionMissingException( @Nonnull String command, @Nonnull String option )
    {
        super( command, "missing option '" + option + "'" );
        this.option = option;
    }

    @Nonnull
    public String getOption()
    {
        return option;
    }
}
