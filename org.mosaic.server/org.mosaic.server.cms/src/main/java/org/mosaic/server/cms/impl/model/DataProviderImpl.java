package org.mosaic.server.cms.impl.model;

import org.mosaic.cms.DataProvider;
import org.mosaic.cms.MissingRequiredDataException;
import org.mosaic.server.cms.impl.DataProviderRegistry;
import org.w3c.dom.Element;

/**
 * @author arik
 */
public class DataProviderImpl extends BaseModel implements DataProvider
{
    private final String type;

    private final boolean required;

    private final DataProviderRegistry dataProviderRegistry;

    public DataProviderImpl( DataProviderRegistry dataProviderRegistry, Element dataProviderElement )
    {
        this.type = dataProviderElement.getAttribute( "type" );
        this.required = Boolean.valueOf( dataProviderElement.getAttribute( "required" ) );
        this.dataProviderRegistry = dataProviderRegistry;

        DomModelUtils.setNames( this, dataProviderElement );
        DomModelUtils.setSecurityExpression( this, dataProviderElement );
    }

    @Override
    public String getType()
    {
        return this.type;
    }

    @Override
    public boolean isRequired()
    {
        return this.required;
    }

    @Override
    public Object getData( Parameters parameters ) throws Exception
    {
        if( this.dataProviderRegistry == null )
        {
            throw new IllegalStateException( String.format( "Data provider registry has not been set on data provider '%s'", getName() ) );
        }

        Object data = this.dataProviderRegistry.getData( this.type, parameters );
        if( data != null || !this.required )
        {
            return data;
        }
        else
        {
            throw new MissingRequiredDataException( "Data is missing: " + this.type + " for '" + getName() + "'" );
        }
    }
}
