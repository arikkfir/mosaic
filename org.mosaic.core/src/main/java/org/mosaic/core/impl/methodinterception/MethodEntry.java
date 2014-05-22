package org.mosaic.core.impl.methodinterception;

import java.lang.reflect.Method;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.base.ToStringHelper;

/**
 * @author arik
 */
public class MethodEntry
{
    @Nonnull
    private static Class<?>[] getType( @Nonnull ClassLoader classLoader,
                                       @Nonnull String desc,
                                       int descLen,
                                       int start,
                                       int num )
    {
        Class<?> clazz;
        if( start >= descLen )
        {
            return new Class<?>[ num ];
        }

        char c = desc.charAt( start );
        switch( c )
        {
            case 'Z':
                clazz = Boolean.TYPE;
                break;
            case 'C':
                clazz = Character.TYPE;
                break;
            case 'B':
                clazz = Byte.TYPE;
                break;
            case 'S':
                clazz = Short.TYPE;
                break;
            case 'I':
                clazz = Integer.TYPE;
                break;
            case 'J':
                clazz = Long.TYPE;
                break;
            case 'F':
                clazz = Float.TYPE;
                break;
            case 'D':
                clazz = Double.TYPE;
                break;
            case 'V':
                clazz = Void.TYPE;
                break;
            case 'L':
            case '[':
                return getClassType( classLoader, desc, descLen, start, num );
            default:
                return new Class<?>[ num ];
        }

        Class<?>[] result = getType( classLoader, desc, descLen, start + 1, num + 1 );
        result[ num ] = clazz;
        return result;
    }

    @Nonnull
    private static Class<?>[] getClassType( @Nonnull ClassLoader classLoader,
                                            @Nonnull String desc,
                                            int descLen,
                                            int start,
                                            int num )
    {
        int end = start;
        while( desc.charAt( end ) == '[' )
        {
            ++end;
        }

        if( desc.charAt( end ) == 'L' )
        {
            end = desc.indexOf( ';', end );
            if( end < 0 )
            {
                throw new IndexOutOfBoundsException( "bad descriptor" );
            }
        }

        String cname;
        if( desc.charAt( start ) == 'L' )
        {
            cname = desc.substring( start + 1, end );
        }
        else
        {
            cname = desc.substring( start, end + 1 );
        }

        Class<?>[] result = getType( classLoader, desc, descLen, end + 1, num + 1 );
        try
        {
            result[ num ] = classLoader.loadClass( cname.replace( '/', '.' ) );
        }
        catch( ClassNotFoundException e )
        {
            // "new RuntimeException(e)" is not available in JDK 1.3.
            throw new RuntimeException( e.getMessage() );
        }

        return result;
    }

    private final long id;

    private final long moduleId;

    private final long moduleRevisionId;

    @Nonnull
    private final Method method;

    public MethodEntry( long id,
                        long moduleId,
                        long moduleRevisionId,
                        @Nonnull Class<?> clazz,
                        @Nonnull String methodName,
                        @Nonnull String parametersDescriptor )
    {
        this.id = id;
        this.moduleId = moduleId;
        this.moduleRevisionId = moduleRevisionId;

        Class<?>[] argumentTypes = getType( clazz.getClassLoader(), parametersDescriptor, parametersDescriptor.length(), 1, 0 );
        try
        {
            this.method = clazz.getDeclaredMethod( methodName, argumentTypes );
        }
        catch( Throwable e )
        {
            throw new IllegalStateException( "Could not discover method '" + clazz.getName() + "." + methodName + "': " + e.getMessage(), e );
        }
    }

    public long getId()
    {
        return this.id;
    }

    public long getModuleId()
    {
        return this.moduleId;
    }

    public long getModuleRevisionId()
    {
        return this.moduleRevisionId;
    }

    @Nonnull
    public Method getMethod()
    {
        return this.method;
    }

    @SuppressWarnings( "RedundantIfStatement" )
    @Override
    public boolean equals( Object o )
    {
        if( this == o )
        {
            return true;
        }
        if( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        MethodEntry that = ( MethodEntry ) o;

        if( id != that.id )
        {
            return false;
        }
        if( moduleId != that.moduleId )
        {
            return false;
        }
        if( moduleRevisionId != that.moduleRevisionId )
        {
            return false;
        }
        if( !method.equals( that.method ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = ( int ) ( id ^ ( id >>> 32 ) );
        result = 31 * result + ( int ) ( moduleId ^ ( moduleId >>> 32 ) );
        result = 31 * result + ( int ) ( moduleRevisionId ^ ( moduleRevisionId >>> 32 ) );
        result = 31 * result + method.hashCode();
        return result;
    }

    @Nonnull
    @Override
    public String toString()
    {
        return ToStringHelper.create( this )
                             .add( "id", this.id )
                             .add( "module", this.moduleId + "." + this.moduleRevisionId )
                             .add( "method", this.method.toGenericString() )
                             .toString();
    }
}
