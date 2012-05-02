package org.mosaic.server.boot.impl.publish.requirement.support;

import org.mosaic.server.boot.impl.publish.BundleTracker;
import org.springframework.beans.factory.BeanFactory;

/**
 * @author arik
 */
public abstract class AbstractBeanRequirement extends AbstractRequirement
{

    private final String beanName;

    protected AbstractBeanRequirement( BundleTracker tracker, String beanName )
    {
        super( tracker );
        this.beanName = beanName;
    }

    protected Object getBean( BeanFactory beanFactory )
    {
        return beanFactory.getBean( this.beanName );
    }

    protected String getBeanName()
    {
        return beanName;
    }

    @Override
    public boolean isBeanPublishable( Object bean, String beanName )
    {
        return this.beanName.equals( beanName );
    }

    @Override
    protected final void publishBeanInternal( Object bean, String beanName ) throws Exception
    {
        onInitBeanInternal( bean );
    }

    protected void onInitBeanInternal( Object bean ) throws Exception
    {
        // no-op
    }
}
