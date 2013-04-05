package org.mosaic.security;

/**
 * @author arik
 */
public interface Principal
{
    String getType();

    void attach( User user );

    void detach( User user );
}
