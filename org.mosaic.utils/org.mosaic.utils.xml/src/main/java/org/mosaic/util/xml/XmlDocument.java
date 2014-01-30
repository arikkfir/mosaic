package org.mosaic.util.xml;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface XmlDocument
{
    void addNamespace( @Nonnull String prefix, @Nonnull String uri );

    @Nonnull
    XmlElement getRoot();
}
