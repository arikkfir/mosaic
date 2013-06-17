package org.mosaic.shell.impl.command;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.shell.OptionsBuilder;

/**
 * @author arik
 */
public class OptionsBuilderImpl implements OptionsBuilder
{
    @Nonnull
    private final Map<String, OptionImpl> options = new HashMap<>();

    @Nonnull
    private String extraArgumentsDescription = "arguments";

    @Nonnull
    public Collection<OptionImpl> getOptions()
    {
        return this.options.values();
    }

    @Nonnull
    public String getExtraArgumentsDescription()
    {
        return extraArgumentsDescription;
    }

    @Nonnull
    @Override
    public Option add( @Nonnull String shortName )
    {
        OptionImpl option = this.options.get( shortName );
        if( option == null )
        {
            option = new OptionImpl( shortName );
            this.options.put( shortName, option );
        }
        return option.notRequired();
    }

    @Nonnull
    @Override
    public Option require( @Nonnull String shortName )
    {
        OptionImpl option = this.options.get( shortName );
        if( option == null )
        {
            option = new OptionImpl( shortName );
            this.options.put( shortName, option );
        }
        return option.required();
    }

    @Nonnull
    @Override
    public OptionsBuilder withExtraArguments( @Nonnull String description )
    {
        this.extraArgumentsDescription = description;
        return this;
    }

    public class OptionImpl implements Option
    {
        @Nonnull
        private final String shortName;

        @Nullable
        private String alias;

        private boolean required;

        private boolean argumentRequired;

        @Nullable
        private String description;

        private OptionImpl( @Nonnull String shortName )
        {
            this.shortName = shortName;
        }

        @Nonnull
        public String getShortName()
        {
            return shortName;
        }

        @Nullable
        public String getAlias()
        {
            return alias;
        }

        @Nullable
        public String getDescription()
        {
            return description;
        }

        public boolean isRequired()
        {
            return required;
        }

        public boolean isArgumentRequired()
        {
            return argumentRequired;
        }

        @Nonnull
        @Override
        public Option withRequiredArgument()
        {
            this.argumentRequired = true;
            return this;
        }

        @Nonnull
        @Override
        public Option withDescription( @Nonnull String description )
        {
            this.description = description;
            return this;
        }

        @Nonnull
        @Override
        public Option withAlias( @Nonnull String alias )
        {
            this.alias = alias;
            return this;
        }

        @Nonnull
        private Option required()
        {
            this.required = true;
            return this;
        }

        @Nonnull
        private Option notRequired()
        {
            this.required = false;
            return this;
        }
    }
}
