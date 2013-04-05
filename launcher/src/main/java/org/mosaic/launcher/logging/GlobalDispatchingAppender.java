package org.mosaic.launcher.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Marker;

import static org.apache.commons.lang3.text.StrMatcher.*;
import static org.mosaic.launcher.logging.AppenderRegistry.*;

/**
 * Global dispatcher is a necessary beast. In theory, we can simply use logback.xml file to configure which logger uses
 * which appender. BUT what we want is dynamicity, i.e. different configurations under different circumstances, therefor
 * the static logback XML file is no longer sufficient.
 *
 * @author arik
 */
public class GlobalDispatchingAppender<E extends ILoggingEvent> extends AppenderBase<E>
{
    private final ThreadLocal<Set<Appender<E>>> appenders = new ThreadLocal<Set<Appender<E>>>()
    {
        @Override
        protected Set<Appender<E>> initialValue()
        {
            return new LinkedHashSet<>( 5 );
        }
    };

    private final ThreadLocal<StringBuilder> buffers = new ThreadLocal<StringBuilder>()
    {
        @Override
        protected StringBuilder initialValue()
        {
            return new StringBuilder( 100 );
        }
    };

    private final ThreadLocal<StrTokenizer> tokenizers = new ThreadLocal<StrTokenizer>()
    {
        @Override
        protected StrTokenizer initialValue()
        {
            return new StrTokenizer().setDelimiterMatcher( commaMatcher() )
                                     .setIgnoreEmptyTokens( true )
                                     .setQuoteMatcher( doubleQuoteMatcher() )
                                     .setTrimmerMatcher( trimMatcher() );
        }
    };

    @Override
    public void start()
    {
        if( findAppender( CONSOLE_APPENDER ) == null )
        {
            addError( "Could not start global appender - could not find '" + CONSOLE_APPENDER + "' appender" );
        }
        else if( findAppender( FILE_APPENDER ) == null )
        {
            addError( "Could not start global appender - could not find '" + FILE_APPENDER + "' appender" );
        }
        else
        {
            super.start();
        }
    }

    @Override
    protected void append( @Nonnull E event )
    {
        // get local buffer for this thread
        StringBuilder buffer = this.buffers.get();
        buffer.setLength( 0 );

        // allow specifying appenders using the 'appender' MDC key (value is "X,Y,Z" where X, Y and Z are all appender names)
        addAppendersFromMdc( buffer, event );

        // allow specific log calls to specify their own appenders by using a custom "appender:X,Y,Z" marker (X, Y and Z are each just appender names)
        addAppendersFromMarker( buffer, event );

        // tokenize appender names
        StrTokenizer tokenizer = this.tokenizers.get();
        tokenizer.reset( buffer.toString() );

        // if no tokens available, then no specific appender has been requested; send to default appender
        if( !tokenizer.hasNext() )
        {
            AppenderRegistry.getDefaultAppender().doAppend( event );
        }
        else
        {
            // keep track of used appenders for the event - to prevent sending the same event twice
            Set<Appender<E>> appenders = this.appenders.get();
            appenders.clear();

            // start iterating the appender names, find the appender for each and send the event to it if it wasn't used yet for this specific event
            while( tokenizer.hasNext() )
            {
                String token = tokenizer.next();

                // if the "dev" system property was given, then the default appender is "CONSOLE".
                // BUT do not do this if we're in a job - to avoid polluting the console...
                if( Boolean.getBoolean( "dev" ) && !"jobs".equalsIgnoreCase( token ) )
                {
                    token = AppenderRegistry.CONSOLE_APPENDER;
                }

                // add to list of appenders
                Appender<E> appender = AppenderRegistry.findAppender( token );
                if( appender != null && !appenders.contains( appender ) )
                {
                    appenders.add( appender );
                    appender.doAppend( event );
                }
            }
        }
    }

    private void addAppendersFromMarker( @Nonnull StringBuilder buffer, @Nonnull E event )
    {
        Marker marker = event.getMarker();
        if( marker != null )
        {
            String markerName = marker.getName();
            if( markerName.startsWith( "appender:" ) )
            {
                String markerAppendersNames = markerName.substring( "appender:".length() ).trim();
                if( !markerAppendersNames.isEmpty() )
                {
                    if( buffer.length() > 0 )
                    {
                        buffer.append( ',' );
                    }
                    buffer.append( markerAppendersNames );
                }
            }
        }
    }

    private void addAppendersFromMdc( @Nonnull StringBuilder buffer, @Nonnull E event )
    {
        String mdcAppenders = event.getMDCPropertyMap().get( "appender" );
        if( mdcAppenders != null )
        {
            if( buffer.length() > 0 )
            {
                buffer.append( ',' );
            }
            buffer.append( mdcAppenders );
        }
    }
}
