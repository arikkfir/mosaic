package org.mosaic.util.xml;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import java.util.List;
import javax.annotation.Nonnull;
import javax.xml.xpath.XPathException;

/**
 * @author arik
 */
public interface XmlElement
{
    @Nonnull
    String getName();

    @Nonnull
    Optional<String> getValue();

    @Nonnull
    <T> Optional<T> getValue( @Nonnull TypeToken<T> type );

    @Nonnull
    Optional<String> getAttribute( @Nonnull String name );

    @Nonnull
    <T> Optional<T> getAttribute( @Nonnull String name, @Nonnull TypeToken<T> type );

    @Nonnull
    <T> Optional<T> find( @Nonnull String xpath, @Nonnull TypeToken<T> type ) throws XPathException;

    @Nonnull
    Optional<XmlElement> getFirstChildElement();

    @Nonnull
    Optional<XmlElement> getFirstChildElement( @Nonnull String name );

    @Nonnull
    List<XmlElement> getChildElements();

    @Nonnull
    List<XmlElement> getChildElements( @Nonnull String name );

    @Nonnull
    List<String> findTexts( @Nonnull String xpath ) throws XPathException;

    @Nonnull
    Optional<XmlElement> findFirstElement( @Nonnull String xpath ) throws XPathException;

    @Nonnull
    List<XmlElement> findElements( @Nonnull String xpath ) throws XPathException;
}
