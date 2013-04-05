package org.mosaic.util.reflection.impl;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author arik
 */
public class AnnotationUtilsTest
{
    @Test
    public void testIsAnnotatedWith() throws NoSuchMethodException
    {
        Method method = AnnotatedClass.class.getDeclaredMethod( "annotatedMethod" );

        Assert.assertNotNull( AnnotationUtils.getAnnotation( method, C.class ) );
        Assert.assertNull( AnnotationUtils.getAnnotation( method, D.class ) );

        A a = AnnotationUtils.getAnnotation( method, A.class );
        Assert.assertNotNull( a );
        Assert.assertEquals( 2, a.value() );
    }

    @Retention( RetentionPolicy.RUNTIME )
    public static @interface A
    {
        int value();
    }

    @Retention( RetentionPolicy.RUNTIME )
    @A( 1 )
    @C
    public static @interface B
    {
    }

    @Retention( RetentionPolicy.RUNTIME )
    @A( 2 )
    @B
    public static @interface C
    {
    }

    @Retention( RetentionPolicy.RUNTIME )
    @A( 2 )
    @B
    public static @interface D
    {
    }

    public class AnnotatedClass
    {
        @SuppressWarnings( "UnusedDeclaration" )
        @C
        public void annotatedMethod()
        {
        }
    }
}
