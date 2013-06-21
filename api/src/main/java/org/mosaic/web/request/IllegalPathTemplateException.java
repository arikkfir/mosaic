package org.mosaic.web.request;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class IllegalPathTemplateException extends RuntimeException
{
    @Nonnull
    private final String template;

    public IllegalPathTemplateException( String message, @Nonnull String template )
    {
        super( "Illegal path template '" + template + "': " + message );
        this.template = template;
    }

    @Nonnull
    public String getTemplate()
    {
        return this.template;
    }
}
