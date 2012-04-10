package org.mosaic.server.shell.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSpecBuilder;
import org.mosaic.describe.Description;
import org.mosaic.describe.RequiredArg;
import org.mosaic.lifecycle.MethodEndpointInfo;
import org.mosaic.lifecycle.ServiceBind;
import org.mosaic.lifecycle.ServiceUnbind;
import org.mosaic.server.shell.Option;
import org.mosaic.server.shell.ShellCommand;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
@Component
public class ShellCommandsManager {

    private final Map<MethodEndpointInfo, OptionParser> commands = new ConcurrentHashMap<>();

    @ServiceBind
    public synchronized void addListener( MethodEndpointInfo methodEndpointInfo ) {
        if( methodEndpointInfo.isOfType( ShellCommand.class ) ) {
            this.commands.put( methodEndpointInfo, parseCommand( methodEndpointInfo ) );
        }
    }

    @ServiceUnbind
    public synchronized void removeListener( MethodEndpointInfo methodEndpointInfo ) {
        if( methodEndpointInfo.isOfType( ShellCommand.class ) ) {
            this.commands.remove( methodEndpointInfo );
        }
    }

    public OptionParser getParser( String command ) {
        for( MethodEndpointInfo endpoint : this.commands.keySet() ) {
            if( endpoint.getMethod().getName().equals( command ) ) {
                return this.commands.get( endpoint );
            }
        }
        return null;
    }

    private OptionParser parseCommand( MethodEndpointInfo endpoint ) {
        Method method = endpoint.getMethod();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if( parameterTypes.length == 1 && parameterTypes[ 0 ].isInterface() ) {
            return parseOptionsBean( parameterTypes[ 0 ] );
        } else {
            return parseMethodOptions( endpoint );
        }
    }

    private OptionParser parseOptionsBean( @SuppressWarnings( "UnusedParameters" ) Class<?> type ) {
        throw new UnsupportedOperationException();
        //TODO 4/10/12: parse options bean
    }

    private OptionParser parseMethodOptions( MethodEndpointInfo endpoint ) {
        Method method = endpoint.getMethod();

        OptionParser parser = new OptionParser();

        ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames( method );
        Class<?>[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        for( int i = 0, parameterTypesLength = parameterTypes.length; i < parameterTypesLength; i++ ) {
            addOption( endpoint, parser, parameterNames[ i ], parameterTypes[ i ], parameterAnnotations[ i ] );
        }

        return parser;
    }

    private void addOption( MethodEndpointInfo endpoint,
                            OptionParser parser,
                            String parameterName,
                            Class<?> parameterType, Annotation[] parameterAnnotation ) {
        Option optionAnn = findAnnotation( parameterAnnotation, Option.class );

        Description descAnn = findAnnotation( parameterAnnotation, Description.class );
        String description = descAnn == null ? "" : descAnn.value();

        RequiredArg requiredArgAnn = findAnnotation( parameterAnnotation, RequiredArg.class );
        boolean argRequired = requiredArgAnn != null && requiredArgAnn.value();

        if( parameterType.isPrimitive() ) {

            throw new IllegalArgumentException( "@ShellCommand must not have primitive parameters (" + endpoint + ")" );

        } else if( Boolean.class.equals( parameterType ) ) {

            @SuppressWarnings( { "unchecked", "UnusedDeclaration" } )
            ArgumentAcceptingOptionSpec<Boolean> spec =
                    addOption( parser, parameterName, optionAnn, description )
                            .withOptionalArg()
                            .ofType( Boolean.class )
                            .defaultsTo( true );

        } else {

            OptionSpecBuilder builder = addOption( parser, parameterName, optionAnn, description );

            ArgumentAcceptingOptionSpec<String> argSpec;
            if( argRequired ) {
                argSpec = builder.withRequiredArg();
            } else {
                argSpec = builder.withOptionalArg();
            }
            argSpec.ofType( parameterType );

        }
    }

    private OptionSpecBuilder addOption( OptionParser parser, String name, Option optionAnn, String description ) {
        OptionSpecBuilder specBuilder;
        if( optionAnn != null && optionAnn.alias().trim().length() > 0 ) {
            specBuilder = parser.acceptsAll( asList( name, optionAnn.alias() ), description );
        } else {
            specBuilder = parser.accepts( name, description );
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
}
