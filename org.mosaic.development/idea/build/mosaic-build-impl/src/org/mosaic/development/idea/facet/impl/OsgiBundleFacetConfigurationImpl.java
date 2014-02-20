package org.mosaic.development.idea.facet.impl;

import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.mosaic.development.idea.facet.OsgiBundleFacetConfiguration;

/**
 * @author arik
 */
@State(
        name = "org.mosaic.development.idea.facet.OsgiBundleFacet",
        storages = { @Storage( file = "$APP_CONFIG$/osgiBundles.xml" ) }
)
public class OsgiBundleFacetConfigurationImpl extends OsgiBundleFacetConfiguration
        implements PersistentStateComponent<OsgiBundleFacetConfigurationImpl.State>
{
    public static class State
    {
        // currently we have no state...
    }

    @Override
    public FacetEditorTab[] createEditorTabs( @NotNull FacetEditorContext editorContext,
                                              @NotNull FacetValidatorsManager validatorsManager )
    {
        return new FacetEditorTab[ 0 ];
    }

    @NotNull
    @Override
    public State getState()
    {
        return new State();
    }

    @Override
    public void loadState( @NotNull State state )
    {
    }

    @Override
    public void readExternal( @NotNull Element element ) throws InvalidDataException
    {
        // no-op
    }

    @Override
    public void writeExternal( @NotNull Element element ) throws WriteExternalException
    {
        // no-op
    }
}
