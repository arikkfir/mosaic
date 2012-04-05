package org.mosaic.server.boot.impl.publish.requirement.support;

import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.springframework.beans.factory.BeanFactory;

/**
 * @author arik
 */
public abstract class AbstractBeanRequirement extends AbstractRequirement {

    private final String beanName;

    protected AbstractBeanRequirement( BundlePublisher publisher, String beanName ) {
        super( publisher );
        this.beanName = beanName;
    }

    protected String getBeanName() {
        return beanName;
    }

    protected Object getBean( BeanFactory beanFactory ) {
        return beanFactory.getBean( this.beanName );
    }
}
