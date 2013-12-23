package org.mosaic.web.application.impl;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.xml.xpath.XPathException;
import org.mosaic.modules.Service;
import org.mosaic.util.expression.Expression;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.web.application.Application;

import static org.mosaic.web.application.impl.ApplicationImpl.PROPERTY_PLACEHOLDER_RESOLVER;

/**
 * @author arik
 */
final class SecurityConstraintImpl implements Application.ApplicationSecurity.SecurityConstraint
{
    @Nonnull
    private final Set<String> paths;

    @Nonnull
    private final String authenticationMethod;

    @Nonnull
    private final Expression<Boolean> expression;

    @Nonnull
    @Service
    private ExpressionParser expressionParser;

    SecurityConstraintImpl( @Nonnull XmlElement constraintElt ) throws XPathException
    {
        Set<String> paths = new LinkedHashSet<>();
        for( String path : constraintElt.findTexts( "m:path" ) )
        {
            paths.add( PROPERTY_PLACEHOLDER_RESOLVER.resolve( path ) );
        }
        this.paths = Collections.unmodifiableSet( paths );

        this.authenticationMethod = constraintElt.requireAttribute( "method" );

        String expression = PROPERTY_PLACEHOLDER_RESOLVER.resolve( constraintElt.requireAttribute( "expression" ) );
        this.expression = this.expressionParser.parseExpression( expression, Boolean.class );
    }

    @Nonnull
    @Override
    public String getAuthenticationMethod()
    {
        return this.authenticationMethod;
    }

    @Nonnull
    @Override
    public Set<String> getPaths()
    {
        return this.paths;
    }

    @Nonnull
    @Override
    public Expression<Boolean> getExpression()
    {
        return this.expression;
    }
}
