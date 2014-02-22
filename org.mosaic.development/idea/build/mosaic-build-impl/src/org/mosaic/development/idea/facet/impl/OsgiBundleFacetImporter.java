package org.mosaic.development.idea.facet.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import java.util.List;
import java.util.Map;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.importing.FacetImporter;
import org.jetbrains.idea.maven.importing.MavenModifiableModelsProvider;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;
import org.mosaic.development.idea.facet.OsgiBundleFacet;
import org.mosaic.development.idea.facet.OsgiBundleFacetConfiguration;
import org.mosaic.development.idea.facet.OsgiBundleFacetType;


/**
 * @author arik
 */
public class OsgiBundleFacetImporter
        extends FacetImporter<OsgiBundleFacet, OsgiBundleFacetConfiguration, OsgiBundleFacetType>
{
    public static final String FELIX_GROUP_ID = "org.apache.felix";

    public static final String MAVEN_BUNDLE_PLUGIN_ARTIFACT_ID = "maven-bundle-plugin";

    private final Logger logger = Logger.getInstance( getClass() );

    public OsgiBundleFacetImporter()
    {
        super( FELIX_GROUP_ID,
               MAVEN_BUNDLE_PLUGIN_ARTIFACT_ID,
               OsgiBundleFacetType.getOsgiBundleFacetType(),
               OsgiBundleFacetType.getOsgiBundleFacetType().getDefaultFacetName() );
    }

    @Override
    protected void setupFacet( @NotNull OsgiBundleFacet osgiBundleFacet, @NotNull MavenProject mavenProject )
    {
    }

    @Override
    protected void reimportFacet( MavenModifiableModelsProvider modelsProvider,
                                  Module module,
                                  MavenRootModelAdapter rootModel,
                                  final OsgiBundleFacet facet,
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
                // no-op
            }
            else
            {
                modelsProvider.getFacetModel( module ).removeFacet( facet );
            }
        }
    }
}
