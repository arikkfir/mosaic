package org.mosaic.development.idea.make.impl;

import com.intellij.execution.BeforeRunTask;

/**
 * @author arik
 */
public class RebuildBundlesBeforeRunTask extends BeforeRunTask<RebuildBundlesBeforeRunTask>
{
    public RebuildBundlesBeforeRunTask()
    {
        super( RebuildBundlesBeforeRunTaskProvider.ID );
        setEnabled( true );
    }
}
