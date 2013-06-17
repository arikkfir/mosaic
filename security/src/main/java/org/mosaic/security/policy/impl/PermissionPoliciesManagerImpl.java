package org.mosaic.security.policy.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.mosaic.filewatch.WatchEvent;
import org.mosaic.filewatch.WatchRoot;
import org.mosaic.filewatch.annotation.FileWatcher;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.mosaic.security.policy.Permission;
import org.mosaic.security.policy.PermissionPoliciesManager;
import org.mosaic.security.policy.PermissionPolicy;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.util.xml.StrictErrorHandler;
import org.mosaic.util.xml.XmlDocument;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.util.xml.XmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author arik
 */
@Service(PermissionPoliciesManager.class)
public class PermissionPoliciesManagerImpl implements PermissionPoliciesManager
{
    private static final Logger LOG = LoggerFactory.getLogger( PermissionPoliciesManagerImpl.class );

    private static final Schema PERMISSION_POLICY_SCHEMA;

    static
    {
        SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
        schemaFactory.setErrorHandler( StrictErrorHandler.INSTANCE );
        try
        {
            PERMISSION_POLICY_SCHEMA = schemaFactory.newSchema( PermissionPoliciesManager.class.getResource( "permission-policy.xsd" ) );
        }
        catch( SAXException e )
        {
            throw new IllegalStateException( "Could not find 'users.xsd' schema resource in Mosaic API bundle: " + e.getMessage(), e );
        }
    }

    @Nonnull
    private final Map<String, PermissionPolicy> permissionPolicies = new ConcurrentHashMap<>();

    @Nonnull
    private final LoadingCache<String, Permission> permissionsCache;

    @Nonnull
    private ExpressionParser expressionParser;

    @Nonnull
    private XmlParser xmlParser;

    public PermissionPoliciesManagerImpl()
    {
        this.permissionsCache = CacheBuilder.newBuilder()
                                            .concurrencyLevel( 100 )
                                            .expireAfterAccess( 1, TimeUnit.HOURS )
                                            .initialCapacity( 100 )
                                            .maximumSize( 5000 )
                                            .build( new CacheLoader<String, Permission>()
                                            {
                                                @Override
                                                public Permission load( String key ) throws Exception
                                                {
                                                    return new PermissionImpl( PermissionPoliciesManagerImpl.this, key );
                                                }
                                            } );
        this.permissionsCache.cleanUp();
    }

    @ServiceRef
    public void setExpressionParser( @Nonnull ExpressionParser expressionParser )
    {
        this.expressionParser = expressionParser;
    }

    @ServiceRef
    public void setXmlParser( @Nonnull XmlParser xmlParser )
    {
        this.xmlParser = xmlParser;
    }

    @Nonnull
    @Override
    public Permission parsePermission( @Nonnull final String permission )
    {
        return this.permissionsCache.getUnchecked( permission );
    }

    @Nullable
    @Override
    public PermissionPolicy getPolicy( @Nonnull String name )
    {
        return this.permissionPolicies.get( name );
    }

    @FileWatcher(root = WatchRoot.ETC,
                 pattern = "permission-policies/**/*.xml",
                 event = {
                         WatchEvent.FILE_ADDED,
                         WatchEvent.FILE_MODIFIED
                 })
    public void onFileModified( @Nonnull Path file ) throws IOException, ParserConfigurationException, SAXException
    {
        String fileName = file.getFileName().toString();
        String name = fileName.substring( 0, fileName.lastIndexOf( '.' ) );

        XmlDocument doc = this.xmlParser.parse( file, PERMISSION_POLICY_SCHEMA );

        PermissionPolicyImpl policy = new PermissionPolicyImpl();

        XmlElement rolesElement = doc.getRoot().getFirstChildElement( "roles" );
        if( rolesElement != null )
        {
            for( XmlElement roleElement : rolesElement.getChildElements( "role" ) )
            {
                policy.addRole( new RoleImpl( this, roleElement ) );
            }
        }

        XmlElement rulesElement = doc.getRoot().getFirstChildElement( "rules" );
        if( rulesElement != null )
        {
            for( XmlElement grantElement : rulesElement.getChildElements( "test-if" ) )
            {
                String test = grantElement.requireAttribute( "user" );
                String grant = grantElement.requireAttribute( "then-grant" );
                policy.addGrant( new GrantRule( this.expressionParser.parseExpression( test ), parsePermission( grant ) ) );
            }
        }

        // parse and extract policy from digester stack
        try
        {
            this.permissionPolicies.put( name, policy );
        }
        catch( Exception e )
        {
            LOG.error( "Error parsing permission policy at '{}': {}", file, e.getMessage(), e );
            this.permissionPolicies.remove( name );
        }
    }

    @FileWatcher(root = WatchRoot.ETC,
                 pattern = "permission-policies/**/*.xml",
                 event = WatchEvent.FILE_DELETED)
    public void onFileDeleted( @Nonnull Path file ) throws IOException
    {
        String fileName = file.getFileName().toString();
        String name = fileName.substring( 0, fileName.lastIndexOf( '.' ) );
        this.permissionPolicies.remove( name );
    }
}
