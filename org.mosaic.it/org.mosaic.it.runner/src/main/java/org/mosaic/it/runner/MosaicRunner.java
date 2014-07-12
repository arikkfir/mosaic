package org.mosaic.it.runner;

import java.util.List;
import org.junit.rules.MethodRule;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * @author arik
 */
public class MosaicRunner extends BlockJUnit4ClassRunner
{
    public MosaicRunner( Class<?> klass ) throws InitializationError
    {
        super( klass );
    }

    @Override
    protected List<MethodRule> rules( Object target )
    {
        List<MethodRule> rules = super.rules( target );
        rules.add( new MosaicServerRule() );
        return rules;
    }
}
