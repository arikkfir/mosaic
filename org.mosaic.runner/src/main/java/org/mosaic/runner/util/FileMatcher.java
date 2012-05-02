package org.mosaic.runner.util;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;

/**
 * @author arik
 */
public class FileMatcher
{
    public static List<File> find( String pattern )
    {
        int patternStart = -1;

        int starIndex = pattern.indexOf( '*' );
        if( starIndex >= 0 )
        {
            patternStart = starIndex;
        }

        int questionMarkIndex = pattern.indexOf( '?' );
        if( questionMarkIndex >= 0 )
        {
            if( starIndex < 0 || questionMarkIndex < starIndex )
            {
                patternStart = questionMarkIndex;
            }
        }

        if( patternStart < 0 )
        {
            return Arrays.asList( new File( pattern ) );
        }

        int lastIndex = -1;
        int index = pattern.indexOf( '/' );
        while( index >= 0 && index < patternStart )
        {
            lastIndex = index;
            index = pattern.indexOf( '/', lastIndex + 1 );
        }
        if( lastIndex < 0 )
        {
            throw new IllegalArgumentException( "Illegal resource pattern: " + pattern );
        }

        File root = new File( pattern.substring( 0, lastIndex ) );
        List<File> files = new FileMatcher( root )._find( pattern.substring( lastIndex + 1 ) );
        Collections.sort( files );
        return files;
    }

    private final File file;

    private FileMatcher( File file )
    {
        this.file = file;
    }

    private List<File> _find( String pattern )
    {
        if( this.file.isFile( ) )
        {
            throw new IllegalStateException( "_find(...) is only supported on directory resources" );
        }

        String[] tokens = pattern.split( "/" );

        File[] matchingFiles =
                this.file.listFiles( ( FileFilter ) new WildcardFileFilter( tokens[ 0 ], IOCase.SYSTEM ) );
        if( matchingFiles == null )
        {
            return Collections.emptyList( );
        }

        List<File> matches = new LinkedList<>( );
        if( tokens.length == 1 )
        {
            for( File file : matchingFiles )
            {
                if( file.isFile( ) )
                {
                    matches.add( file );
                }
            }
        }
        else
        {
            String next = StringUtils.join( tokens, '/', 1, tokens.length );
            for( File file : matchingFiles )
            {
                if( file.isDirectory( ) )
                {
                    matches.addAll( new FileMatcher( file )._find( next ) );
                }
            }
        }
        return matches;
    }
}
