package org.mosaic.web.client;

import com.google.common.collect.ListMultimap;
import org.mosaic.web.net.HttpStatus;

/**
 * @author arik
 */
public interface Response<Type>
{
    HttpStatus getStatusCode();

    String getStatusText();

    Type getResult();

    ListMultimap<String, String> getHeaders();
}
