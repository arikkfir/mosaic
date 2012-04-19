package org.mosaic.web;

/**
 * @author arik
 */
public interface HttpCookie {

    String getName();

    String getValue();

    void setValue( String value );

    String getDomain();

    void setDomain( String domain );

    String getPath();

    void setPath( String path );

    Integer getMaxAge();

    void setMaxAge( Integer maxAge );

    boolean isSecure();

    String getComment();

    void setComment( String comment );

}
