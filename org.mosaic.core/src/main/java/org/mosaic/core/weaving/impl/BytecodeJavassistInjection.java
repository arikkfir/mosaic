package org.mosaic.core.weaving.impl;

import java.util.Collection;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.NotFoundException;
import org.mosaic.core.components.Inject;
import org.mosaic.core.util.Nonnull;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WovenClass;

import static javassist.Modifier.isFinal;
import static javassist.Modifier.isStatic;

/**
 * @author arik
 */
class BytecodeJavassistInjection
{
    void weaveFieldInjections( @Nonnull WovenClass wovenClass, @Nonnull CtClass ctClass )
    {
        // list of constructors that should be weaved with our modifications
        Collection<CtConstructor> superCallingConstructors = BytecodeUtil.getSuperCallingConstructors( wovenClass, ctClass );

        // iterate *declared* fields (only!) of this class, add injection code if they have @Inject
        for( CtField field : ctClass.getDeclaredFields() )
        {
            try
            {
                int modifiers = field.getModifiers();
                if( !isStatic( modifiers ) && !isFinal( modifiers ) )
                {
                    if( field.hasAnnotation( Inject.class ) )
                    {
                        BytecodeUtil.addBeforeBody(
                                superCallingConstructors,
                                BytecodeUtil.javaCode( "this.%s = (%s) WeavingSpi.getInstanceFieldValue( %dl, %dl, %s.class, \"%s\" );",
                                                       field.getName(),
                                                       field.getType().getName(),
                                                       wovenClass.getBundleWiring().getBundle().getBundleId(),
                                                       BytecodeUtil.getBundleRevisionNumber( wovenClass.getBundleWiring().getRevision() ),
                                                       ctClass.getName(),
                                                       field.getName() )
                        );
                    }
                }
            }
            catch( NotFoundException ignore )
            {
                // simply not weaving the class; it won't load anyway...
            }
            catch( WeavingException e )
            {
                throw e;
            }
            catch( Throwable e )
            {
                throw new WeavingException( "could not weave @Inject for field '" + field.getName() + "' of '" + wovenClass.getClassName() + "' in " + BytecodeUtil.toString( wovenClass.getBundleWiring().getRevision() ), e );
            }
        }
    }
}
