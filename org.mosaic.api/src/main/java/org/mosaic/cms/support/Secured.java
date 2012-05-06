package org.mosaic.cms.support;

import org.springframework.expression.Expression;

/**
 * @author arik
 */
public interface Secured
{
    Expression getSecurityExpression();
}
