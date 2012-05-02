package org.mosaic.server.boot.impl.publish.requirement;

import org.springframework.context.ApplicationContext;

public interface Requirement
{

    int BUNDLE_CONTEXT_PRIORITY = 10;

    int SERVICE_REF_PRIORITY = 20;

    int SERVICE_LIST_PRIORITY = 30;

    int SERVICE_BIND_PRIORITY = 40;

    int SERVICE_UNBIND_PRIORITY = 50;

    int SERVICE_EXPORT_PRIORITY = 60;

    int getPriority();

    String toShortString();

    boolean track() throws Exception;

    void untrack();

    void publish( ApplicationContext applicationContext ) throws Exception;

    void unpublish();

    boolean isBeanPublishable( Object bean, String beanName );

    void publishBean( Object bean, String beanName ) throws Exception;

    void onSatisfy( ApplicationContext applicationContext, Object... state );
}
