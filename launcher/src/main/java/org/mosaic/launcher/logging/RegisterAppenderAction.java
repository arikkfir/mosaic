package org.mosaic.launcher.logging;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import org.xml.sax.Attributes;

/**
 * @author arik
 */
public class RegisterAppenderAction extends Action
{
    private final AppenderRegistry appenderRegistry;

    public RegisterAppenderAction( AppenderRegistry appenderRegistry )
    {
        this.appenderRegistry = appenderRegistry;
    }

    @Override
    public void begin( InterpretationContext ic, String name, Attributes attributes ) throws ActionException
    {
        // no-op
    }

    @Override
    public void end( InterpretationContext ic, String name ) throws ActionException
    {
        Object appenderObject = ic.peekObject();
        if( appenderObject instanceof Appender )
        {
            Appender<?> appender = ( Appender<?> ) appenderObject;
            appenderRegistry.addAppender( appender );
        }
        else
        {
            addError( "Could not register object '" + appenderObject + "' in appender registry - not an appender" );
        }
    }
}
