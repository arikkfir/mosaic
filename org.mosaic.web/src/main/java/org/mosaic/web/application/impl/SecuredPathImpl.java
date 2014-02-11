package org.mosaic.web.application.impl;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.xpath.XPathException;
import org.mosaic.modules.Service;
import org.mosaic.util.expression.Expression;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.util.properties.PropertyPlaceholderResolver;
import org.mosaic.util.reflection.TypeTokens;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.web.application.Application;

/**
 * @author arik
 */
final class SecuredPathImpl implements Application.SecuredPath
{
    private static final Splitter AUTH_METHODS_SPLITTER = Splitter.on( ',' ).trimResults().omitEmptyStrings();

    private static final PropertyPlaceholderResolver PROPERTY_PLACEHOLDER_RESOLVER = new PropertyPlaceholderResolver();

    @Nonnull
    private final String path;

    @Nonnull
    private final Collection<String> authenticationMethods;

    @Nullable
    private final Expression<Boolean> expression;

    @Nullable
    private final String challangeMethod;

    @Nonnull
    @Service
    private ExpressionParser expressionParser;

    SecuredPathImpl( @Nonnull XmlElement constraintElt ) throws XPathException
    {
        this.path = PROPERTY_PLACEHOLDER_RESOLVER.resolve( constraintElt.getAttribute( "path" ).get() );
        this.authenticationMethods = AUTH_METHODS_SPLITTER.splitToList( constraintElt.getAttribute( "auth" ).get() );

        Optional<String> challange = constraintElt.getAttribute( "challange" );
        if( challange.isPresent() )
        {
            this.challangeMethod = PROPERTY_PLACEHOLDER_RESOLVER.resolve( challange.get() );
        }
        else
        {
            this.challangeMethod = null;
        }

        Optional<String> expression = constraintElt.getAttribute( "expression" );
        if( expression.isPresent() )
        {
            String resolved = PROPERTY_PLACEHOLDER_RESOLVER.resolve( expression.get() );
            this.expression = this.expressionParser.parseExpression( resolved, TypeTokens.BOOLEAN );
        }
        else
        {
            this.expression = null;
        }
    }

    @Nonnull
    @Override
    public String getPath()
    {
        return this.path;
    }

    @Nonnull
    @Override
    public Collection<String> getAuthenticationMethods()
    {
        return this.authenticationMethods;
    }

    @Nullable
    @Override
    public Expression<Boolean> getExpression()
    {
        return this.expression;
    }

    @Nullable
    @Override
    public String getChallangeMethod()
    {
        return this.challangeMethod;
    }
}
