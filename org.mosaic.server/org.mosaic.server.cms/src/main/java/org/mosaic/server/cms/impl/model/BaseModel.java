package org.mosaic.server.cms.impl.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.mosaic.cms.DataProvider;
import org.mosaic.cms.support.*;
import org.mosaic.util.collection.MapAccessor;
import org.mosaic.util.collection.MapWrapper;
import org.springframework.expression.Expression;

/**
 * @author arik
 */
public abstract class BaseModel implements DataContainer, Filtered, Named, PropertiesProvider, Secured
{
    private Map<String, DataProviderImpl> dataProviders = new HashMap<>();

    private Expression filterExpression;

    private Expression securityExpression;

    private String name;

    private String displayName;

    private MapWrapper<String, String> properties = new MapWrapper<>();

    @Override
    public DataProvider getDataProvider( String name )
    {
        return this.dataProviders.get( name );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public Map<String, DataProvider> getDataProviders()
    {
        return ( Map ) this.dataProviders;
    }

    public synchronized void addDataProvider( DataProviderImpl dataProvider )
    {
        Map<String, DataProviderImpl> newDataProviders = new HashMap<>( this.dataProviders );
        newDataProviders.put( dataProvider.getName(), dataProvider );
        this.dataProviders = newDataProviders;
    }

    @Override
    public Expression getFilterExpression()
    {
        return this.filterExpression;
    }

    public void setFilterExpression( Expression filterExpression )
    {
        this.filterExpression = filterExpression;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @Override
    public String getDisplayName()
    {
        return this.displayName;
    }

    public void setDisplayName( String displayName )
    {
        this.displayName = displayName;
    }

    @Override
    public MapAccessor<String, String> getProperties()
    {
        return this.properties;
    }

    public synchronized void setProperties( Map<String, String> properties )
    {
        this.properties.setMap( Collections.unmodifiableMap( new HashMap<>( properties ) ) );
    }

    @Override
    public Expression getSecurityExpression()
    {
        return this.securityExpression;
    }

    public void setSecurityExpression( Expression securityExpression )
    {
        this.securityExpression = securityExpression;
    }
}
