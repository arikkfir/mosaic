package org.mosaic.server.boot.impl.publish.requirement;

import org.springframework.context.ApplicationContext;

/**
 * A dynamic bundle requirement on an external capability. Usually requirements are made on external services, bundles,
 * or some other capability which dictates that only while that capability is provided the bundle can be published.
 *
 * @author arik
 */
public interface Requirement {

    /**
     * Open the requirement, making it "live". Usually this will result in the requirement starting to track a service
     * or other dependencies.
     *
     * @throws Exception in case the requirement cannot be opened. The bundle will not be tracked for requirements.
     */
    boolean open() throws Exception;

    /**
     * Applies requirement usage on the given application context. Usually this means injecting a service to some bean,
     * or registering a service in the OSGi service engine, etc.
     * <p/>
     * This method is called whenever a requirement notifies the bundle publisher that it is satisfied, and the bundle
     * is already published. In such cases, only that requirement's {@code onSatisfy} method is called.
     * <p/>
     * If, however, the bundle is not published when the requirement notifies that it is satisfied, an attempt is made
     * to publish the bundle, if all other requirements were already satisfied; if so, the publisher will call each
     * requirement's {@link #onPublish(org.springframework.context.ApplicationContext)} method (including the
     * requirement which triggered the publish).
     *
     * @param applicationContext the application context of the bundle
     * @param state              the state that was passed to {@link org.mosaic.server.boot.impl.publish.BundlePublisher#markAsSatisfied(Requirement, Object[])}
     * @throws Exception in case the requirement could not be applied
     */
    void onSatisfy( ApplicationContext applicationContext, Object... state ) throws Exception;

    /**
     * Called when bundle is being published and allows the requirement to initialize specific beans.
     *
     * @param bean     the bean being initialized
     * @param beanName the name of the bean
     */
    void onInitBean( Object bean, String beanName ) throws Exception;

    /**
     * Applies requirement usage on the given application context. Usually this means injecting a service to some bean,
     * or registering a service in the OSGi service engine, etc.
     * <p/>
     * Please see {@link #onSatisfy} method for details.
     *
     * @param applicationContext the application context of the bundle
     * @throws Exception in case the requirement could not be applied
     */
    void onPublish( ApplicationContext applicationContext ) throws Exception;

    /**
     * Reverts any changes made on the last {@link #onSatisfy}
     * call. There is no need to revert injections made to beans in the application context, as it is discarded along
     * with its beans. But if the requirement registered a service in the OSGi service engine, it needs to be
     * unregistered, or if some files were created or any resource allocated, they should be discarded and freed.
     *
     * @throws Exception in case the revert fails
     */
    void revert() throws Exception;

    /**
     * Closes the requirement and releases any used resources. Implementations should not throw any exceptions to allow
     * other requirements to close as well (they should be logged though).
     */
    void close();

}
