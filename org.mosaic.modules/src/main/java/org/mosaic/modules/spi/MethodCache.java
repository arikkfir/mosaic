package org.mosaic.modules.spi;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.osgi.BundleUtils;
import org.osgi.framework.*;

/**
 * @author arik
 */
public final class MethodCache
{
    @Nonnull
    private static final MethodCache instance = new MethodCache();

    @Nonnull
    public static MethodCache getInstance()
    {
        return MethodCache.instance;
    }

    @Nonnull
    private final Map<Long, MethodEntry> knownMethods = new ConcurrentHashMap<>();

    private MethodCache()
    {
        Bundle bundle = FrameworkUtil.getBundle( getClass() );
        if( bundle == null )
        {
            throw new IllegalStateException( "cannot use MethodCache outside OSGi context" );
        }

        BundleContext bundleContext = bundle.getBundleContext();
        if( bundleContext != null )
        {
            bundleContext.addBundleListener( new BundleListener()
            {
                @Override
                public void bundleChanged( @Nonnull BundleEvent event )
                {
                    if( event.getType() == BundleEvent.UNRESOLVED )
                    {
                        long bundleId = event.getBundle().getBundleId();
                        for( Iterator<MethodEntry> iterator = knownMethods.values().iterator(); iterator.hasNext(); )
                        {
                            MethodEntry entry = iterator.next();
                            if( entry.moduleId == bundleId )
                            {
                                iterator.remove();
                            }
                        }
                    }
                }
            } );
        }
    }

    public void registerMethod( long id,
                                long moduleId,
                                @Nonnull String className,
                                @Nonnull String methodName,
                                @Nonnull String[] argumentTypeNames ) throws MethodAlreadyRegisteredException
    {
        MethodEntry newEntry = new MethodEntry( id, moduleId, className, methodName, argumentTypeNames );
        MethodEntry existingEntry = this.knownMethods.get( id );
        if( existingEntry != null )
        {
            if( !newEntry.equals( existingEntry ) )
            {
                throw new MethodAlreadyRegisteredException( id, existingEntry, newEntry );
            }
            else
            {
                existingEntry.method = null;
            }
        }
        else
        {
            this.knownMethods.put( id, newEntry );
        }
    }

    @Nonnull
    public Method getMethod( long id ) throws ClassNotFoundException
    {
        MethodEntry entry = this.knownMethods.get( id );
        if( entry == null )
        {
            throw new IllegalArgumentException( "Unknown method ID: " + id );
        }
        else
        {
            return entry.getMethod();
        }
    }

    class MethodEntry
    {
        private final long id;

        private final long moduleId;

        @Nonnull
        private final String className;

        @Nonnull
        private final String methodName;

        @Nonnull
        private final String[] argumentTypeNames;

        @Nullable
        private Method method;

        private MethodEntry( long id,
                             long moduleId,
                             @Nonnull String className,
                             @Nonnull String methodName,
                             @Nonnull String[] argumentTypeNames )
        {
            this.id = id;
            this.moduleId = moduleId;
            this.className = className;
            this.methodName = methodName;
            this.argumentTypeNames = argumentTypeNames;
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
            if( this.id != that.id )
            {
                return false;
            }
            else if( this.moduleId != that.moduleId )
            {
                return false;
            }
            else if( !Arrays.equals( this.argumentTypeNames, that.argumentTypeNames ) )
            {
                return false;
            }
            else if( !this.className.equals( that.className ) )
            {
                return false;
            }
            else if( !this.methodName.equals( that.methodName ) )
            {
                return false;
            }
            else
            {
                return true;
            }
        }

        @Override
        public int hashCode()
        {
            int result = ( int ) ( this.id ^ ( this.id >>> 32 ) );
            result = 31 * result + ( int ) ( this.moduleId ^ ( this.moduleId >>> 32 ) );
            result = 31 * result + this.className.hashCode();
            result = 31 * result + this.methodName.hashCode();
            result = 31 * result + Arrays.hashCode( this.argumentTypeNames );
            return result;
        }

        @Override
        public String toString()
        {
            return "MethodEntry[id=" + this.id + ",moduleId=" + moduleId + ",class=" + this.className + ",methodName=" + this.methodName + "]";
        }

        long getModuleId()
        {
            return moduleId;
        }

        Method getMethod() throws ClassNotFoundException
        {
            if( this.method == null )
            {
                // resolve declaring class
                Class<?> declaringClass;
                ClassLoader classLoader;
                try
                {
                    Bundle entryBundle = BundleUtils.bundleContext( getClass() ).get().getBundle( this.moduleId );
                    declaringClass = entryBundle.loadClass( this.className );
                    classLoader = declaringClass.getClassLoader();
                }
                catch( Throwable e )
                {
                    throw new IllegalStateException( "Could not load class '" + this.className + "' declaring method '" + this.className + "." + this.methodName + "': " + e.getMessage(), e );
                }

                // resolve argument types
                Class<?>[] argumentTypes = new Class<?>[ this.argumentTypeNames.length ];
                for( int i = 0; i < this.argumentTypeNames.length; i++ )
                {
                    try
                    {
                        argumentTypes[ i ] = resolveType( classLoader, this.argumentTypeNames[ i ] );
                    }
                    catch( ClassNotFoundException e )
                    {
                        throw new IllegalStateException( "Could not find argument type '" + this.className + "' for declaring method '" + this.className + "." + this.methodName + "': " + e.getMessage(), e );
                    }
                }

                // resolve method and cache it
                try
                {
                    this.method = declaringClass.getDeclaredMethod( this.methodName, argumentTypes );
                }
                catch( Throwable e )
                {
                    throw new IllegalStateException( "Could not find method '" + this.className + "' declaring method '" + this.className + "." + this.methodName + "': " + e.getMessage(), e );
                }
            }
            return this.method;
        }

        @Nonnull
        private Class<?> resolveType( @Nonnull ClassLoader classLoader, @Nonnull String parameterTypeName )
                throws ClassNotFoundException
        {
            StringBuilder arrBuf = new StringBuilder( 20 );
            while( parameterTypeName.endsWith( "[]" ) )
            {
                arrBuf.append( "[" );
                parameterTypeName = parameterTypeName.substring( 0, parameterTypeName.length() - 2 );
            }

            boolean isArray = arrBuf.length() > 0;
            switch( parameterTypeName )
            {
                case "boolean":
                    return isArray ? Class.forName( arrBuf + "Z" ) : boolean.class;
                case "byte":
                    return isArray ? Class.forName( arrBuf + "B" ) : byte.class;
                case "char":
                    return isArray ? Class.forName( arrBuf + "C" ) : char.class;
                case "double":
                    return isArray ? Class.forName( arrBuf + "D" ) : double.class;
                case "float":
                    return isArray ? Class.forName( arrBuf + "F" ) : float.class;
                case "int":
                    return isArray ? Class.forName( arrBuf + "I" ) : int.class;
                case "long":
                    return isArray ? Class.forName( arrBuf + "J" ) : long.class;
                case "short":
                    return isArray ? Class.forName( arrBuf + "S" ) : short.class;
                default:
                    return isArray ? Class.forName( arrBuf + "L" + parameterTypeName + ";", true, classLoader )
                                   : classLoader.loadClass( parameterTypeName );
            }
        }
    }
}
