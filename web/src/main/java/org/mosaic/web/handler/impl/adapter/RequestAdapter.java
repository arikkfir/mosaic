package org.mosaic.web.handler.impl.adapter;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.HashMapEx;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.web.handler.annotation.Method;
import org.mosaic.web.handler.annotation.WebAppFilter;
import org.mosaic.web.handler.impl.RequestExecutionPlan;
import org.mosaic.web.handler.impl.action.Participator;
import org.mosaic.web.handler.impl.filter.Filter;
import org.mosaic.web.handler.impl.filter.HttpMethodFilter;
import org.mosaic.web.handler.impl.filter.PathFilter;
import org.mosaic.web.handler.impl.filter.WebApplicationFilter;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.mosaic.web.net.HttpMethod.GET;
import static org.mosaic.web.net.HttpMethod.POST;

/**
 * @author arik
 */
public class RequestAdapter implements Comparable<RequestAdapter>
{
    @Nonnull
    protected final ConversionService conversionService;

    private final long id;

    private int rank;

    @Nullable
    private Participator participator;

    @Nonnull
    private List<Filter> filters = emptyList();

    public RequestAdapter( @Nonnull ConversionService conversionService, long id )
    {
        this.conversionService = conversionService;
        this.id = id;
    }

    public final long getId()
    {
        return this.id;
    }

    public final int getRank()
    {
        return this.rank;
    }

    public void setRank( int rank )
    {
        this.rank = rank;
    }

    public void setParticipator( @Nullable Participator participator )
    {
        this.participator = participator;
    }

    public void addFilter( @Nonnull Filter filter )
    {
        List<Filter> newFilters = new LinkedList<>( this.filters );
        newFilters.add( filter );
        this.filters = unmodifiableList( newFilters );
    }

    public void addWebAppFilter( @Nullable WebAppFilter webAppFilterAnn )
    {
        if( webAppFilterAnn != null )
        {
            addFilter( new WebApplicationFilter( webAppFilterAnn.value() ) );
        }
    }

    public void addHttpMethodFilter( @Nullable Method methodAnn )
    {
        if( methodAnn != null )
        {
            addFilter( new HttpMethodFilter( methodAnn.value() ) );
        }
        else
        {
            addFilter( new HttpMethodFilter( GET, POST ) );
        }
    }

    public void addPathFilter( @Nullable Annotation annotation, boolean emptyPathListMatchesAll )
    {
        if( annotation != null )
        {
            try
            {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                java.lang.reflect.Method method = annotationType.getDeclaredMethod( "value" );
                addFilter( new PathFilter( emptyPathListMatchesAll, ( String[] ) method.invoke( annotation ) ) );
            }
            catch( Exception e )
            {
                throw new IllegalArgumentException( "Could not extract URL paths from '" + annotation + "': " + e.getMessage(), e );
            }
        }
    }

    @Override
    public int compareTo( @Nullable RequestAdapter o )
    {
        if( o == null )
        {
            return -1;
        }
        else if( getId() == o.getId() )
        {
            return 0;
        }
        else if( getRank() > o.getRank() )
        {
            return -1;
        }
        else if( getRank() < o.getRank() )
        {
            return 1;
        }
        else if( getId() < o.getId() )
        {
            return -1;
        }
        else if( getId() > o.getId() )
        {
            return 1;
        }
        throw new IllegalStateException( "should not happen" );
    }

    public final void apply( @Nonnull RequestExecutionPlan plan )
    {
        if( this.participator != null )
        {
            MapEx<String, Object> context = new HashMapEx<>( 50, this.conversionService );
            for( Filter filter : this.filters )
            {
                if( !filter.matches( plan, context ) )
                {
                    return;
                }
            }
            this.participator.apply( plan, context );
        }
    }
}
