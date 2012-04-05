package org.mosaic.server.boot.impl.publish.requirement.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.springframework.beans.factory.BeanFactory;

/**
 * @author arik
 */
public abstract class AbstractMethodRequirement extends AbstractBeanRequirement {

    private final Method targetMethod;

    protected AbstractMethodRequirement( BundlePublisher publisher, String beanName, Method targetMethod ) {
        super( publisher, beanName );
        this.targetMethod = targetMethod;
    }

    protected void invoke( BeanFactory beanFactory, Object... args )
            throws InvocationTargetException, IllegalAccessException {
        this.targetMethod.invoke( beanFactory.getBean( getBeanName() ), args );
    }
}
