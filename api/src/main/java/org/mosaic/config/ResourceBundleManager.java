package org.mosaic.config;

import java.util.Locale;
import java.util.ResourceBundle;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface ResourceBundleManager
{
    @Nonnull
    ResourceBundle getResourceBundle( @Nonnull String name, @Nonnull Locale locale );
}
