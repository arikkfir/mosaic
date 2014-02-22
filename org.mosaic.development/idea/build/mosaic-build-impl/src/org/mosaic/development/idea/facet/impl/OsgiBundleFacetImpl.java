package org.mosaic.development.idea.facet.impl;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.openapi.module.Module;
import java.io.File;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.mosaic.development.idea.facet.OsgiBundleFacet;
import org.mosaic.development.idea.facet.OsgiBundleFacetConfiguration;

import static org.mosaic.development.idea.util.Util.getBundleFileName;

/**
 * @author arik
 */
public class OsgiBundleFacetImpl extends OsgiBundleFacet
{
    public OsgiBundleFacetImpl( @NotNull FacetType facetType,
                                @NotNull Module module,
                                @NotNull String name,
                                @NotNull OsgiBundleFacetConfiguration configuration,
                                Facet underlyingFacet )
    {
        super( facetType, module, name, configuration, underlyingFacet );
    }

    @Override
    @Nullable
    public String getBundlePath()
    {
        MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance( getModule().getProject() );
        if( mavenProjectsManager == null )
        {
            return null;
        }

        MavenProject mavenProject = mavenProjectsManager.findProject( getModule() );
        if( mavenProject == null )
        {
            return null;
        }

        File buildDirectory = new File( mavenProject.getBuildDirectory() );
        File bundleFile = new File( buildDirectory, getBundleFileName( mavenProject ) );
        return bundleFile.getPath();
    }
}
