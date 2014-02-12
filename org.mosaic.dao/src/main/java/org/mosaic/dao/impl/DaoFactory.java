package org.mosaic.dao.impl;

import com.google.common.base.Optional;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import org.mosaic.dao.Dao;
import org.mosaic.modules.*;

/**
 * @author arik
 */
@Component
final class DaoFactory
{
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @Nonnull
    private final Map<Long, ServiceRegistration> registrations = new ConcurrentHashMap<>();

    @Nonnull
    @Component
    private Module module;

    @SuppressWarnings( "unchecked" )
    @OnServiceAdded
    void onDaoTemplateAdded( @Nonnull ServiceReference<ServiceTemplate<Dao>> daoTemplateReference )
    {
        Optional<ServiceTemplate<Dao>> tmplHolder = daoTemplateReference.service();
        if( tmplHolder.isPresent() )
        {
            Class<Object> daoType = ( Class<Object> ) tmplHolder.get().getTemplate();
            Object dao = Proxy.newProxyInstance( daoType.getClassLoader(),
                                                 new Class[] { daoType },
                                                 new DaoProxy( daoType, tmplHolder.get().getType().value() ) );
            ServiceRegistration registration = this.module.register( daoType, dao );
            this.registrations.put( daoTemplateReference.getId(), registration );
        }
    }

    @OnServiceRemoved
    void onDaoTemplateRemoved( @Nonnull ServiceReference<ServiceTemplate<Dao>> daoTemplateReference )
    {
        this.registrations.remove( daoTemplateReference.getId() );
    }
}
