package org.mosaic.util.xml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import org.xml.sax.SAXException;

/**
 * @author arik
 */
public interface XmlParser
{
    @Nonnull
    XmlDocument parse( @Nonnull Path file ) throws ParserConfigurationException, IOException, SAXException;

    @Nonnull
    XmlDocument parse( @Nonnull Path file, @Nonnull Schema schema )
            throws ParserConfigurationException, IOException, SAXException;

    @Nonnull
    XmlDocument parse( @Nonnull InputStream is,
                       @Nonnull String systemId ) throws ParserConfigurationException, IOException, SAXException;

    @Nonnull
    XmlDocument parse( @Nonnull InputStream is,
                       @Nonnull String systemId,
                       @Nonnull Schema schema ) throws ParserConfigurationException, IOException, SAXException;
}
