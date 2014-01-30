package org.mosaic.security.impl;

import com.google.common.base.Optional;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.mosaic.modules.Component;
import org.mosaic.modules.Service;
import org.mosaic.pathwatchers.OnPathCreated;
import org.mosaic.pathwatchers.OnPathDeleted;
import org.mosaic.pathwatchers.OnPathModified;
import org.mosaic.security.AuthorizationException;
import org.mosaic.security.Permission;
import org.mosaic.security.Subject;
import org.mosaic.server.Server;
import org.mosaic.util.expression.Expression;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.util.reflection.TypeTokens;
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
@Component
final class PermissionPolicyManager
{
    private static final Logger LOG = LoggerFactory.getLogger( PermissionPolicyManager.class );

    private static final String PRM_POLS_PATH = "${mosaic.home.etc}/security/permission-policies/**/*.xml";

    @Nonnull
    private final Schema permissionPolicySchema;

    @Nonnull
    private final Map<String, PermissionPolicyImpl> permissionPolicies = new ConcurrentHashMap<>();

    @Nonnull
    @Service
    private Server server;

    @Nonnull
    @Service
    private ExpressionParser expressionParser;

    @Nonnull
    @Service
    private XmlParser xmlParser;

    PermissionPolicyManager() throws IOException, SAXException
    {
        Path schemaFile = this.server.getHome().resolve( "schemas/permission-policy-1.0.0.xsd" );
        if( Files.notExists( schemaFile ) )
        {
            throw new IllegalStateException( "could not find permission policy schema at '" + schemaFile + "'" );
        }

        SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
        schemaFactory.setErrorHandler( StrictErrorHandler.INSTANCE );
        try( InputStream stream100 = Files.newInputStream( schemaFile ) )
        {
            this.permissionPolicySchema = schemaFactory.newSchema( new Source[] {
                    new StreamSource( stream100, "http://www.mosaicserver.com/permission-policy-1.0.0" )
            } );
        }
    }

    boolean isPermitted( @Nonnull String permissionPolicyName,
                         @Nonnull Subject subject,
                         @Nonnull Permission permission )
    {
        PermissionPolicyImpl permissionPolicy = this.permissionPolicies.get( permissionPolicyName );
        if( permissionPolicy == null )
        {
            throw new AuthorizationException( "unknown permission policy: " + permissionPolicyName, subject, permission );
        }
        else
        {
            return permissionPolicy.isPermitted( subject, permission );
        }
    }

    @OnPathCreated( PRM_POLS_PATH )
    @OnPathModified( PRM_POLS_PATH )
    void addPermissionPolicy( @Nonnull Path file ) throws IOException, SAXException, ParserConfigurationException
    {
        String fileName = file.getFileName().toString();
        String name = fileName.substring( 0, fileName.lastIndexOf( '.' ) );

        XmlDocument doc = this.xmlParser.parse( file, this.permissionPolicySchema );

        PermissionPolicyImpl policy = new PermissionPolicyImpl();

        Optional<XmlElement> rolesHolder = doc.getRoot().getFirstChildElement( "roles" );
        if( rolesHolder.isPresent() )
        {
            for( XmlElement roleElement : rolesHolder.get().getChildElements( "role" ) )
            {
                policy.addRole( new RoleImpl( roleElement ) );
            }
        }

        Optional<XmlElement> rulesHolder = doc.getRoot().getFirstChildElement( "rules" );
        if( rulesHolder.isPresent() )
        {
            for( XmlElement grantElement : rulesHolder.get().getChildElements( "test-if" ) )
            {
                String test = grantElement.getAttribute( "user" ).get();
                Permission grant = Permission.get( grantElement.getAttribute( "then-grant" ).get() );
                Expression<Boolean> expr = this.expressionParser.parseExpression( test, TypeTokens.of( Boolean.class ) );
                policy.addGrant( new GrantRule( expr, grant ) );
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

    @OnPathDeleted( PRM_POLS_PATH )
    void removePermissionPolicy( @Nonnull Path file )
    {
        String fileName = file.getFileName().toString();
        String name = fileName.substring( 0, fileName.lastIndexOf( '.' ) );
        this.permissionPolicies.remove( name );
    }
}
