package org.mosaic.server.lifecycle;

import java.io.File;
import org.springframework.expression.Expression;

/**
 * @author arik
 */
public interface WebModuleInfo
{
    Expression getApplicationFilter();

    File getContentRoot();
}
