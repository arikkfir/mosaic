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
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.apache.commons.digester3.ObjectCreateRule;
import org.apache.commons.digester3.Rule;
import org.mosaic.filewatch.FileWatcher;
import org.mosaic.filewatch.WatchEvent;
import org.mosaic.filewatch.WatchRoot;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.mosaic.security.policy.Permission;
import org.mosaic.security.policy.PermissionPoliciesManager;
import org.mosaic.security.policy.PermissionPolicy;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.util.xml.impl.Digester;
import org.mosaic.util.xml.impl.StrictErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
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
    public void onFileModified( @Nonnull Path file ) throws IOException
    {
        String fileName = file.getFileName().toString();
        String name = fileName.substring( 0, fileName.lastIndexOf( '.' ) );

        Digester digester = new Digester( getClass(), PERMISSION_POLICY_SCHEMA );
        digester.setRuleNamespaceURI( "https://github.com/arikkfir/mosaic/permission-policy-1.0.0" );

        // add policy rules
        digester.addObjectCreate( "permission-policy", PermissionPolicyImpl.class );

        // add create role rules
        digester.addRule( "permission-policy/roles/*/role", createNewRoleRule() );
        digester.addCallParam( "permission-policy/roles/*/role", 0, "name" );
        digester.addSetNext( "permission-policy/roles/*/role", "addRole" );

        // add create permission rules
        digester.addRule( "permission-policy/roles/*/role/permission", new AddPermissionRule() );

        // add create grant rules
        digester.addRule( "permission-policy/rules/test-if", new AddGrantRule() );

        // parse and extract policy from digester stack
        try
        {
            PermissionPolicyImpl policy = digester.parse( file.toFile() );
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

    private ObjectCreateRule createNewRoleRule()
    {
        ObjectCreateRule createRoleRule = new ObjectCreateRule( RoleImpl.class );
        createRoleRule.setConstructorArgumentTypes( String.class );
        return createRoleRule;
    }

    private class AddPermissionRule extends Rule
    {
        @Override
        public void begin( @Nonnull String namespace, @Nonnull String name, @Nonnull Attributes attributes )
                throws Exception
        {
            String permissionString = attributes.getValue( "name" );

            RoleImpl role = getDigester().peek();
            role.addPermission( parsePermission( permissionString ) );
        }
    }

    private class AddGrantRule extends Rule
    {
        @Override
        public void begin( @Nonnull String namespace, @Nonnull String name, @Nonnull Attributes attributes )
                throws Exception
        {
            String test = attributes.getValue( "user" );
            String grant = attributes.getValue( "then-grant" );

            PermissionPolicyImpl policy = getDigester().peek();
            policy.addGrant( new GrantRule( expressionParser.parseExpression( test ), parsePermission( grant ) ) );
        }
    }
}
