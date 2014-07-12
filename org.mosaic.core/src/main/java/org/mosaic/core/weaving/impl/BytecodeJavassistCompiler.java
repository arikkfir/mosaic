package org.mosaic.core.weaving.impl;

import javassist.CtClass;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WovenClass;

/**
 * @author arik
 */
class BytecodeJavassistCompiler implements BytecodeCompiler
{
    /*
        Javassist macros:
            $0              "this"
            $1, $2          method arguments (Object[])
            $$              actual arguments (not the array - eg. myMethod($$) is like myMethod($1, $2, ...) )
            $cflow(...)     the "cflow" variable
            $r              the result type (used in a cast expression)
            $w              the wrapper type (used in a cast expression)
            $_              resulting value
            $sig            an array of java.lang.Class representing formal parameter types
            $type           java.lang.Class object representing the formal result type
            $class          java.lang.Class object representing the class being edited
     */

    @Nonnull
    private final BytecodeJavassistMethodInterception interception = new BytecodeJavassistMethodInterception();

    @Nonnull
    private final BytecodeJavassistInjection injection = new BytecodeJavassistInjection();

    @Nonnull
    private final BytecodeJavassistValidation validation = new BytecodeJavassistValidation();

    @Nullable
    @Override
    public byte[] compile( @Nonnull WovenClass wovenClass )
    {
        // weave the mother..!
        CtClass ctClass = BytecodeUtil.loadConcreteClass( wovenClass );
        if( ctClass != null )
        {
            // weave instance initializer that wires @Component, @Service, etc to fields
            this.injection.weaveFieldInjections( wovenClass, ctClass );

            // weave support for @Nonnull checks to all methods
            this.validation.weaveNonnullChecks( wovenClass, ctClass );

            // weave method interception
            this.interception.weaveMethodsInterception( wovenClass, ctClass );

            // if any weaving was done - compile the changes to the bytecode
            if( ctClass.isModified() )
            {
                try
                {
                    return ctClass.toBytecode();
                }
                catch( Throwable e )
                {
                    throw new WeavingException( "could not compile class '" + wovenClass.getClassName() + "': " + e.getMessage(), e );
                }
            }
        }

        // no weaving, return null
        return null;
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this ).toString();
    }
}
