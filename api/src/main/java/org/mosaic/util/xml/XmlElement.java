package org.mosaic.util.xml;

import com.google.common.reflect.TypeToken;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.xpath.XPathException;

/**
 * @author arik
 */
public interface XmlElement
{
    @Nullable
    String getValue();

    @Nonnull
    String requireValue();

    @Nullable
    <T> T getValue( @Nonnull TypeToken<T> type );

    @Nullable
    String getAttribute( @Nonnull String name );

    @Nullable
    <T> T getAttribute( @Nonnull String name, @Nonnull TypeToken<T> type );

    @Nonnull
    String requireAttribute( @Nonnull String name );

    @Nonnull
    <T> T requireAttribute( @Nonnull String name, @Nonnull TypeToken<T> type );

    @Nonnull
    <T> T requireAttribute( @Nonnull String name, @Nonnull TypeToken<T> type, @Nonnull T defaultValue );

    @Nullable
    <T> T find( @Nonnull String xpath, @Nonnull TypeToken<T> type ) throws XPathException;

    @Nonnull
    <T> T find( @Nonnull String xpath, @Nonnull TypeToken<T> type, @Nonnull T defaultValue ) throws XPathException;

    @Nullable
    XmlElement getFirstChildElement();

    @Nullable
    XmlElement getFirstChildElement( @Nonnull String name );

    @Nonnull
    List<XmlElement> getChildElements();

    @Nonnull
    List<XmlElement> getChildElements( @Nonnull String name );

    @Nonnull
    List<String> findTexts( @Nonnull String xpath ) throws XPathException;

    @Nullable
    XmlElement findFirstElement( @Nonnull String xpath ) throws XPathException;

    @Nonnull
    List<XmlElement> findElements( @Nonnull String xpath ) throws XPathException;
}
