package org.mosaic.util.collection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
public abstract class Maps
{
    public static Map<String, String> mapFrom( Properties properties )
    {
        Map<String, String> map = new HashMap<>( properties.size() );
        for( String propertyName : properties.stringPropertyNames() )
        {
            map.put( propertyName, properties.getProperty( propertyName ) );
        }
        return map;
    }

    public static Map<String, List<String>> listMapFromArrayMap( Map<String, String[]> source )
    {
        Map<String, List<String>> result = new HashMap<>();
        for( Map.Entry<String, String[]> entry : source.entrySet() )
        {
            result.put( entry.getKey(), asList( entry.getValue() ) );
        }
        return result;
    }

}
