package org.mosaic.security.localusers.impl;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.apache.commons.digester3.Digester;
import org.apache.commons.logging.LogFactory;
import org.mosaic.Server;
import org.mosaic.lifecycle.DP;
import org.mosaic.lifecycle.ServicePropertiesProvider;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.mosaic.security.User;
import org.mosaic.security.credentials.Password;
import org.mosaic.security.localusers.LocalUser;
import org.mosaic.security.realm.MutableUser;
import org.mosaic.security.realm.Realm;
import org.mosaic.util.io.FileVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.*;

/**
 * @author arik
 */
@Service( { Realm.class, FileVisitor.class } )
public class LocalUsersRealm extends FileVisitorAdapter implements Realm, ServicePropertiesProvider
{
    private static final Logger LOG = LoggerFactory.getLogger( LocalUsersRealm.class );

    private static final StrictErrorHandler STRICT_ERROR_HANDLER = new StrictErrorHandler();

    private static final LocalUsersRealmEntityResolver LOCAL_USERS_REALM_ENTITY_RESOLVER = new LocalUsersRealmEntityResolver();

    private static final Schema USERS_SCHEMA;

    static
    {
        SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
        schemaFactory.setErrorHandler( STRICT_ERROR_HANDLER );
        try
        {
            USERS_SCHEMA = schemaFactory.newSchema( LocalUser.class.getResource( "users.xsd" ) );
        }
        catch( SAXException e )
        {
            throw new IllegalStateException( "Could not find 'users.xsd' schema resource in Mosaic API bundle: " + e.getMessage(), e );
        }
    }

    private static class StrictErrorHandler implements ErrorHandler
    {
        @Override
        public void warning( SAXParseException exception ) throws SAXException
        {
            throw exception;
        }

        @Override
        public void error( SAXParseException exception ) throws SAXException
        {
            throw exception;
        }

        @Override
        public void fatalError( SAXParseException exception ) throws SAXException
        {
            throw exception;
        }
    }

    private static class LocalUsersRealmEntityResolver implements EntityResolver
    {
        @Override
        public InputSource resolveEntity( String publicId, String systemId ) throws SAXException, IOException
        {
            LOG.info( "Resolving public ID '{} with system ID '{}'" );
            return null;
        }
    }

    @Nonnull
    private Server server;

    @Nonnull
    private Users users = new Users();

    @ServiceRef
    public void setServer( @Nonnull Server server )
    {
        this.server = server;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return "local";
    }

    @Override
    public boolean loadUser( @Nonnull MutableUser user )
    {
        // check if we have such a user
        UserInfo userInfo = this.users.users.get( user.getName() );
        if( userInfo == null )
        {
            return false;
        }

        // mark user as a local user
        user.addPrincipal( new LocalUserPrincipal() );

        // add password credentials if the local user has one
        if( userInfo.password != null )
        {
            user.addCredential( userInfo.password );
        }

        // add local user's roles
        for( String role : userInfo.roles )
        {
            user.addRole( role );
        }

        // all good then!
        return true;
    }

    @Nonnull
    @Override
    public DP[] getServiceProperties()
    {
        return new DP[] { DP.dp( "root", this.server.getEtc().resolve( "users.xml" ) ) };
    }

    @Override
    public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException
    {
        Digester digester = new Digester();
        digester.setClassLoader( getClass().getClassLoader() );
        digester.setEntityResolver( LOCAL_USERS_REALM_ENTITY_RESOLVER );
        digester.setErrorHandler( STRICT_ERROR_HANDLER );
        digester.setLogger( LogFactory.getLog( getClass().getName() + ".digester" ) );
        digester.setNamespaceAware( true );
        digester.setSAXLogger( LogFactory.getLog( getClass().getName() + ".sax" ) );
        digester.setUseContextClassLoader( false );
        digester.setXMLSchema( USERS_SCHEMA );

        digester.addObjectCreate( "users", Users.class );
        digester.addObjectCreate( "users/user", UserInfo.class );
        digester.addSetProperties( "users/user", "name", "name" );
        digester.addCallMethod( "users/user/password", "setPassword", 2 );
        digester.addCallParam( "users/user/password", 0, "value" );
        digester.addCallParam( "users/user/password", 0, "encryption" );
        digester.addCallMethod( "users/user/roles/role", "addRole", 1 );
        digester.addCallParam( "users/user/roles/role", 0 );
        digester.addSetNext( "users/user", "addUser" );
        try
        {
            this.users = digester.parse( file.toFile() );
        }
        catch( SAXException e )
        {
            LOG.error( "Error parsing '{}': {}", file, e.getMessage(), e );
        }
        return FileVisitResult.CONTINUE;
    }

    private class Users
    {
        @Nonnull
        private final Map<String, UserInfo> users = new HashMap<>();

        @SuppressWarnings( "UnusedDeclaration" )
        public void addUser( @Nonnull UserInfo user )
        {
            if( this.users.containsKey( user.name ) )
            {
                throw new IllegalArgumentException( "User '" + user.name + "' already defined!" );
            }
            else
            {
                this.users.put( user.name, user );
            }
        }
    }

    private class LocalUserPrincipal implements LocalUser
    {
        @Override
        public String getType()
        {
            return "local";
        }

        @Override
        public void attach( User user )
        {
            // no-op
        }

        @Override
        public void detach( User user )
        {
            // no-op
        }
    }

    private class UserInfo
    {
        @Nonnull
        private String name;

        @Nullable
        private Password password;

        @Nonnull
        private Set<String> roles = new HashSet<>();

        @SuppressWarnings( "UnusedDeclaration" )
        public void addRole( @Nonnull String role )
        {
            this.roles.add( role );
        }

        @SuppressWarnings( "UnusedDeclaration" )
        private void setName( @Nonnull String name )
        {
            this.name = name;
        }

        @SuppressWarnings( "UnusedDeclaration" )
        private void setPassword( @Nonnull String password, @Nonnull String encryption )
        {
            this.password = new PasswordImpl( password, PasswordEncryption.valueOf( encryption.toUpperCase() ) );
        }
    }
}
