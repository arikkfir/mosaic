package org.mosaic.idea.module.facet;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import java.util.List;
import java.util.Map;
import org.jdom.Element;
import org.jetbrains.idea.maven.importing.FacetImporter;
import org.jetbrains.idea.maven.importing.MavenModifiableModelsProvider;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;


/**
 * @author arik
 */
public class ModuleFacetImporter
        extends FacetImporter<ModuleFacet, ModuleFacetConfiguration, ModuleFacetType>
{
    private final Logger logger = Logger.getInstance( getClass() );

    public static final String FELIX_GROUP_ID = "org.apache.felix";

    public static final String MAVEN_BUNDLE_PLUGIN_ARTIFACT_ID = "maven-bundle-plugin";

    public ModuleFacetImporter()
    {
        super( FELIX_GROUP_ID,
               MAVEN_BUNDLE_PLUGIN_ARTIFACT_ID,
               ModuleFacetType.getFacetType(),
               ModuleFacetType.getFacetType().getDefaultFacetName() );
    }

    @Override
    protected void setupFacet( ModuleFacet moduleFacet, MavenProject mavenProject )
    {
    }

    @Override
    protected void reimportFacet( MavenModifiableModelsProvider modelsProvider,
                                  Module module,
                                  MavenRootModelAdapter rootModel,
                                  final ModuleFacet facet,
                                  MavenProjectsTree mavenTree,
                                  MavenProject mavenProject,
                                  MavenProjectChanges changes,
                                  Map<MavenProject, String> mavenProjectToModuleName,
                                  List<MavenProjectsProcessorTask> postTasks )
    {
        if( facet != null )
        {
            Element pluginConfig = getConfig( mavenProject );
            if( pluginConfig != null && "bundle".equalsIgnoreCase( mavenProject.getPackaging() ) )
            {
                this.logger.info( "Mosaic module facet created/updated for module: " + module.getName() );
            }
            else
            {
                this.logger.info( "Removing Mosaic module facet for module: " + module.getName() );
                modelsProvider.getFacetModel( module ).removeFacet( facet );
            }
        }
    }
}
