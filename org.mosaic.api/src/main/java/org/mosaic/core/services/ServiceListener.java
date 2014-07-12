package org.mosaic.core.services;

/**
 * @author arik
 */
public interface ServiceListener<ServiceType> extends ServiceRegistrationListener<ServiceType>,
                                                      ServiceUnregistrationListener<ServiceType>
{
}
