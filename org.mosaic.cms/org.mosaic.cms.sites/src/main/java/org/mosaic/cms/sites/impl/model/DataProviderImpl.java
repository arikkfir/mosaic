package org.mosaic.cms.sites.impl.model;

import org.mosaic.cms.DataProvider;
import org.mosaic.cms.sites.impl.DataProviderRegistry;

/**
 * @author arik
 */
public class DataProviderImpl extends BaseModel implements DataProvider
{
    private String type;

    private boolean required;

    private DataProviderRegistry dataProviderRegistry;

    @Override
    public String getType()
    {
        return this.type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    @Override
    public boolean isRequired()
    {
        return this.required;
    }

    public void setRequired( boolean required )
    {
        this.required = required;
    }

    @Override
    public Object getData( Parameters parameters ) throws Exception
    {
        if( this.dataProviderRegistry == null )
        {
            throw new IllegalStateException( String.format( "Data provider registry has not been set on data provider '%s'", getName() ) );
        }
        else
        {
            return this.dataProviderRegistry.getData( this.type, parameters );
        }
    }
}
