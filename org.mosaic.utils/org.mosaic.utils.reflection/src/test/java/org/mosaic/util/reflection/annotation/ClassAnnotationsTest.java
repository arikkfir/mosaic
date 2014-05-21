package org.mosaic.util.reflection.annotation;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import org.mosaic.core.util.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.mosaic.util.reflection.ClassAnnotations;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

/**
 * @author arik
 */
public class ClassAnnotationsTest
{
    @Before
    public void setUp() throws Exception
    {
        ClassAnnotations.clearCaches();
    }

    @Test
    public void testGetAnnotations()
    {
        Collection<Annotation> annotations = ClassAnnotations.getAnnotations( Annotated.class );
        assertNotNull( annotations );
        assertThat( annotations.size(), equalTo( 2 ) );

        Collection<Class<? extends Annotation>> annotationTypes = Collections2.transform( annotations, new Function<Annotation, Class<? extends Annotation>>()
        {
            @Nullable
            @Override
            public Class<? extends Annotation> apply( @Nullable Annotation input )
            {
                return input == null ? null : input.annotationType();
            }
        } );
        assertTrue( annotationTypes.contains( A.class ) );
        assertTrue( annotationTypes.contains( B.class ) );
    }

    @Test
    public void testGetMetaAnnotation()
    {
        C coptional = ClassAnnotations.getMetaAnnotation( Annotated.class, C.class );
        assertTrue( coptional != null );
        assertEquals( coptional.annotationType(), C.class );

        D doptional = ClassAnnotations.getMetaAnnotation( Annotated.class, D.class );
        assertFalse( doptional != null );
    }

    @Test
    public void testGetMetaAnnotationTarget()
    {
        Annotation aoptional = ClassAnnotations.getMetaAnnotationTarget( Annotated.class, C.class );
        assertTrue( aoptional != null );
        assertEquals( aoptional.annotationType(), A.class );

        Annotation doptional = ClassAnnotations.getMetaAnnotationTarget( Annotated.class, D.class );
        assertFalse( doptional != null );
    }

    @C
    @Retention( RetentionPolicy.RUNTIME )
    public static @interface A
    {
    }

    @A
    @Retention( RetentionPolicy.RUNTIME )
    public static @interface B
    {
    }

    @B
    @Retention( RetentionPolicy.RUNTIME )
    public static @interface C
    {
    }

    @B
    @Retention( RetentionPolicy.RUNTIME )
    public static @interface D
    {
    }

    @A
    @B
    public static class Annotated
    {
    }
}
