package org.mosaic.console.impl;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.console.Command;
import org.mosaic.console.Console;
import org.mosaic.console.spi.*;
import org.mosaic.console.util.table.SimpleColumn;
import org.mosaic.console.util.table.TablePrinter;
import org.mosaic.modules.*;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.conversion.ConversionService;
import org.mosaic.util.method.MethodParameter;

import static com.google.common.base.Strings.repeat;
import static com.google.common.collect.Collections2.filter;
import static java.util.Arrays.asList;

/**
 * @author arik
 */
@Service
public class CommandManagerImpl implements CommandManager
{
    private static final TypeToken<MapEx<String, List<String>>> OPTION_VALUES_TYPE_TOKEN = new TypeToken<MapEx<String, List<String>>>()
    {
    };

    private static class MethodParameterAnnotationPredicate implements Predicate<MethodParameter>
    {
        @Nonnull
        private final Class<? extends Annotation> annotationType;

        private MethodParameterAnnotationPredicate( @Nonnull Class<? extends Annotation> annotationType )
        {
            this.annotationType = annotationType;
        }

        @Override
        public boolean apply( @Nullable MethodParameter input )
        {
            return input != null && input.getAnnotation( this.annotationType ) != null;
        }
    }

    private static class ValuedParameterAnnotationPredicate implements Predicate<ValuedParameter>
    {
        @Nonnull
        private final Class<?> type;

        private ValuedParameterAnnotationPredicate( @Nonnull Class<?> type )
        {
            this.type = type;
        }

        @Override
        public boolean apply( @Nullable ValuedParameter input )
        {
            return this.type.isInstance( input );
        }
    }

    @Nonnull
    private final Map<Long, CommandAdapter> commandAdapters = new ConcurrentHashMap<>();

    @Nonnull
    @Service
    private ConversionService conversionService;

    @OnServiceAdded
    void addCommand( @Nonnull ServiceReference<MethodEndpoint<Command>> reference )
    {
        Optional<MethodEndpoint<Command>> endpoint = reference.service();
        if( endpoint.isPresent() )
        {
            this.commandAdapters.put( reference.getId(), new CommandAdapter( endpoint.get() ) );
        }
    }

    @OnServiceRemoved
    void removeCommand( @Nonnull ServiceReference<MethodEndpoint<Command>> reference )
    {
        this.commandAdapters.remove( reference.getId() );
    }

    @Nonnull
    @Override
    public Collection<CommandDescriptor> getCommands()
    {
        List<CommandDescriptor> commandDescriptors = new LinkedList<>();
        for( CommandAdapter commandAdapter : this.commandAdapters.values() )
        {
            commandDescriptors.add( commandAdapter );
        }
        return commandDescriptors;
    }

    @Override
    public void execute( @Nonnull Console console, @Nonnull String commandLine ) throws IOException
    {
        String commandName;
        String arguments;

        int indexOfSpace = commandLine.indexOf( ' ' );
        if( indexOfSpace < 0 )
        {
            commandName = commandLine;
            arguments = "";
        }
        else
        {
            commandName = commandLine.substring( 0, indexOfSpace );
            arguments = commandLine.substring( indexOfSpace + 1 );
        }

        for( CommandAdapter adapter : this.commandAdapters.values() )
        {
            if( adapter.names.contains( commandName ) )
            {
                try
                {
                    adapter.invoke( console, arguments );
                    return;
                }
                catch( CommandExecutionException e )
                {
                    Throwable throwable = e;
                    while( throwable != null )
                    {
                        console.println( throwable.getMessage() );
                        throwable = throwable.getCause();
                    }
                    return;
                }
                catch( CommandCanceledException ignore )
                {
                    return;
                }
                catch( QuitSessionException e )
                {
                    throw e;
                }
                catch( Throwable e )
                {
                    console.printStackTrace( e );
                    return;
                }
            }
        }
        throw new CommandNotFoundException( "could not find command '" + commandName + "'" );
    }

    @Override
    public void showHelp( @Nonnull Console console, @Nonnull String commandName ) throws IOException
    {
        for( CommandAdapter commandAdapter : this.commandAdapters.values() )
        {
            if( commandAdapter.names.contains( commandName ) )
            {
                commandAdapter.showHelp( console );
                return;
            }
        }
        console.println( "unknown command: {}", commandName );
    }

    abstract class ValuedParameter
    {
        abstract Object getValue( @Nonnull Console console, @Nonnull ParsedCommandLine commandLine );
    }

    final class ConsoleParameter extends ValuedParameter
    {
        @Override
        Object getValue( @Nonnull Console console, @Nonnull ParsedCommandLine commandLine )
        {
            return console;
        }
    }

    final class CommandArgument extends ValuedParameter
    {
        @Nonnull
        private final String name;

        @Nonnull
        private final String synopsis;

        @Nonnull
        private final String description;

        private final boolean required;

        @Nonnull
        private final TypeToken<?> type;

        private final int index;

        private CommandArgument( @Nonnull MethodParameter methodParameter )
        {
            Command.Arg ann = methodParameter.getAnnotation( Command.Arg.class );
            //noinspection ConstantConditions
            this.name = ann.name().isEmpty() ? methodParameter.getName() : ann.name();
            this.synopsis = ann.synopsis();
            this.description = ann.description();
            this.type = methodParameter.getType();
            this.required = methodParameter.getAnnotation( Nonnull.class ) != null;

            List<MethodParameter> methodParameters = methodParameter.getMethod().getParameters();
            List<MethodParameter> argParams = new ArrayList<>( filter( methodParameters, new MethodParameterAnnotationPredicate( Command.Arg.class ) ) );
            this.index = argParams.indexOf( methodParameter );
        }

        @Nullable
        Object getValue( @Nonnull Console console, @Nonnull ParsedCommandLine commandLine )
        {
            String value = commandLine.getArgumentValue( this.index );
            if( value == null && this.required )
            {
                throw new BadUsageException( "missing '" + this.name + "' argument" );
            }
            else
            {
                return CommandManagerImpl.this.conversionService.convert( value, this.type );
            }
        }
    }

    final class CommandOption extends ValuedParameter
    {
        @Nonnull
        private final MethodParameter methodParameter;

        @Nonnull
        private final String[] names;

        @Nonnull
        private final String synopsis;

        @Nonnull
        private final String description;

        @Nullable
        private final String defaultValue;

        private CommandOption( @Nonnull MethodParameter methodParameter )
        {
            this.methodParameter = methodParameter;

            Command.Option ann = methodParameter.getAnnotation( Command.Option.class );
            //noinspection ConstantConditions
            String[] names = ann.names();
            if( names.length == 0 )
            {
                names = new String[] { methodParameter.getName() };
            }
            this.names = names;
            this.synopsis = ann.synopsis();
            this.description = ann.description();
            this.defaultValue = ann.defaultValue().equals( "##org.mosaic.null" ) ? null : ann.defaultValue();
        }

        @Nullable
        Object getValue( @Nonnull Console console, @Nonnull ParsedCommandLine commandLine )
        {
            TypeToken<?> type = this.methodParameter.getType();
            if( this.methodParameter.isArray() )
            {
                TypeToken<?> itemType = this.methodParameter.getCollectionItemType();
                if( itemType == null )
                {
                    throw new BadUsageException( "could not discover array type for parameter " + this.methodParameter );
                }

                List<String> values = commandLine.getOptionValues( this.names );
                if( values == null )
                {
                    return Array.newInstance( itemType.getRawType(), 0 );
                }

                Object array = Array.newInstance( itemType.getRawType(), values.size() );
                for( int i = 0; i < values.size(); i++ )
                {
                    Array.set( array, i, CommandManagerImpl.this.conversionService.convert( values.get( i ), itemType ) );
                }
                return array;
            }
            else if( this.methodParameter.isCollection() || this.methodParameter.isList() )
            {
                final TypeToken<?> itemType = this.methodParameter.getCollectionItemType();
                if( itemType == null )
                {
                    throw new BadUsageException( "could not discover collection type for parameter " + this.methodParameter );
                }

                List<String> values = commandLine.getOptionValues( this.names );
                if( values == null )
                {
                    return Collections.emptyList();
                }

                return Lists.transform( values, new Function<String, Object>()
                {
                    @Nullable
                    @Override
                    public Object apply( @Nullable String input )
                    {
                        return CommandManagerImpl.this.conversionService.convert( input, itemType );
                    }
                } );
            }
            else if( this.methodParameter.isSet() )
            {
                final TypeToken<?> itemType = this.methodParameter.getCollectionItemType();
                if( itemType == null )
                {
                    throw new BadUsageException( "could not discover collection type for parameter " + this.methodParameter );
                }

                List<String> values = commandLine.getOptionValues( this.names );
                if( values == null )
                {
                    return Collections.emptyList();
                }

                Set<Object> set = new LinkedHashSet<>();
                for( String value : values )
                {
                    set.add( CommandManagerImpl.this.conversionService.convert( value, itemType ) );
                }
                return set;
            }
            else if( this.methodParameter.getType().isAssignableFrom( OPTION_VALUES_TYPE_TOKEN ) )
            {
                MapEx<String, List<String>> values = new HashMapEx<>();
                for( String name : this.names )
                {
                    List<String> valuesForName = commandLine.getOptionValues( name );
                    if( valuesForName != null )
                    {
                        values.put( name, valuesForName );
                    }
                }
                return values;
            }
            else if( this.methodParameter.isProperties() )
            {
                Properties values = new Properties();
                for( String name : this.names )
                {
                    List<String> valuesForName = commandLine.getOptionValues( name );
                    if( valuesForName != null && !valuesForName.isEmpty() )
                    {
                        values.put( name, valuesForName.get( 0 ) );
                    }
                }
                return values;
            }
            else if( type.isAssignableFrom( boolean.class ) )
            {
                boolean value = Boolean.valueOf( this.defaultValue );
                List<String> values = commandLine.getOptionValues( this.names );
                if( values != null )
                {
                    for( String specifiedValue : values )
                    {
                        if( specifiedValue == null )
                        {
                            value = !value;
                        }
                        else
                        {
                            value = Boolean.valueOf( specifiedValue );
                        }
                    }
                }
                return value;
            }
            else if( type.isAssignableFrom( Boolean.class ) )
            {
                Boolean value = this.defaultValue == null ? null : Boolean.valueOf( this.defaultValue );
                List<String> values = commandLine.getOptionValues( this.names );
                if( values != null )
                {
                    for( String specifiedValue : values )
                    {
                        if( specifiedValue == null )
                        {
                            value = value == null || !value;
                        }
                        else
                        {
                            value = Boolean.valueOf( specifiedValue );
                        }
                    }
                }
                return value;
            }
            else
            {
                List<String> values = commandLine.getOptionValues( this.names );
                if( values == null || values.isEmpty() )
                {
                    return null;
                }
                else if( values.size() > 1 )
                {
                    throw new BadUsageException( "too many values for option '" + asList( this.names ) + "'" );
                }
                else
                {
                    return CommandManagerImpl.this.conversionService.convert( values.get( 0 ), type );
                }
            }
        }
    }

    private class CommandAdapter implements CommandDescriptor
    {
        @Nonnull
        private final MethodEndpoint<Command> endpoint;

        @Nonnull
        private final Set<String> names;

        @Nonnull
        private final String synopsis;

        @Nonnull
        private final String description;

        @Nonnull
        private final List<ValuedParameter> valuedParameters = new LinkedList<>();

        private CommandAdapter( @Nonnull MethodEndpoint<Command> endpoint )
        {
            this.endpoint = endpoint;

            String[] names = this.endpoint.getType().names();
            if( names.length == 0 )
            {
                names = new String[] { endpoint.getMethodHandle().getName() };
            }
            this.names = new HashSet<>( asList( names ) );
            this.synopsis = this.endpoint.getType().synopsis();
            this.description = this.endpoint.getType().description();

            for( MethodParameter methodParameter : this.endpoint.getMethodHandle().getParameters() )
            {
                if( methodParameter.getAnnotation( Command.Arg.class ) != null )
                {
                    this.valuedParameters.add( new CommandArgument( methodParameter ) );
                }
                else if( methodParameter.getAnnotation( Command.Option.class ) != null )
                {
                    this.valuedParameters.add( new CommandOption( methodParameter ) );
                }
                else if( methodParameter.getType().isAssignableFrom( Console.class ) )
                {
                    this.valuedParameters.add( new ConsoleParameter() );
                }
                else
                {
                    throw new IllegalArgumentException( "unsupported type of method parameter: " + methodParameter );
                }
            }
        }

        @Nonnull
        @Override
        public Collection<String> getNames()
        {
            return this.names;
        }

        @Nullable
        @Override
        public String getSynonpsis()
        {
            return this.synopsis;
        }

        @Nonnull
        @Override
        public String getDescription()
        {
            return this.description;
        }

        private void showHelp( @Nonnull Console console ) throws IOException
        {
            String names = this.names.toString();
            console.println()
                   .println( "NAME(S)" )
                   .print( repeat( " ", 8 ) ).println( names.substring( 1, names.length() - 1 ) )
                   .println()
                   .println( "SYNOPSIS" )
                   .print( repeat( " ", 8 ) ).println( this.synopsis.isEmpty() ? "no synopsis provided" : this.synopsis )
                   .println()
                   .println( "DESCRIPTION" )
                   .print( repeat( " ", 8 ) ).println( this.description.isEmpty() ? "no description provided" : this.description )
                   .println();

            console.println( "ARGUMENTS" );
            Collection<ValuedParameter> arguments = filter( this.valuedParameters, new ValuedParameterAnnotationPredicate( CommandArgument.class ) );
            if( arguments.isEmpty() )
            {
                console.print( repeat( " ", 8 ) ).println( "this commands does not accept arguments." );
            }
            else
            {
                @SuppressWarnings("unchecked")
                TablePrinter<CommandArgument> argumentsTable =
                        new TablePrinter<>( console,
                                            8,
                                            new SimpleColumn<CommandArgument>( "Name", 15 )
                                            {
                                                @Nullable
                                                @Override
                                                public String getValue( @Nonnull CommandArgument argument )
                                                {
                                                    return argument.name;
                                                }
                                            },
                                            new SimpleColumn<CommandArgument>( "Description" )
                                            {
                                                @Nullable
                                                @Override
                                                public String getValue( @Nonnull CommandArgument argument )
                                                {
                                                    StringBuilder buf = new StringBuilder( argument.synopsis );
                                                    if( !argument.description.isEmpty() )
                                                    {
                                                        buf.append( "\n" ).append( argument.description );
                                                    }
                                                    return buf.toString();
                                                }
                                            }
                        ).noChrome();
                for( ValuedParameter parameter : arguments )
                {
                    argumentsTable.print( ( CommandArgument ) parameter );
                }
                argumentsTable.endTable();
            }
            console.println();

            console.println( "OPTIONS" );
            Collection<ValuedParameter> options = filter( this.valuedParameters, new ValuedParameterAnnotationPredicate( CommandOption.class ) );
            if( options.isEmpty() )
            {
                console.print( repeat( " ", 8 ) ).println( "this commands does not accept options." );
            }
            else
            {
                @SuppressWarnings("unchecked")
                TablePrinter<CommandOption> optionsTable =
                        new TablePrinter<>( console,
                                            8,
                                            new SimpleColumn<CommandOption>( "Name", 15 )
                                            {
                                                @Nullable
                                                @Override
                                                public String getValue( @Nonnull CommandOption option )
                                                {
                                                    StringBuilder buffer = new StringBuilder( 30 );
                                                    for( String name : option.names )
                                                    {
                                                        if( buffer.length() > 0 )
                                                        {
                                                            buffer.append( "," );
                                                        }
                                                        buffer.append( name.length() == 1 ? "-" : "--" );
                                                        buffer.append( name );
                                                    }
                                                    return buffer.toString();
                                                }
                                            },
                                            new SimpleColumn<CommandOption>( "Description" )
                                            {
                                                @Nullable
                                                @Override
                                                public String getValue( @Nonnull CommandOption option )
                                                {
                                                    StringBuilder buf = new StringBuilder( option.synopsis );
                                                    if( !option.description.isEmpty() )
                                                    {
                                                        buf.append( "\n" ).append( option.description );
                                                    }
                                                    return buf.toString();
                                                }
                                            }
                        ).noChrome();
                for( ValuedParameter parameter : options )
                {
                    optionsTable.print( ( CommandOption ) parameter );
                }
                optionsTable.endTable();
            }
            console.println();
        }

        private void invoke( @Nonnull Console console, @Nonnull String commandLine ) throws Throwable
        {
            ParsedCommandLine cmdLine = new ParsedCommandLine( commandLine );

            List<Object> parameterValues = new LinkedList<>();
            for( ValuedParameter valuedParameter : this.valuedParameters )
            {
                parameterValues.add( valuedParameter.getValue( console, cmdLine ) );
            }
            this.endpoint.invoke( parameterValues.toArray() );
        }
    }
}
