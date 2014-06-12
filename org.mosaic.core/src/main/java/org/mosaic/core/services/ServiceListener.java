package org.mosaic.core.services;

/**
 * @author arik
 */
public interface ServiceListener<ServiceType> extends ServiceManager.ServiceRegisteredAction<ServiceType>,
                                                      ServiceManager.ServiceUnregisteredAction<ServiceType>
{
}
