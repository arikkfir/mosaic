package org.mosaic.modules.spi;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.osgi.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mosaic.util.osgi.BundleUtils.requireBundleContext;

/**
 * @author arik
 */
public final class MethodCache
{
    private static final Logger LOG = LoggerFactory.getLogger( MethodCache.class );

    @Nonnull
    private static final MethodCache instance = new MethodCache();

    @Nonnull
    public static MethodCache getInstance()
    {
        return MethodCache.instance;
    }

    @Nonnull
    private final AtomicLong idGenerator = new AtomicLong();

    @Nonnull
    private final Map<Long, MethodEntry> knownMethods = new ConcurrentHashMap<>();

    private MethodCache()
    {
        Bundle bundle = FrameworkUtil.getBundle( getClass() );
        if( bundle == null )
        {
            LOG.warn( "MethodCache running outside OSGi context!" );
        }
        else
        {
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
    }

    public long registerMethod( long moduleId,
                                @Nonnull String className,
                                @Nonnull String methodName,
                                @Nonnull String[] argumentTypeNames )
    {
        long id = this.idGenerator.incrementAndGet();
        registerMethod( id, moduleId, className, methodName, argumentTypeNames );
        return id;
    }

    public void registerMethod( long id,
                                long moduleId,
                                @Nonnull String className,
                                @Nonnull String methodName,
                                @Nonnull String[] argumentTypeNames )
    {
        while( this.idGenerator.get() <= id )
        {
            this.idGenerator.incrementAndGet();
        }

        if( this.knownMethods.containsKey( id ) )
        {
            throw new IllegalArgumentException( "method ID '" + id + "' already used!" );
        }

        MethodEntry entry = new MethodEntry( moduleId, className, methodName, argumentTypeNames );
        this.knownMethods.put( id, entry );
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

    private class MethodEntry
    {
        private final long moduleId;

        @Nonnull
        private final String className;

        @Nonnull
        private final String methodName;

        @Nonnull
        private final String[] argumentTypeNames;

        @Nullable
        private Method method;

        private MethodEntry( long moduleId,
                             @Nonnull String className,
                             @Nonnull String methodName,
                             @Nonnull String[] argumentTypeNames )
        {
            this.moduleId = moduleId;
            this.className = className;
            this.methodName = methodName;
            this.argumentTypeNames = argumentTypeNames;
        }

        public Method getMethod() throws ClassNotFoundException
        {
            if( this.method == null )
            {
                synchronized( this )
                {
                    if( this.method == null )
                    {
                        // resolve declaring class
                        Class<?> declaringClass;
                        ClassLoader classLoader;
                        try
                        {
                            Bundle entryBundle = requireBundleContext( getClass() ).getBundle( this.moduleId );
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
