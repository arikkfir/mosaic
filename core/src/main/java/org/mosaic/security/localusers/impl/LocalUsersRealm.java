package org.mosaic.security.localusers.impl;

import java.io.IOException;
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
import org.mosaic.filewatch.FileWatcher;
import org.mosaic.filewatch.WatchEvent;
import org.mosaic.filewatch.WatchRoot;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.security.User;
import org.mosaic.security.credentials.Password;
import org.mosaic.security.credentials.PublicKeys;
import org.mosaic.security.localusers.LocalUser;
import org.mosaic.security.realm.MutableUser;
import org.mosaic.security.realm.Realm;
import org.mosaic.util.xml.impl.Digester;
import org.mosaic.util.xml.impl.StrictErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author arik
 */
@Service(value = Realm.class,
         properties = @Service.P(key = "name", value = "local"))
public class LocalUsersRealm implements Realm
{
    private static final Logger LOG = LoggerFactory.getLogger( LocalUsersRealm.class );

    private static final Schema USERS_SCHEMA;

    static
    {
        SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
        schemaFactory.setErrorHandler( StrictErrorHandler.INSTANCE );
        try
        {
            USERS_SCHEMA = schemaFactory.newSchema( LocalUser.class.getResource( "users.xsd" ) );
        }
        catch( SAXException e )
        {
            throw new IllegalStateException( "Could not find 'users.xsd' schema resource in Mosaic API bundle: " + e.getMessage(), e );
        }
    }

    @Nonnull
    private Users users = new Users();

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

        // add public keys credentials if the local user has one
        if( userInfo.publicKeys != null )
        {
            user.addCredential( userInfo.publicKeys );
        }

        // add local user's roles
        for( String role : userInfo.roles )
        {
            user.addRole( role );
        }

        // all good then!
        return true;
    }

    @FileWatcher(root = WatchRoot.ETC,
                 pattern = "users.xml",
                 event = { WatchEvent.FILE_ADDED, WatchEvent.FILE_MODIFIED })
    public void onFileModified( @Nonnull Path file, @Nonnull BasicFileAttributes attrs ) throws IOException
    {
        Digester digester = new Digester( getClass(), USERS_SCHEMA );
        digester.setRuleNamespaceURI( "https://github.com/arikkfir/mosaic/users-1.0.0" );
        digester.addObjectCreate( "users", Users.class );
        digester.addObjectCreate( "users/user", UserInfo.class );
        digester.addSetProperties( "users/user", "name", "name" );
        digester.addCallMethod( "users/user/password", "setPassword", 2 );
        digester.addCallParam( "users/user/password", 0, "value" );
        digester.addCallParam( "users/user/password", 0, "encryption" );
        digester.addBeanPropertySetter( "users/user/public-keys", "publicKeys" );
        digester.addCallMethod( "users/user/roles/role", "addRole", 1 );
        digester.addCallParam( "users/user/roles/role", 0 );
        digester.addSetNext( "users/user", "addUser" );
        try
        {
            this.users = digester.parse( file.toFile() );
        }
        catch( SAXException e )
        {
            LOG.error( "Error parsing local users realm users at '{}': {}", file, e.getMessage(), e );
        }
    }

    @FileWatcher(root = WatchRoot.ETC,
                 pattern = "users.xml",
                 event = WatchEvent.FILE_DELETED)
    public void onFileDeleted( @Nonnull Path file ) throws IOException
    {
        this.users = new Users();
    }

    private class Users
    {
        @Nonnull
        private final Map<String, UserInfo> users = new HashMap<>();

        @SuppressWarnings("UnusedDeclaration")
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

        @Nullable
        private PublicKeys publicKeys;

        @Nonnull
        private Set<String> roles = new HashSet<>();

        @SuppressWarnings("UnusedDeclaration")
        public void addRole( @Nonnull String role )
        {
            this.roles.add( role );
        }

        @SuppressWarnings("UnusedDeclaration")
        private void setName( @Nonnull String name )
        {
            this.name = name;
        }

        @SuppressWarnings("UnusedDeclaration")
        private void setPassword( @Nonnull String password, @Nonnull String encryption )
        {
            this.password = new PasswordImpl( password, PasswordEncryption.valueOf( encryption.toUpperCase() ) );
        }

        @SuppressWarnings("UnusedDeclaration")
        private void setPublicKeys( @Nullable String publicKeys ) throws IOException
        {
            if( publicKeys == null || publicKeys.trim().isEmpty() )
            {
                this.publicKeys = null;
            }
            else
            {
                AuthorizedKeys authorizedKeys = new AuthorizedKeys( publicKeys );
                if( !authorizedKeys.getKeys().isEmpty() )
                {
                    this.publicKeys = authorizedKeys;
                }
            }
        }
    }
}
