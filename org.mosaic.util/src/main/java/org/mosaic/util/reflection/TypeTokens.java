package org.mosaic.util.reflection;

import com.google.common.reflect.TypeToken;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

/**
 * @author arik
 */
@SuppressWarnings( "WeakerAccess" )
public class TypeTokens
{
    public static final TypeToken<String> STRING = TypeToken.of( String.class );

    public static final TypeToken<Boolean> BOOLEAN = TypeToken.of( Boolean.class );

    public static final TypeToken<Date> DATE = TypeToken.of( Date.class );

    public static final TypeToken<Writer> WRITER = TypeToken.of( Writer.class );

    public static final TypeToken<OutputStream> OUTPUT_STREAM = TypeToken.of( OutputStream.class );

    public static final Type LIST_GET_GENERIC_TYPE;

    static
    {
        try
        {
            LIST_GET_GENERIC_TYPE = List.class.getMethod( "get", int.class ).getGenericReturnType();
        }
        catch( NoSuchMethodException e )
        {
            throw new IllegalStateException( "Could not obtain List.get(int) method via reflection: " + e.getMessage(), e );
        }
    }
}
