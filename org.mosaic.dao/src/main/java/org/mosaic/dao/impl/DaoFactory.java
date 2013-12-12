package org.mosaic.dao.impl;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import org.mosaic.dao.Dao;
import org.mosaic.modules.*;

/**
 * @author arik
 */
@SuppressWarnings( "unchecked" )
@Component
final class DaoFactory
{
    @SuppressWarnings( "MismatchedQueryAndUpdateOfCollection" )
    @Nonnull
    private final Map<Long, ServiceRegistration> registrations = new ConcurrentHashMap<>();

    @Nonnull
    @Component
    private Module module;

    @OnServiceAdded
    void onDaoTemplateAdded( @Nonnull ServiceReference<ServiceTemplate<Dao>> daoTemplateReference )
    {
        ServiceTemplate<Dao> serviceTemplate = daoTemplateReference.get();
        if( serviceTemplate != null )
        {
            Class daoType = serviceTemplate.getTemplate();

            Object dao = Proxy.newProxyInstance( daoType.getClassLoader(),
                                                 new Class[] { daoType },
                                                 new DaoProxy( daoType, serviceTemplate.getType().value() ) );
            ServiceRegistration registration = this.module.getModuleWiring().register( daoType, dao );
            this.registrations.put( daoTemplateReference.getId(), registration );
        }
    }

    @OnServiceRemoved
    void onDaoTemplateRemoved( @Nonnull ServiceReference<ServiceTemplate<Dao>> daoTemplateReference )
    {
        this.registrations.remove( daoTemplateReference.getId() );
    }
}
