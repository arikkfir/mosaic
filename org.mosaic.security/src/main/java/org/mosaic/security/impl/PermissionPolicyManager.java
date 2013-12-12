package org.mosaic.security.impl;

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
import org.mosaic.modules.Module;
import org.mosaic.modules.Service;
import org.mosaic.pathwatchers.PathWatcher;
import org.mosaic.security.AuthorizationException;
import org.mosaic.security.Permission;
import org.mosaic.security.Subject;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.util.xml.StrictErrorHandler;
import org.mosaic.util.xml.XmlDocument;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.util.xml.XmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import static org.mosaic.util.resource.PathEvent.*;

/**
 * @author arik
 */
@Component
final class PermissionPolicyManager
{
    private static final Logger LOG = LoggerFactory.getLogger( PermissionPolicyManager.class );

    @Nonnull
    private final Schema permissionPolicySchema;

    @Nonnull
    private final Map<String, PermissionPolicyImpl> permissionPolicies = new ConcurrentHashMap<>();

    @Nonnull
    @Component
    private Module module;

    @Nonnull
    @Service
    private ExpressionParser expressionParser;

    @Nonnull
    @Service
    private XmlParser xmlParser;

    PermissionPolicyManager() throws IOException, SAXException
    {
        Path schemaFile = this.module.getContext().getServerHome().resolve( "schemas/permission-policy-1.0.0.xsd" );
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

    @PathWatcher( value = "${mosaic.home.etc}/security/permission-policies/**/*.xml", events = { CREATED, MODIFIED } )
    void addPermissionPolicy( @Nonnull Path file ) throws IOException, SAXException, ParserConfigurationException
    {
        String fileName = file.getFileName().toString();
        String name = fileName.substring( 0, fileName.lastIndexOf( '.' ) );

        XmlDocument doc = this.xmlParser.parse( file, this.permissionPolicySchema );

        PermissionPolicyImpl policy = new PermissionPolicyImpl();

        XmlElement rolesElement = doc.getRoot().getFirstChildElement( "roles" );
        if( rolesElement != null )
        {
            for( XmlElement roleElement : rolesElement.getChildElements( "role" ) )
            {
                policy.addRole( new RoleImpl( roleElement ) );
            }
        }

        XmlElement rulesElement = doc.getRoot().getFirstChildElement( "rules" );
        if( rulesElement != null )
        {
            for( XmlElement grantElement : rulesElement.getChildElements( "test-if" ) )
            {
                String test = grantElement.requireAttribute( "user" );
                Permission grant = Permission.get( grantElement.requireAttribute( "then-grant" ) );
                policy.addGrant( new GrantRule( this.expressionParser.parseExpression( test, Boolean.class ), grant ) );
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

    @PathWatcher( value = "${mosaic.home.etc}/security/permission-policies/**/*.xml", events = DELETED )
    void removePermissionPolicy( @Nonnull Path file )
    {
        String fileName = file.getFileName().toString();
        String name = fileName.substring( 0, fileName.lastIndexOf( '.' ) );
        this.permissionPolicies.remove( name );
    }
}
