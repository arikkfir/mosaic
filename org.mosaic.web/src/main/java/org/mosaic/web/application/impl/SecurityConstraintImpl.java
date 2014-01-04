package org.mosaic.web.application.impl;

import com.google.common.base.Splitter;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.xpath.XPathException;
import org.mosaic.modules.Service;
import org.mosaic.util.expression.Expression;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.web.security.SecurityConstraint;

import static org.mosaic.web.application.impl.ApplicationImpl.PROPERTY_PLACEHOLDER_RESOLVER;

/**
 * @author arik
 */
final class SecurityConstraintImpl implements SecurityConstraint
{
    private static final Splitter AUTH_METHODS_SPLITTER = Splitter.on( ',' ).trimResults().omitEmptyStrings();

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

    SecurityConstraintImpl( @Nonnull XmlElement constraintElt ) throws XPathException
    {
        this.path = PROPERTY_PLACEHOLDER_RESOLVER.resolve( constraintElt.requireAttribute( "path" ) );
        this.authenticationMethods = AUTH_METHODS_SPLITTER.splitToList( constraintElt.requireAttribute( "auth" ) );
        this.challangeMethod = PROPERTY_PLACEHOLDER_RESOLVER.resolve( constraintElt.requireAttribute( "challange" ) );

        String expression = constraintElt.getAttribute( "expression" );
        if( expression != null )
        {
            expression = PROPERTY_PLACEHOLDER_RESOLVER.resolve( expression );
            this.expression = this.expressionParser.parseExpression( expression, Boolean.class );
        }
        else
        {
            this.expression = null;
        }
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

    @Nonnull
    String getPath()
    {
        return this.path;
    }
}
