package org.mosaic.web;

/**
 * @author arik
 */
public interface HttpCookie {

    String getName( );

    String getValue( );

    void setValue( String value );

    String getDomain( );

    void setDomain( String domain );

    String getPath( );

    void setPath( String path );

    Integer getMaxAge( );

    void setMaxAge( Integer maxAge );

    boolean getSecure( );

    void setSecure( boolean secure );

    String getComment( );

    void setComment( String comment );

    boolean getHttpOnly( );

    void setHttpOnly( boolean httpOnly );

    int getVersion( );

    void setVersion( int version );

}
