package org.mosaic.util.osgi;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author arik
 */
public class FilterBuilderTest
{
    @Test
    public void testFilterBuilder()
    {
        FilterBuilder fb = new FilterBuilder();
        fb.addEquals( "key", "value" );
        assertThat( fb.toString(), containsString( "key=value" ) );
        assertThat( fb.toString(), equalTo( "(key=value)" ) );

        fb.addEquals( "k2", "v2" );
        assertThat( fb.toString(), containsString( "key=value" ) );
        assertThat( fb.toString(), containsString( "k2=v2" ) );

        fb.addClass( "  a.b.Clazz " );
        assertThat( fb.toString(), containsString( "key=value" ) );
        assertThat( fb.toString(), containsString( "k2=v2" ) );
        assertThat( fb.toString(), containsString( "(objectClass=a.b.Clazz)" ) );

        fb.addClass( FilterBuilderTest.class );
        assertThat( fb.toString(), containsString( "key=value" ) );
        assertThat( fb.toString(), containsString( "k2=v2" ) );
        assertThat( fb.toString(), containsString( "(objectClass=a.b.Clazz)" ) );
        assertThat( fb.toString(), containsString( "objectClass="+ FilterBuilderTest.class.getName() ) );

        fb.addEquals( "k3", "null" );
        assertThat( fb.toString(), containsString( "key=value" ) );
        assertThat( fb.toString(), containsString( "k2=v2" ) );
        assertThat( fb.toString(), containsString( "(objectClass=a.b.Clazz)" ) );
        assertThat( fb.toString(), containsString( "objectClass="+ FilterBuilderTest.class.getName() ) );
        assertThat( fb.toString(), containsString( "k3=null" ) );

        fb.add(" k4=v4 ");
        assertThat( fb.toString(), containsString( "key=value" ) );
        assertThat( fb.toString(), containsString( "k2=v2" ) );
        assertThat( fb.toString(), containsString( "(objectClass=a.b.Clazz)" ) );
        assertThat( fb.toString(), containsString( "objectClass="+ FilterBuilderTest.class.getName() ) );
        assertThat( fb.toString(), containsString( "k3=null" ) );
        assertThat( fb.toString(), containsString( "(k4=v4)" ) );
    }
}
