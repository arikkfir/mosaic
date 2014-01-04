package org.mosaic.web.security;

import java.util.Collection;
import javax.annotation.Nullable;
import org.mosaic.util.expression.Expression;

/**
 * @author arik
 */
public interface SecurityConstraint
{
    @Nullable
    Collection<String> getAuthenticationMethods();

    @Nullable
    Expression<Boolean> getExpression();

    @Nullable
    String getChallangeMethod();
}
