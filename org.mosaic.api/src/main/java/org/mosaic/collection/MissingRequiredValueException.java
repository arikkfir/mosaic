package org.mosaic.collection;

/**
 * @author arik
 */
public class MissingRequiredValueException extends RuntimeException {

    private final String key;

    public MissingRequiredValueException( String key ) {
        super( "Key '" + key + "' has no value" );
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
