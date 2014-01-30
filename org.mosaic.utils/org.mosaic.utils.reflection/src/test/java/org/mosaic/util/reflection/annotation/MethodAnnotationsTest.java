package org.mosaic.util.reflection.annotation;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Collection;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.mosaic.util.reflection.MethodAnnotations;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

/**
 * @author arik
 */
public class MethodAnnotationsTest
{
    private final Method method;

    public MethodAnnotationsTest() throws NoSuchMethodException
    {
        this.method = Sub2.class.getDeclaredMethod( "method" );
    }

    @Before
    public void setUp() throws Exception
    {
        MethodAnnotations.clearCaches();
    }

    @Test
    public void testGetAnnotations() throws NoSuchMethodException
    {
        Collection<Annotation> annotations = MethodAnnotations.getAnnotations( this.method );
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
        Optional<C> coptional = MethodAnnotations.getMetaAnnotation( this.method, C.class );
        assertTrue( coptional.isPresent() );
        assertEquals( coptional.get().annotationType(), C.class );

        Optional<D> doptional = MethodAnnotations.getMetaAnnotation( this.method, D.class );
        assertFalse( doptional.isPresent() );
    }

    @Test
    public void testGetMetaAnnotationTarget()
    {
        Optional<Annotation> aoptional = MethodAnnotations.getMetaAnnotationTarget( this.method, C.class );
        assertTrue( aoptional.isPresent() );
        assertEquals( aoptional.get().annotationType(), A.class );

        Optional<Annotation> doptional = MethodAnnotations.getMetaAnnotationTarget( this.method, D.class );
        assertFalse( doptional.isPresent() );
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

    public static class Super
    {
        @A
        void method()
        {
            // no-op
        }
    }

    public static class Sub1 extends Super
    {
    }

    public static class Sub2 extends Sub1
    {
        @B
        void method()
        {
            // no-op
        }
    }
}
