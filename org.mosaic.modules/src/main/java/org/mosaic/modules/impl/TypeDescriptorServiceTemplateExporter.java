package org.mosaic.modules.impl;

import java.lang.annotation.Annotation;
import java.util.Dictionary;
import java.util.Hashtable;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.modules.Ranking;
import org.mosaic.modules.ServiceTemplate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * @author arik
 */
@SuppressWarnings("unchecked")
final class TypeDescriptorServiceTemplateExporter extends Lifecycle implements ServiceTemplate
{
    @Nonnull
    private final TypeDescriptor typeDescriptor;

    @Nonnull
    private final Annotation type;

    @Nullable
    private ServiceRegistration<ServiceTemplate> registration;

    @Nullable
    private Integer ranking;

    TypeDescriptorServiceTemplateExporter( @Nonnull TypeDescriptor typeDescriptor, @Nonnull Annotation type )
    {
        this.typeDescriptor = typeDescriptor;
        this.type = type;

        Ranking rankingAnn = this.typeDescriptor.getType().getAnnotation( Ranking.class );
        if( rankingAnn != null )
        {
            this.ranking = rankingAnn.value();
        }
    }

    @Override
    public final String toString()
    {
        return "TypeDescriptorServiceTemplateExporter[@" + this.type.annotationType().getSimpleName() + "]";
    }

    @Nonnull
    @Override
    public Class<?> getTemplate()
    {
        return this.typeDescriptor.getType();
    }

    @Nonnull
    @Override
    public Annotation getType()
    {
        return this.type;
    }

    @Override
    protected synchronized void onAfterActivate()
    {
        BundleContext bundleContext = this.typeDescriptor.getModule().getBundle().getBundleContext();
        if( bundleContext == null )
        {
            throw new IllegalStateException( "no bundle context for module " + typeDescriptor.getModule() );
        }

        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put( "type", this.type.annotationType().getName() );
        if( this.ranking != null )
        {
            dict.put( Constants.SERVICE_RANKING, this.ranking );
        }
        this.registration = bundleContext.registerService( ServiceTemplate.class, this, dict );
    }

    @Override
    protected synchronized void onBeforeDeactivate()
    {
        if( this.registration != null )
        {
            try
            {
                this.registration.unregister();
            }
            catch( Throwable ignore )
            {
            }
            this.registration = null;
        }
    }
}
