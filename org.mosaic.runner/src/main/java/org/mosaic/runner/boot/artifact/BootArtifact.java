package org.mosaic.runner.boot.artifact;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.mosaic.runner.util.SystemPropertyUtils;

/**
 * @author arik
 */
public class BootArtifact {

    private final String type;

    private final String coordinates;

    public BootArtifact( String spec ) {

        int split = spec.indexOf( ':' );
        if( split < 0 ) {
            throw new IllegalArgumentException( "Illegal boot artifact reference '" + spec + "': must be in the format of \"type:coordinates\"" );
        }

        this.type = SystemPropertyUtils.resolvePlaceholders( spec.substring( 0, split ) );
        this.coordinates = SystemPropertyUtils.resolvePlaceholders( spec.substring( split + 1 ) );
    }

    public BootArtifact( String type, String coordinates ) {
        this.type = type;
        this.coordinates = coordinates;
    }

    public String getType() {
        return type;
    }

    public String getCoordinates() {
        return coordinates;
    }

    @SuppressWarnings( "RedundantIfStatement" )
    @Override
    public boolean equals( Object o ) {
        if( this == o ) {
            return true;
        }
        if( o == null || getClass() != o.getClass() ) {
            return false;
        }

        BootArtifact that = ( BootArtifact ) o;

        if( !coordinates.equals( that.coordinates ) ) {
            return false;
        }
        if( !type.equals( that.type ) ) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + coordinates.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return new ToStringBuilder( this )
                .append( "type", this.type )
                .append( "coordinates", this.coordinates )
                .toString();
    }
}
