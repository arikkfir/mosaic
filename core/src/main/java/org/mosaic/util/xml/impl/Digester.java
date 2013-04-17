package org.mosaic.util.xml.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.xml.validation.Schema;
import org.apache.commons.digester3.substitution.MultiVariableExpander;
import org.apache.commons.digester3.substitution.VariableSubstitutor;
import org.apache.commons.logging.LogFactory;

/**
 * @author arik
 */
public class Digester extends org.apache.commons.digester3.Digester
{
    public Digester( @Nonnull Class<?> loadContext, @Nonnull Schema schema )
    {
        this( loadContext, schema, Collections.<String, Object>emptyMap() );
    }

    public Digester( @Nonnull Class<?> loadContext, @Nonnull Schema schema, @Nonnull Map<String, ?> vars )
    {
        setClassLoader( loadContext.getClassLoader() );
        setErrorHandler( StrictErrorHandler.INSTANCE );
        setLogger( LogFactory.getLog( loadContext.getName() + ".digester" ) );
        setNamespaceAware( true );
        setSAXLogger( LogFactory.getLog( loadContext.getName() + ".sax" ) );
        setUseContextClassLoader( false );
        setXMLSchema( schema );

        // set up the variables the input xml can reference
        Map<String, Object> variables = new HashMap<>();
        for( Map.Entry<Object, Object> entry : System.getProperties().entrySet() )
        {
            variables.put( entry.getKey().toString(), entry.getValue() );
        }
        variables.putAll( vars );

        // map ${...} to the entries in the vars map
        MultiVariableExpander expander = new MultiVariableExpander();
        expander.addSource( "$", variables );
        setSubstitutor( new VariableSubstitutor( expander ) );
    }
}
