package org.mosaic.web;

/**
 * @author arik
 */
public interface HttpCookie {

    String getName();

    Object getValue();

    void setValue( Object value );

    String getDomain();

    void setDomain( String domain );

    String getPath();

    void setPath();

    Integer getMaxAge();

    void setMaxAge( Integer maxAge );

    boolean isSecure();

    String getComment();

    void setComment( String comment );

}
