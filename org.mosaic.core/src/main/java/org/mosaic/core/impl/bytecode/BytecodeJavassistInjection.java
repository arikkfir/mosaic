package org.mosaic.core.impl.bytecode;

import java.util.Collection;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.NotFoundException;
import org.mosaic.core.components.Inject;
import org.mosaic.core.modules.ModuleRevision;
import org.mosaic.core.util.Nonnull;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WovenClass;

import static javassist.Modifier.isFinal;
import static javassist.Modifier.isStatic;
import static org.mosaic.core.impl.bytecode.BytecodeUtil.javaCode;

/**
 * @author arik
 */
class BytecodeJavassistInjection
{
    void weaveFieldInjections( @Nonnull ModuleRevision moduleRevision,
                               @Nonnull WovenClass wovenClass,
                               @Nonnull CtClass ctClass )
    {
        // list of constructors that should be weaved with our modifications
        Collection<CtConstructor> superCallingConstructors = BytecodeUtil.getSuperCallingConstructors( moduleRevision, ctClass );

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
                                javaCode( "this.%s = (%s) ModulesSpi.getInstanceFieldValue( %dl, %dl, %s.class, \"%s\" );",
                                          field.getName(),
                                          field.getType().getName(),
                                          moduleRevision.getModule().getId(),
                                          moduleRevision.getId(),
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
                throw new WeavingException( "could not weave @Inject for field '" + field.getName() + "' of '" + wovenClass.getClassName() + "' in " + moduleRevision, e );
            }
        }
    }
}
