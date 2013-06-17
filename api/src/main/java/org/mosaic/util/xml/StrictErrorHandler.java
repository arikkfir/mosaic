package org.mosaic.util.xml;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author arik
 */
public class StrictErrorHandler implements ErrorHandler
{
    public static final StrictErrorHandler INSTANCE = new StrictErrorHandler();

    @Override
    public void warning( SAXParseException exception ) throws SAXException
    {
        throw exception;
    }

    @Override
    public void error( SAXParseException exception ) throws SAXException
    {
        throw exception;
    }

    @Override
    public void fatalError( SAXParseException exception ) throws SAXException
    {
        throw exception;
    }
}
