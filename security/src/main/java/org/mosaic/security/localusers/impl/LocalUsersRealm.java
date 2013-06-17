package org.mosaic.security.localusers.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.mosaic.filewatch.annotation.FileWatcher;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.mosaic.security.User;
import org.mosaic.security.credentials.Password;
import org.mosaic.security.credentials.PublicKeys;
import org.mosaic.security.localusers.LocalUser;
import org.mosaic.security.realm.MutableUser;
import org.mosaic.security.realm.Realm;
import org.mosaic.util.xml.StrictErrorHandler;
import org.mosaic.util.xml.XmlDocument;
import org.mosaic.util.xml.XmlElement;
import org.mosaic.util.xml.XmlParser;
import org.xml.sax.SAXException;

import static java.util.Collections.emptyMap;
import static org.mosaic.filewatch.WatchEvent.*;
import static org.mosaic.filewatch.WatchRoot.ETC;

/**
 * @author arik
 */
@Service(value = Realm.class, properties = @Service.P(key = "name", value = "local"))
public class LocalUsersRealm implements Realm
{
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
    private XmlParser xmlParser;

    @Nonnull
    private Map<String, UserInfo> users = emptyMap();

    @ServiceRef
    public void setXmlParser( @Nonnull XmlParser xmlParser )
    {
        this.xmlParser = xmlParser;
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
        UserInfo userInfo = this.users.get( user.getName() );
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

    @FileWatcher(root = ETC, pattern = "users.xml", event = { FILE_ADDED, FILE_MODIFIED })
    public void onFileModified( @Nonnull Path file, @Nonnull BasicFileAttributes attrs )
            throws IOException, ParserConfigurationException, SAXException
    {
        XmlDocument doc = this.xmlParser.parse( file, USERS_SCHEMA );

        Map<String, UserInfo> users = new HashMap<>();
        for( XmlElement userElement : doc.getRoot().getChildElements( "user" ) )
        {
            UserInfo userInfo = new UserInfo( userElement );
            users.put( userInfo.name, userInfo );
        }
        this.users = users;
    }

    @FileWatcher(root = ETC, pattern = "users.xml", event = FILE_DELETED)
    public void onFileDeleted( @Nonnull Path file ) throws IOException
    {
        this.users = emptyMap();
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
        private final String name;

        @Nullable
        private final Password password;

        @Nullable
        private final PublicKeys publicKeys;

        @Nonnull
        private final Set<String> roles;

        private UserInfo( @Nonnull XmlElement element ) throws IOException
        {
            this.name = element.requireAttribute( "name" );

            XmlElement passwordElement = element.getFirstChildElement( "password" );
            if( passwordElement != null )
            {
                String value = passwordElement.requireAttribute( "value" );
                String encryption = passwordElement.requireAttribute( "encryption" );
                this.password = new PasswordImpl( value, PasswordEncryption.valueOf( encryption.toUpperCase() ) );
            }
            else
            {
                this.password = null;
            }

            XmlElement publicKeysElement = element.getFirstChildElement( "public-keys" );
            if( publicKeysElement != null )
            {
                String keys = publicKeysElement.getValue();
                if( keys != null && !keys.trim().isEmpty() )
                {
                    AuthorizedKeys authorizedKeys = new AuthorizedKeys( keys.trim() );
                    this.publicKeys = authorizedKeys.getKeys().isEmpty() ? null : authorizedKeys;
                }
                else
                {
                    this.publicKeys = null;
                }
            }
            else
            {
                this.publicKeys = null;
            }

            XmlElement rolesElement = element.getFirstChildElement( "roles" );
            if( rolesElement != null )
            {
                Set<String> roles = new LinkedHashSet<>();
                for( XmlElement roleElement : rolesElement.getChildElements( "role" ) )
                {
                    roles.add( roleElement.getValue() );
                }
                this.roles = Collections.unmodifiableSet( roles );
            }
            else
            {
                this.roles = Collections.emptySet();
            }
        }
    }
}
