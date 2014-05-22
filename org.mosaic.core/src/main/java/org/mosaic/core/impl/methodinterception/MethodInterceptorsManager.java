package org.mosaic.core.impl.methodinterception;

import java.lang.reflect.Method;
import java.util.*;
import org.mosaic.core.MethodInterceptor;
import org.mosaic.core.ServiceListener;
import org.mosaic.core.ServiceManager;
import org.mosaic.core.ServiceRegistration;
import org.mosaic.core.impl.ServerStatus;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.mosaic.core.util.logging.Logging;
import org.mosaic.core.util.workflow.Status;
import org.mosaic.core.util.workflow.TransitionAdapter;
import org.slf4j.Logger;

import static java.util.Arrays.copyOf;

/**
 * @author arik
 */
public class MethodInterceptorsManager extends TransitionAdapter
{
    private static final Logger LOG = Logging.getLogger();

    @Nonnull
    private final Logger logger;

    @Nonnull
    private final ReadWriteLock lock;

    @Nonnull
    private final ServiceManager serviceManager;

    @Nonnull
    private final MethodInterceptor.BeforeInvocationDecision continueDecision = new ContinueDecision();

    @Nonnull
    private final MethodInterceptor.BeforeInvocationDecision abortDecision = new AbortDecision();

    @Nonnull
    private final ServiceListener<MethodInterceptor> methodInterceptorServiceListener = new MethodInterceptorServiceListener();

    @Nonnull
    private final ThreadLocal<Deque<InvocationContext>> contextHolder = new ThreadLocal<Deque<InvocationContext>>()
    {
        @Override
        protected Deque<InvocationContext> initialValue()
        {
            return new LinkedList<>();
        }
    };

    @Nullable
    private Map<Method, List<InterestedMethodInterceptorEntry>> methodInterceptorsForMethods;

    @Nullable
    private List<MethodInterceptor> methodInterceptors;

    public MethodInterceptorsManager( @Nonnull Logger logger,
                                      @Nonnull ReadWriteLock lock,
                                      @Nonnull ServiceManager serviceManager )
    {
        this.logger = logger;
        this.lock = lock;
        this.serviceManager = serviceManager;
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this ).toString();
    }

    @Override
    public void execute( @Nonnull Status origin, @Nonnull Status target ) throws Exception
    {
        if( target == ServerStatus.STARTED )
        {
            initialize();
        }
        else if( target == ServerStatus.STOPPED )
        {
            shutdown();
        }
    }

    @Override
    public void revert( @Nonnull Status origin, @Nonnull Status target ) throws Exception
    {
        if( target == ServerStatus.STARTED )
        {
            shutdown();
        }
    }

    public boolean beforeInvocation( @Nonnull MethodEntry methodEntry,
                                     @Nullable Object object,
                                     @Nonnull Object[] arguments ) throws Throwable
    {
        // create context for method invocation and push to stack
        InvocationContext context = new InvocationContext( methodEntry, object, arguments );
        this.contextHolder.get().push( context );

        // invoke interceptors "before" action, returning true if method should proceed, false if circumvent method and request a call to "after" action
        return context.beforeInvocation();
    }

    @Nullable
    public Object afterAbortedInvocation() throws Throwable
    {
        return this.contextHolder.get().peek().afterInvocation();
    }

    @Nullable
    public Object afterSuccessfulInvocation( @Nullable Object returnValue ) throws Throwable
    {
        InvocationContext context = this.contextHolder.get().peek();
        context.returnValue = returnValue;
        return context.afterInvocation();
    }

    @Nullable
    public Object afterThrowable( @Nonnull Throwable throwable ) throws Throwable
    {
        InvocationContext context = this.contextHolder.get().peek();
        context.throwable = throwable;
        return context.afterInvocation();
    }

    public void cleanup( @Nonnull MethodEntry methodEntry )
    {
        Deque<InvocationContext> deque = this.contextHolder.get();
        if( deque.isEmpty() )
        {
            LOG.error( "STACK EMPTY! received method: {}", methodEntry );
        }
        else if( !deque.peek().methodEntry.equals( methodEntry ) )
        {
            LOG.error( "STACK DIRTY!\n" +
                       "    On stack: {}\n" +
                       "    Received: {}",
                       deque.peek().methodEntry, methodEntry
            );
        }
        else
        {
            deque.pop();
        }
    }

    @Nonnull
    private List<InterestedMethodInterceptorEntry> getMethodInterceptors( @Nonnull Method method )
    {
        this.lock.acquireReadLock();
        try
        {
            Map<Method, List<InterestedMethodInterceptorEntry>> methodInterceptorsForMethods = this.methodInterceptorsForMethods;
            List<MethodInterceptor> methodInterceptors = this.methodInterceptors;

            if( methodInterceptorsForMethods == null || methodInterceptors == null )
            {
                throw new IllegalStateException( "server not started" );
            }

            List<InterestedMethodInterceptorEntry> interceptors = methodInterceptorsForMethods.get( method );
            if( interceptors == null )
            {
                this.lock.releaseReadLock();
                this.lock.acquireWriteLock();
                try
                {
                    // check again since it might have been created just after releasing the read lock and acquiring the write lock
                    interceptors = methodInterceptorsForMethods.get( method );
                    if( interceptors == null )
                    {
                        Map<String, Object> context = new HashMap<>();
                        for( MethodInterceptor methodInterceptor : this.methodInterceptors )
                        {
                            context.clear();
                            if( methodInterceptor.interestedIn( method, context ) )
                            {
                                if( interceptors == null )
                                {
                                    interceptors = new LinkedList<>();
                                }
                                interceptors.add( new InterestedMethodInterceptorEntry( methodInterceptor, new HashMap<>( context ) ) );
                            }
                        }
                        if( interceptors == null )
                        {
                            interceptors = Collections.emptyList();
                        }
                        methodInterceptorsForMethods.put( method, interceptors );
                    }
                }
                finally
                {
                    this.lock.releaseWriteLock();
                    this.lock.acquireReadLock();
                }
            }
            return interceptors;
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    private void initialize()
    {
        this.logger.debug( "Initializing method interceptors manager" );
        this.methodInterceptorsForMethods = new HashMap<>();
        this.methodInterceptors = new LinkedList<>();
        this.serviceManager.addListener( this.methodInterceptorServiceListener, MethodInterceptor.class );
    }

    private void shutdown() throws InterruptedException
    {
        this.logger.debug( "Shutting down method interceptors manager" );
        this.serviceManager.removeListener( this.methodInterceptorServiceListener );
        this.methodInterceptors = null;
        this.methodInterceptorsForMethods = null;
    }

    private void clearMethodInterceptorsCache()
    {
        this.lock.acquireWriteLock();
        try
        {
            Map<Method, List<InterestedMethodInterceptorEntry>> methodInterceptors = this.methodInterceptorsForMethods;
            if( methodInterceptors != null )
            {
                methodInterceptors.clear();
            }
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    private class InterestedMethodInterceptorEntry
    {
        @Nonnull
        private final MethodInterceptor target;

        @Nonnull
        private final Map<String, Object> interceptorContext;

        private InterestedMethodInterceptorEntry( @Nonnull MethodInterceptor target,
                                                  @Nonnull Map<String, Object> interceptorContext )
        {
            this.target = target;
            this.interceptorContext = interceptorContext;
        }
    }

    private class MethodInterceptorServiceListener implements ServiceListener<MethodInterceptor>
    {
        @Override
        public void serviceRegistered( @Nonnull ServiceRegistration<MethodInterceptor> registration )
        {
            clearMethodInterceptorsCache();
        }

        @Override
        public void serviceUnregistered( @Nonnull ServiceRegistration<MethodInterceptor> registration,
                                         @Nonnull MethodInterceptor service )
        {
            clearMethodInterceptorsCache();
        }
    }

    private class InvocationContext extends HashMap<String, Object>
    {
        @Nonnull
        private final MethodEntry methodEntry;

        @Nullable
        private final Object object;

        @Nonnull
        private final Object[] arguments;

        @Nonnull
        private final List<InterestedMethodInterceptorEntry> invokedInterceptors = new LinkedList<>();

        @Nullable
        private Map<InterestedMethodInterceptorEntry, Map<String, Object>> interceptorInvocationContexts = null;

        @Nullable
        private BeforeMethodInvocationImpl beforeInvocation;

        @Nullable
        private AfterMethodInvocationImpl afterInvocation;

        @Nullable
        private AfterMethodExceptionImpl afterThrowable;

        @Nullable
        private Throwable throwable;

        @Nullable
        private Object returnValue;

        private InvocationContext( @Nonnull MethodEntry methodEntry,
                                   @Nullable Object object,
                                   @Nonnull Object[] arguments ) throws ClassNotFoundException
        {
            super( 5 );
            this.methodEntry = methodEntry;
            this.object = object;
            this.arguments = arguments;
        }

        private boolean beforeInvocation()
        {
            for( InterestedMethodInterceptorEntry interceptorEntry : getMethodInterceptors( this.methodEntry.getMethod() ) )
            {
                MethodInterceptor interceptor = interceptorEntry.target;
                try
                {
                    BeforeMethodInvocationImpl invocation = getBeforeInvocation();
                    invocation.methodInterceptorEntry = interceptorEntry;

                    // invoke interceptor
                    MethodInterceptor.BeforeInvocationDecision decision = interceptor.beforeInvocation( invocation );

                    // if interceptor succeeded, add it to the list of interceptors to invoke on "after" action
                    // note that we add it to the start, so we can invoke them in reverse order in "after" action
                    this.invokedInterceptors.add( 0, interceptorEntry );

                    if( decision == MethodInterceptorsManager.this.abortDecision )
                    {
                        return false;
                    }
                    else if( decision != MethodInterceptorsManager.this.continueDecision )
                    {
                        throw new IllegalStateException( "method interceptor \"before\" did not use MethodInvocation.continue/abort methods" );
                    }
                }
                catch( Throwable throwable )
                {
                    this.throwable = throwable;
                    this.returnValue = null;
                    return false;
                }
            }
            return true;
        }

        @Nullable
        private Object afterInvocation() throws Throwable
        {
            for( InterestedMethodInterceptorEntry interceptorEntry : this.invokedInterceptors )
            {
                MethodInterceptor interceptor = interceptorEntry.target;
                try
                {
                    if( this.throwable != null )
                    {
                        AfterMethodExceptionImpl invocation = getAfterThrowable();
                        invocation.methodInterceptorEntry = interceptorEntry;
                        this.returnValue = interceptor.afterThrowable( invocation );
                    }
                    else
                    {
                        AfterMethodInvocationImpl invocation = getAfterInvocation();
                        invocation.methodInterceptorEntry = interceptorEntry;
                        this.returnValue = interceptor.afterInvocation( invocation );
                    }
                    this.throwable = null;
                }
                catch( Throwable throwable )
                {
                    this.throwable = throwable;
                    this.returnValue = null;
                }
            }

            if( this.throwable != null )
            {
                throw this.throwable;
            }
            else
            {
                return this.returnValue;
            }
        }

        @Nonnull
        private BeforeMethodInvocationImpl getBeforeInvocation()
        {
            if( this.beforeInvocation == null )
            {
                this.beforeInvocation = new BeforeMethodInvocationImpl( this );
            }
            return this.beforeInvocation;
        }

        @Nonnull
        private AfterMethodInvocationImpl getAfterInvocation()
        {
            if( this.afterInvocation == null )
            {
                this.afterInvocation = new AfterMethodInvocationImpl( this );
            }
            return this.afterInvocation;
        }

        @Nonnull
        private AfterMethodExceptionImpl getAfterThrowable()
        {
            if( this.afterThrowable == null )
            {
                this.afterThrowable = new AfterMethodExceptionImpl( this );
            }
            return this.afterThrowable;
        }

        @Nonnull
        private Map<String, Object> getInterceptorInvocationContext( @Nonnull InterestedMethodInterceptorEntry interceptorEntry )
        {
            if( this.interceptorInvocationContexts == null )
            {
                this.interceptorInvocationContexts = new HashMap<>();
            }

            Map<String, Object> context = this.interceptorInvocationContexts.get( interceptorEntry );
            if( context == null )
            {
                context = new HashMap<>();
                this.interceptorInvocationContexts.put( interceptorEntry, context );
            }
            return context;
        }
    }

    private class ContinueDecision implements MethodInterceptor.BeforeInvocationDecision
    {
    }

    private class AbortDecision implements MethodInterceptor.BeforeInvocationDecision
    {
    }

    private abstract class AbstractMethodInvocation implements MethodInterceptor.MethodInvocation
    {
        @Nonnull
        protected final InvocationContext context;

        @Nullable
        protected InterestedMethodInterceptorEntry methodInterceptorEntry;

        protected AbstractMethodInvocation( @Nonnull InvocationContext context )
        {
            this.context = context;
        }

        @Nonnull
        @Override
        public final Map<String, Object> getInterceptorContext()
        {
            if( this.methodInterceptorEntry == null )
            {
                throw new IllegalStateException( "Interceptor context not set on invocation!" );
            }
            return this.methodInterceptorEntry.interceptorContext;
        }

        @Nonnull
        @Override
        public final Map<String, Object> getInvocationContext()
        {
            if( this.methodInterceptorEntry == null )
            {
                throw new IllegalStateException( "Interceptor context not set on invocation!" );
            }
            return this.context.getInterceptorInvocationContext( this.methodInterceptorEntry );
        }

        @Nonnull
        @Override
        public final Method getMethod()
        {
            return this.context.methodEntry.getMethod();
        }

        @Nullable
        @Override
        public final Object getObject()
        {
            return this.context.object;
        }
    }

    private class BeforeMethodInvocationImpl extends AbstractMethodInvocation
            implements MethodInterceptor.BeforeMethodInvocation
    {
        public BeforeMethodInvocationImpl( @Nonnull InvocationContext context )
        {
            super( context );
        }

        @Nonnull
        @Override
        public Object[] getArguments()
        {
            return this.context.arguments;
        }

        @Nonnull
        @Override
        public MethodInterceptor.BeforeInvocationDecision continueInvocation()
        {
            return MethodInterceptorsManager.this.continueDecision;
        }

        @Nonnull
        @Override
        public MethodInterceptor.BeforeInvocationDecision abort( @Nullable Object returnValue )
        {
            this.context.returnValue = returnValue;
            return MethodInterceptorsManager.this.abortDecision;
        }
    }

    private class AfterMethodInvocationImpl extends AbstractMethodInvocation
            implements MethodInterceptor.AfterMethodInvocation
    {
        @Nonnull
        private final Object[] arguments;

        public AfterMethodInvocationImpl( @Nonnull InvocationContext context )
        {
            super( context );
            this.arguments = copyOf( this.context.arguments, this.context.arguments.length );
        }

        @Nonnull
        @Override
        public Object[] getArguments()
        {
            return this.arguments;
        }

        @Nullable
        @Override
        public Object getReturnValue()
        {
            return this.context.returnValue;
        }
    }

    private class AfterMethodExceptionImpl extends AbstractMethodInvocation
            implements MethodInterceptor.AfterMethodException
    {
        @Nonnull
        private final Object[] arguments;

        public AfterMethodExceptionImpl( @Nonnull InvocationContext context )
        {
            super( context );
            this.arguments = copyOf( this.context.arguments, this.context.arguments.length );
        }

        @Nonnull
        @Override
        public Object[] getArguments()
        {
            return this.arguments;
        }

        @Nonnull
        @Override
        public Throwable getThrowable()
        {
            Throwable throwable = this.context.throwable;
            if( throwable == null )
            {
                throw new IllegalStateException( "No throwable found" );
            }
            else
            {
                return throwable;
            }
        }
    }
}
