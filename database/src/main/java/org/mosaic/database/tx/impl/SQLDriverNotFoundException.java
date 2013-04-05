package org.mosaic.database.tx.impl;

import java.sql.SQLException;

/**
 * @author arik
 */
public class SQLDriverNotFoundException extends SQLException
{
    private final String url;

    public SQLDriverNotFoundException( String url )
    {
        super( "No suitable driver found for " + url, "08001" );
        this.url = url;
    }

    public String getUrl()
    {
        return url;
    }
}
