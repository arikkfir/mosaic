package org.mosaic.idea.module.facet;

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;

/**
 * @author arik
 */
@State(
        name = "org.mosaic.idea.module.facet.ModuleFacetConfiguration",
        storages = { @Storage(file = "$APP_CONFIG$/mosaicModule.xml") }
)
public class ModuleFacetConfiguration
        implements FacetConfiguration, PersistentStateComponent<ModuleFacetConfiguration.State>
{
    public static class State
    {
        // currently we have no state...
    }

    @Override
    public FacetEditorTab[] createEditorTabs( FacetEditorContext editorContext,
                                              FacetValidatorsManager validatorsManager )
    {
        return new FacetEditorTab[ 0 ];
    }

    @Override
    public State getState()
    {
        return new State();
    }

    @Override
    public void loadState( State state )
    {
    }

    @Override
    public void readExternal( Element element ) throws InvalidDataException
    {
        // no-op
    }

    @Override
    public void writeExternal( Element element ) throws WriteExternalException
    {
        // no-op
    }
}
