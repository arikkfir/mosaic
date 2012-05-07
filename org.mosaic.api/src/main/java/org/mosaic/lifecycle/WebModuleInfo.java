package org.mosaic.lifecycle;

import java.net.URL;
import org.springframework.expression.Expression;

/**
 * @author arik
 */
public interface WebModuleInfo
{
    Expression getApplicationFilter();

    URL getContentUrl();
}
