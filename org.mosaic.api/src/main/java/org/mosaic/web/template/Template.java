package org.mosaic.web.template;

/**
 * @author arik
 */
public class Template
{
    private final String type;

    private final String name;

    public Template( String name )
    {
        this( null, name );
    }

    public Template( String type, String name )
    {
        this.type = type;
        this.name = name;
    }

    public String getType()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return "Template[type=" + this.type + ",name=" + this.name + "]";
    }
}
