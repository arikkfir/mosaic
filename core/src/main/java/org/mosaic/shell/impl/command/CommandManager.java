package org.mosaic.shell.impl.command;

import com.google.common.base.Function;
import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.commons.cli.*;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.lifecycle.annotation.*;
import org.mosaic.shell.Command;
import org.mosaic.shell.*;
import org.mosaic.shell.Options;
import org.mosaic.shell.annotation.*;
import org.mosaic.shell.annotation.Option;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodParameter;

import static com.google.common.collect.Collections2.transform;
import static java.util.Arrays.asList;
import static java.util.regex.Pattern.quote;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;

/**
 * @author arik
 */
@Bean
public class CommandManager
{
    @Nonnull
    private ConversionService conversionService;

    @Nullable
    private Map<Long, CommandExecutor> commands;

    @ServiceRef
    public void setConversionService( @Nonnull ConversionService conversionService )
    {
        this.conversionService = conversionService;
    }

    @ServiceBind
    public void addCommand( @Nonnull MethodEndpoint endpoint, @ServiceProperty(ServiceProperties.ID) long id )
    {
        Map<Long, CommandExecutor> commands = this.commands;
        if( commands != null && endpoint.getType().annotationType().equals( org.mosaic.shell.annotation.Command.class ) )
        {
            this.commands.put( id, new StandardCommandExecutor( new MethodEndpointCommandAdapter( endpoint ) ) );
        }
    }

    @ServiceUnbind
    public void removeCommand( @Nonnull MethodEndpoint endpoint, @ServiceProperty(ServiceProperties.ID) long id )
    {
        Map<Long, CommandExecutor> commands = this.commands;
        if( commands != null && endpoint.getType().annotationType().equals( org.mosaic.shell.annotation.Command.class ) )
        {
            this.commands.remove( id );
        }
    }

    @ServiceBind
    public void addCommand( @Nonnull Command command, @ServiceProperty(ServiceProperties.ID) long id )
    {
        Map<Long, CommandExecutor> commands = this.commands;
        if( commands != null )
        {
            this.commands.put( id, new StandardCommandExecutor( command ) );
        }
    }

    @ServiceUnbind
    public void removeCommand( Command command, @ServiceProperty(ServiceProperties.ID) long id )
    {
        Map<Long, CommandExecutor> commands = this.commands;
        if( commands != null )
        {
            this.commands.remove( id );
        }
    }

    public int execute( @Nonnull Console console, @Nonnull String line )
            throws IOException, CommandDefinitionException, IllegalUsageException, CommandExecutionException
    {
        String[] tokens = line.split( " " );

        String[] args;
        if( tokens.length == 1 )
        {
            args = new String[ 0 ];
        }
        else
        {
            args = new String[ tokens.length - 1 ];
            System.arraycopy( tokens, 1, args, 0, args.length );
        }

        CommandExecutor adapter = getCommand( tokens[ 0 ] );
        if( adapter == null )
        {
            console.println( "Unknown command: " + tokens[ 0 ] );
            return 4;
        }
        else
        {
            return adapter.execute( console, args );
        }
    }

    @Nullable
    public CommandExecutor getCommand( @Nonnull String name )
    {
        if( this.commands != null )
        {
            for( CommandExecutor command : this.commands.values() )
            {
                if( command.getCommand().getName().equals( name ) )
                {
                    return command;
                }
            }
        }
        return null;
    }

    @Nonnull
    public Set<CommandExecutor> getCommandExecutorsStartingWithPrefix( @Nonnull String prefix )
    {
        Set<CommandExecutor> commands = new HashSet<>();
        if( this.commands != null )
        {
            List<Pattern> patterns = new LinkedList<>();
            patterns.add( Pattern.compile( quote( prefix.toLowerCase() ) + ".*" ) );
            if( !prefix.contains( ":" ) )
            {
                patterns.add( Pattern.compile( ".*:" + quote( prefix.toLowerCase() ) + ".*" ) );
            }

            for( CommandExecutor command : this.commands.values() )
            {
                for( Pattern pattern : patterns )
                {
                    if( pattern.matcher( command.getCommand().getName().toLowerCase() ).matches() )
                    {
                        commands.add( command );
                    }
                }
            }
        }
        return commands;
    }

    @Nonnull
    public Set<CommandExecutor> getCommandExecutors()
    {
        Set<CommandExecutor> commands = new HashSet<>();
        if( this.commands != null )
        {
            commands.addAll( this.commands.values() );
        }
        return commands;
    }

    @PostConstruct
    public void init()
    {
        this.commands = new ConcurrentHashMap<>();
    }

    @PreDestroy
    public void destroy()
    {
        this.commands = null;
    }

    private void printHelp( @Nonnull Console console,
                            @Nonnull Command command,
                            @Nonnull org.apache.commons.cli.Options options ) throws IOException
    {
        HelpFormatter helpFormatter = new HelpFormatter();
        StringWriter usage = new StringWriter( 200 );
        helpFormatter.printUsage( new PrintWriter( usage, true ), console.getWidth() - 9, command.getName(), options );

        console.println();
        console.println( "NAME" );
        console.print( 8, command.getName() ).print( " - " ).print( command.getLabel() );
        console.println();
        console.println( "SYNOPSIS" );
        console.println( 8, usage.toString().trim() );
        console.println();

        String description = command.getDescription();
        if( description != null )
        {
            console.println( "DESCRIPTION" );
            console.println( 8, description );
            console.println();
        }

        if( !options.getOptions().isEmpty() )
        {
            StringWriter optionsBuffer = new StringWriter( 1000 );
            helpFormatter.printOptions( new PrintWriter( optionsBuffer, true ),
                                        console.getWidth() - 9,
                                        options,
                                        0, 4 );

            console.println( "OPTIONS" );
            console.println( 8, optionsBuffer.toString().trim() );
            console.println();

        }
    }

    private void printUsage( @Nonnull Console console,
                             @Nonnull Command command,
                             @Nonnull org.apache.commons.cli.Options options )
    {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printUsage( new PrintWriter( console.getWriter(), true ),
                                  console.getWidth(),
                                  command.getName(),
                                  options );
    }

    @Nonnull
    private String getLongOptionNameForParameter( MethodParameter parameter )
    {
        StringBuilder buf = new StringBuilder( 100 );
        for( String token : splitByCharacterTypeCamelCase( parameter.getName() ) )
        {
            if( buf.length() > 0 )
            {
                buf.append( "-" );
            }
            buf.append( token );
        }
        return buf.toString();
    }

    private class StandardCommandExecutor implements CommandExecutor
    {
        @Nonnull
        private final Command command;

        private StandardCommandExecutor( @Nonnull Command command )
        {
            this.command = command;
        }

        @Nonnull
        @Override
        public Command getCommand()
        {
            return this.command;
        }

        @Override
        public void printHelp( @Nonnull Console console ) throws CommandDefinitionException, IOException
        {
            CommandManager.this.printHelp( console, this.command, getCommandCliOptions() );
        }

        @Override
        public void printUsage( @Nonnull Console console ) throws CommandDefinitionException
        {
            CommandManager.this.printUsage( console, this.command, getCommandCliOptions() );
        }

        @Override
        public int execute( @Nonnull Console console, @Nonnull String... arguments )
                throws CommandDefinitionException, IllegalUsageException, CommandExecutionException, IOException
        {
            org.apache.commons.cli.Options cliOptions = getCommandCliOptions();
            try
            {
                CommandLineParser parser = new PosixParser();
                final CommandLine cmd = parser.parse( cliOptions, arguments, false );
                return this.command.execute( console, new OptionsImpl( cliOptions, cmd, console ) );
            }
            catch( IllegalUsageException | RequiredOptionMissingException | ParseException e )
            {
                printUsage( console );
                console.println( e.getMessage() );
                return 2;
            }
            catch( InsufficientConsoleWidthException e )
            {
                console.println( e.getMessage() );
                return 3;
            }
            catch( CommandExecutionException e )
            {
                throw e;
            }
            catch( Exception e )
            {
                throw new CommandExecutionException( this.command.getName(), 1, e );
            }
        }

        private org.apache.commons.cli.Options getCommandCliOptions() throws CommandDefinitionException
        {
            // allow the command to describe itself with mosaic api
            OptionsBuilderImpl optionsBuilder = new OptionsBuilderImpl();
            this.command.describe( optionsBuilder );

            // translate to commons-cli structure
            final org.apache.commons.cli.Options cliOptions = new org.apache.commons.cli.Options();
            for( OptionsBuilderImpl.OptionImpl option : optionsBuilder.getOptions() )
            {
                org.apache.commons.cli.Option cliOption;
                if( option.getAlias() != null )
                {
                    cliOption = new org.apache.commons.cli.Option( option.getAlias(), option.getShortName(), option.isArgumentRequired(), option.getDescription() );
                }
                else
                {
                    cliOption = new org.apache.commons.cli.Option( option.getShortName(), option.isArgumentRequired(), option.getDescription() );
                }

                if( cliOption.hasArg() )
                {
                    cliOption.setArgName( "arg" );
                }

                cliOption.setRequired( option.isRequired() );

                cliOptions.addOption( cliOption );
            }
            return cliOptions;
        }

        private class OptionsImpl implements Options
        {
            @Nonnull
            private final org.apache.commons.cli.Options options;

            @Nonnull
            private final CommandLine cmd;

            @Nonnull
            private final Console console;

            public OptionsImpl( @Nonnull org.apache.commons.cli.Options options,
                                @Nonnull CommandLine cmd,
                                @Nonnull Console console )
            {
                this.options = options;
                this.cmd = cmd;
                this.console = console;
            }

            @Override
            public void printHelp() throws IOException
            {
                CommandManager.this.printHelp( this.console, command, this.options );
            }

            @Override
            public void printUsage()
            {
                CommandManager.this.printUsage( this.console, command, this.options );
            }

            @Override
            public boolean has( @Nonnull String option )
            {
                return cmd.hasOption( option );
            }

            @Nullable
            @Override
            public String get( @Nonnull String option )
            {
                return cmd.getOptionValue( option );
            }

            @Nonnull
            @Override
            public String get( @Nonnull String option, @Nonnull String defaultValue )
            {
                return cmd.getOptionValue( option, defaultValue );
            }

            @Nullable
            @Override
            public <T> T get( @Nonnull String option, @Nonnull TypeToken<T> typeToken )
            {
                String value = get( option );
                return value != null ? conversionService.convert( value, typeToken ) : null;
            }

            @Nonnull
            @Override
            public <T> T get( @Nonnull String option,
                              @Nonnull String defaultValue,
                              @Nonnull TypeToken<T> typeToken )
            {
                String value = get( option, defaultValue );
                return conversionService.convert( value, typeToken );
            }

            @Nonnull
            @Override
            public Collection<String> getAll( @Nonnull String option )
            {
                return asList( cmd.getOptionValues( option ) );
            }

            @Nonnull
            @Override
            public <T> Collection<T> getAll( @Nonnull String option, @Nonnull final TypeToken<T> typeToken )
            {
                return transform( asList( cmd.getOptionValues( option ) ),
                                  new Function<String, T>()
                                  {
                                      @Nullable
                                      @Override
                                      public T apply( @Nullable String input )
                                      {
                                          if( input == null )
                                          {
                                              return null;
                                          }
                                          else
                                          {
                                              return conversionService.convert( input, typeToken );
                                          }
                                      }
                                  } );
            }

            @Nonnull
            @Override
            public String require( @Nonnull String option ) throws RequiredOptionMissingException
            {
                String value = get( option );
                if( value == null )
                {
                    throw new RequiredOptionMissingException( command.getName(), option );
                }
                else
                {
                    return value;
                }
            }

            @Nonnull
            @Override
            public <T> T require( @Nonnull String option, @Nonnull TypeToken<T> typeToken )
                    throws RequiredOptionMissingException
            {
                T value = get( option, typeToken );
                if( value == null )
                {
                    throw new RequiredOptionMissingException( command.getName(), option );
                }
                else
                {
                    return value;
                }
            }

            @Nonnull
            @Override
            public List<String> getExtraArguments()
            {
                return asList( cmd.getArgs() );
            }
        }
    }

    private class MethodEndpointCommandAdapter implements Command, MethodHandle.ParameterResolver
    {
        @Nonnull
        private final MethodEndpoint endpoint;

        @Nonnull
        private final MethodEndpoint.Invoker invoker;

        private MethodEndpointCommandAdapter( @Nonnull MethodEndpoint endpoint )
        {
            this.endpoint = endpoint;
            this.invoker = this.endpoint.createInvoker( this );
        }

        @Nonnull
        @Override
        public String getName()
        {
            String prefix = this.endpoint.getModule().getName() + ":";
            org.mosaic.shell.annotation.Command commandAnn = ( org.mosaic.shell.annotation.Command ) this.endpoint.getType();
            if( !commandAnn.name().isEmpty() )
            {
                return prefix + commandAnn.name();
            }
            else
            {
                return prefix + this.endpoint.getName();
            }
        }

        @Nonnull
        @Override
        public String getLabel()
        {
            org.mosaic.shell.annotation.Command commandAnn = ( org.mosaic.shell.annotation.Command ) this.endpoint.getType();
            if( !commandAnn.label().isEmpty() )
            {
                return commandAnn.label();
            }
            else
            {
                return getName();
            }
        }

        @Nullable
        @Override
        public String getDescription()
        {
            org.mosaic.shell.annotation.Command commandAnn = ( org.mosaic.shell.annotation.Command ) this.endpoint.getType();
            if( !commandAnn.desc().isEmpty() )
            {
                return commandAnn.desc();
            }
            else
            {
                return null;
            }
        }

        @Override
        public void describe( @Nonnull OptionsBuilder optionsBuilder ) throws CommandDefinitionException
        {
            for( MethodParameter parameter : this.endpoint.getParameters() )
            {
                String longOptionName = getLongOptionNameForParameter( parameter );

                Option ann = parameter.getAnnotation( Option.class );
                if( ann != null )
                {
                    OptionsBuilder.Option opt;
                    if( parameter.hasAnnotation( Required.class ) )
                    {
                        opt = optionsBuilder.require( longOptionName );
                    }
                    else
                    {
                        opt = optionsBuilder.add( longOptionName );
                    }

                    Alias aliasAnn = parameter.getAnnotation( Alias.class );
                    if( aliasAnn != null )
                    {
                        opt.withAlias( aliasAnn.value() );
                    }

                    Desc descAnn = parameter.getAnnotation( Desc.class );
                    if( descAnn != null )
                    {
                        opt.withDescription( descAnn.value() );
                    }

                    TypeToken<?> type = parameter.getType();
                    if( boolean.class.equals( type.getRawType() ) || Boolean.class.equals( type.getRawType() ) )
                    {
                        if( parameter.hasAnnotation( Required.class ) )
                        {
                            throw new CommandDefinitionException( "boolean option '" + longOptionName + "' was defined as required (boolean options cannot be required)", getName() );
                        }
                    }
                    else
                    {
                        opt.withRequiredArgument();
                    }
                }
                else
                {
                    Arguments argsAnn = parameter.getAnnotation( Arguments.class );
                    if( argsAnn != null )
                    {
                        if( !parameter.getType().isAssignableFrom( String[].class ) )
                        {
                            throw new CommandDefinitionException( "@Arguments parameters must be of type String[]", getName() );
                        }

                        Desc descAnn = parameter.getAnnotation( Desc.class );
                        if( descAnn != null )
                        {
                            optionsBuilder.withExtraArguments( descAnn.value() );
                        }
                    }
                }
            }
        }

        @Override
        public int execute( @Nonnull final Console console, @Nonnull final Options options ) throws Exception
        {
            Map<String, Object> resolveContext = new HashMap<>();
            resolveContext.put( "console", console );
            resolveContext.put( "options", options );
            Object result = invoker.resolve( resolveContext ).invoke();
            if( result instanceof Number )
            {
                return ( ( Number ) result ).intValue();
            }
            else
            {
                return 0;
            }
        }

        @Nullable
        @Override
        public Object resolve( @Nonnull MethodParameter parameter, @Nonnull Map<String, Object> resolveContext )
        {
            Options options = ( Options ) resolveContext.get( "options" );
            Console console = ( Console ) resolveContext.get( "console" );

            TypeToken<?> type = parameter.getType();
            if( type.isAssignableFrom( Console.class ) )
            {
                return console;
            }
            else if( type.isAssignableFrom( Options.class ) )
            {
                return options;
            }

            Arguments argumentsAnn = parameter.getAnnotation( Arguments.class );
            if( argumentsAnn != null )
            {
                List<String> extraArguments = options.getExtraArguments();
                return extraArguments.toArray( new String[ extraArguments.size() ] );
            }

            Option ann = parameter.getAnnotation( Option.class );
            if( ann == null )
            {
                return SKIP;
            }

            if( boolean.class.equals( type.getRawType() ) || Boolean.class.equals( type.getRawType() ) )
            {
                return options.has( getLongOptionNameForParameter( parameter ) );
            }
            else
            {
                return options.get( getLongOptionNameForParameter( parameter ), type );
            }
        }
    }
}
