package org.mosaic.core;

/**
 * @author arik
 */
public interface ServiceListener<ServiceType> extends ServiceManager.ServiceRegisteredAction<ServiceType>,
                                                      ServiceManager.ServiceUnregisteredAction<ServiceType>
{
}
