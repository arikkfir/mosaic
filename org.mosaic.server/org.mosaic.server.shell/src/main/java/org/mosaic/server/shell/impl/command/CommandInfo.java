package org.mosaic.server.shell.impl.command;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpecBuilder;
import org.mosaic.describe.Description;
import org.mosaic.describe.RequiredArg;
import org.mosaic.lifecycle.MethodEndpointInfo;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.server.shell.Args;
import org.mosaic.server.shell.Console;
import org.mosaic.server.shell.Option;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
public class CommandInfo {

    private static final Logger LOG = LoggerFactory.getLogger( CommandInfo.class );

    private final String name;

    private final MethodEndpointInfo endpoint;

    private final OptionParser parser;

    private final List<CommandOption> options = new LinkedList<>();

    CommandInfo( MethodEndpointInfo endpoint ) {
        this.endpoint = endpoint;
        this.parser = new OptionParser();

        Method method = endpoint.getMethod();

        ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames( method );
        Class<?>[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        for( int i = 0, parameterTypesLength = parameterTypes.length; i < parameterTypesLength; i++ ) {
            this.options.add( createOption( parameterNames[ i ], parameterTypes[ i ], parameterAnnotations[ i ] ) );
        }

        String commandName = AnnotationUtils.getValue( endpoint.getType(), "value" ).toString();
        if( commandName.trim().length() == 0 ) {
            commandName = method.getName();
        }
        this.name = commandName;
    }

    public String getName() {
        return name;
    }

    public void execute( Console console, String[] args ) throws Exception {
        OptionSet optionSet = this.parser.parse( args );
        List<Object> argValues = new LinkedList<>();
        for( CommandOption option : this.options ) {
            argValues.add( option.getValue( console, optionSet ) );
        }

        try {
            this.endpoint.invoke( argValues.toArray() );

        } catch( InvocationTargetException e ) {
            Throwable cause = e.getCause();
            Exception ex = e;
            if( cause instanceof Exception ) {
                ex = ( Exception ) cause;
            }
            LOG.error( "Error executing command '{}': {} ({})", this.name, e.getMessage(), this.endpoint, ex );
            throw ex;

        } catch( Exception e ) {
            LOG.error( "Error executing command '{}': {} ({})", this.name, e.getMessage(), this.endpoint, e );
            throw e;
        }
    }

    private CommandOption createOption( String parameterName,
                                        Class<?> parameterType,
                                        Annotation[] parameterAnnotation ) {

        Args argsAnn = findAnnotation( parameterAnnotation, Args.class );
        if( argsAnn != null ) {
            return new ArgumentsCommandOption();
        }

        Option optionAnn = findAnnotation( parameterAnnotation, Option.class );

        Description descAnn = findAnnotation( parameterAnnotation, Description.class );
        String description = descAnn == null ? "" : descAnn.value();

        RequiredArg requiredArgAnn = findAnnotation( parameterAnnotation, RequiredArg.class );
        boolean argRequired = requiredArgAnn != null && requiredArgAnn.value();

        if( Console.class.equals( parameterType ) ) {

            return new ConsoleCommandOption();

        } else if( Boolean.class.equals( parameterType ) || boolean.class.equals( parameterType ) ) {

            return new BooleanCommandOption( addOption( parameterName, optionAnn, description )
                                                     .options()
                                                     .iterator()
                                                     .next() );

        } else {

            OptionSpecBuilder builder = addOption( parameterName, optionAnn, description );

            ArgumentAcceptingOptionSpec<String> spec;
            if( argRequired ) {
                spec = builder.withRequiredArg();
            } else {
                spec = builder.withOptionalArg();
            }
            spec.ofType( parameterType );
            return new SimpleCommandOption( spec.options().iterator().next() );

        }
    }

    private OptionSpecBuilder addOption( String name, Option optionAnn, String description ) {
        OptionSpecBuilder specBuilder;
        if( optionAnn != null && optionAnn.alias().trim().length() > 0 ) {
            specBuilder = this.parser.acceptsAll( asList( name, optionAnn.alias() ), description );
        } else {
            specBuilder = this.parser.accepts( name, description );
        }
        return specBuilder;
    }

    private static <T> T findAnnotation( Annotation[] annotations, Class<T> type ) {
        for( Annotation annotation : annotations ) {
            if( annotation.annotationType().equals( type ) ) {
                return type.cast( annotation );
            }
        }
        return null;
    }

    private interface CommandOption {

        Object getValue( Console console, OptionSet options );

    }

    private class SimpleCommandOption implements CommandOption {

        private final String alias;

        private SimpleCommandOption( String alias ) {
            this.alias = alias;
        }

        @Override
        public Object getValue( Console console, OptionSet options ) {
            return options.valueOf( alias );
        }
    }

    private class BooleanCommandOption implements CommandOption {

        private final String alias;

        private BooleanCommandOption( String alias ) {
            this.alias = alias;
        }

        @Override
        public Object getValue( Console console, OptionSet options ) {
            return options.has( this.alias );
        }
    }

    private class ConsoleCommandOption implements CommandOption {

        @Override
        public Object getValue( Console console, OptionSet options ) {
            return console;
        }
    }

    private class ArgumentsCommandOption implements CommandOption {

        @Override
        public Object getValue( Console console, OptionSet options ) {
            List<String> args = new LinkedList<>();
            ListIterator<String> i = options.nonOptionArguments().listIterator();
            while( i.hasNext() ) {
                String arg = i.next();
                if( arg.startsWith( "\"" ) ) {
                    // loop while the starting quote is the *only* quote, and while we have more tokens to swallow
                    while( arg.indexOf( '"', 1 ) < 0 && i.hasNext() ) {
                        arg += ' ';
                        arg += i.next();
                    }

                    int closingQuote = arg.lastIndexOf( '"' );
                    if( closingQuote == 0 ) {
                        // could not find closing quote - close it ourselves
                        arg += '"';
                    }
                }

                if( arg.startsWith( "\"" ) && arg.endsWith( "\"" ) ) {
                    arg = arg.substring( 1, arg.lastIndexOf( '"' ) );
                }
                args.add( arg );
            }
            return args;
        }
    }
}
