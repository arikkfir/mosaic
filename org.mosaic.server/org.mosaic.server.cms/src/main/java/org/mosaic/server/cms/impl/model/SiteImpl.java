package org.mosaic.server.cms.impl.model;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.mosaic.cms.Blueprint;
import org.mosaic.cms.Page;
import org.mosaic.cms.Site;
import org.mosaic.server.cms.impl.DataProviderRegistry;
import org.mosaic.server.lifecycle.WebModuleInfo;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.mosaic.web.HttpApplication;

/**
 * @author arik
 */
public class SiteImpl extends BaseModel implements Site
{
    private static final Logger LOG = LoggerFactory.getLogger( SiteImpl.class );

    private final Map<String, File> blueprintFiles = new HashMap<>();

    private final Map<String, File> pageFiles = new HashMap<>();

    private final Map<String, File> snippetFiles = new HashMap<>();

    private final HttpApplication application;

    private final DataProviderRegistry dataProviderRegistry;

    private final Map<String, Exception> errors = new LinkedHashMap<>();

    private Map<String, BlueprintImpl> blueprints = new HashMap<>();

    private Map<String, PageImpl> pages = new HashMap<>();

    public SiteImpl( HttpApplication application,
                     DataProviderRegistry dataProviderRegistry,
                     List<WebModuleInfo> modules )
    {
        this.application = application;
        this.dataProviderRegistry = dataProviderRegistry;
        setName( this.application.getName() );
        setDisplayName( this.application.getDisplayName() );

        for( WebModuleInfo module : modules )
        {
            File contentRoot = module.getContentRoot();
            if( contentRoot.exists() )
            {
                File blueprintsDir = new File( contentRoot, "blueprints" );
                if( blueprintsDir.exists() && blueprintsDir.isDirectory() )
                {
                    this.blueprintFiles.putAll( new XmlFileCollector().findFiles( blueprintsDir ) );
                }

                File pagesDir = new File( contentRoot, "pages" );
                if( pagesDir.exists() && pagesDir.isDirectory() )
                {
                    this.pageFiles.putAll( new XmlFileCollector().findFiles( pagesDir ) );
                }

                File snippetsDir = new File( contentRoot, "snippets" );
                if( snippetsDir.exists() && snippetsDir.isDirectory() )
                {
                    this.snippetFiles.putAll( new XmlFileCollector().findFiles( snippetsDir ) );
                }
            }
        }
    }

    public HttpApplication getApplication()
    {
        return application;
    }

    public DataProviderRegistry getDataProviderRegistry()
    {
        return dataProviderRegistry;
    }

    public Map<String, Exception> getErrors()
    {
        return errors;
    }

    void addError( String message, Exception exception )
    {
        //noinspection ThrowableResultOfMethodCallIgnored
        this.errors.put( ( this.errors.size() + 1 ) + message, exception );
    }

    @Override
    public BlueprintImpl getBlueprint( String name )
    {
        if( !this.blueprints.containsKey( name ) )
        {
            synchronized( this )
            {
                if( !this.blueprints.containsKey( name ) )
                {
                    BlueprintImpl blueprint = null;
                    File blueprintFile = this.blueprintFiles.get( name );
                    if( blueprintFile != null )
                    {
                        blueprint = new BlueprintImpl( this, blueprintFile );
                    }

                    Map<String, BlueprintImpl> newBlueprints = new HashMap<>( this.blueprints );
                    newBlueprints.put( name, blueprint );
                    this.blueprints = newBlueprints;
                }
            }
        }
        return this.blueprints.get( name );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public Collection<Blueprint> getBlueprints()
    {
        return ( Collection ) this.blueprints.values();
    }

    @Override
    public Page getPage( String name )
    {
        if( !this.pages.containsKey( name ) )
        {
            synchronized( this )
            {
                if( !this.pages.containsKey( name ) )
                {
                    PageImpl page = null;
                    File pageFile = this.pageFiles.get( name );
                    if( pageFile != null )
                    {
                        page = new PageImpl( this, pageFile );
                    }

                    Map<String, PageImpl> newPages = new HashMap<>( this.pages );
                    newPages.put( name, page );
                    this.pages = newPages;
                }
            }
        }
        return this.pages.get( name );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public Collection<Page> getPages()
    {
        return ( Collection ) this.pages.values();
    }

    private static class XmlFileCollector extends DirectoryWalker<File>
    {
        private Map<String, File> files;

        private XmlFileCollector()
        {
            super( null, new WildcardFileFilter( "*.xml", IOCase.INSENSITIVE ), -1 );
        }

        public Map<String, File> findFiles( File directory )
        {
            this.files = new HashMap<>( 100 );
            try
            {
                walk( directory, null );
            }
            catch( IOException e )
            {
                LOG.warn( "Could not traverse directory '{}': {}", directory, e.getMessage(), e );
            }
            Map<String, File> files = this.files;
            this.files = null;
            return files;
        }

        @Override
        protected void handleFile( File file, int depth, Collection<File> results ) throws IOException
        {
            this.files.put( FilenameUtils.getBaseName( file.getName() ).toLowerCase(), file );
        }
    }
}
