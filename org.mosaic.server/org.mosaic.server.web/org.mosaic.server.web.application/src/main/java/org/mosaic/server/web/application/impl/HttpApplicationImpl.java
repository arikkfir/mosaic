package org.mosaic.server.web.application.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.security.PermissionPolicy;
import org.mosaic.util.collection.TypedDict;
import org.mosaic.util.collection.WrappingTypedDict;
import org.mosaic.web.HttpApplication;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import static java.nio.file.Files.*;
import static java.util.Collections.unmodifiableMap;
import static org.mosaic.logging.LoggerFactory.getBundleLogger;
import static org.mosaic.server.web.application.impl.DomUtils.*;

/**
 * @author arik
 */
public class HttpApplicationImpl extends WrappingTypedDict<Object> implements HttpApplication {

    private final Logger logger;

    private final Path path;

    private final String name;

    private TypedDict<String> parameters;

    private Set<Pattern> virtualHosts;

    private Set<Pattern> allowedClientAddresses;

    private Set<Pattern> restrictedClientAddresses;

    private PermissionPolicy permissionPolicy;

    private ServiceRegistration<HttpApplication> registration;

    private long modificationTime;

    public HttpApplicationImpl( Path appFile, ConversionService conversionService ) {
        super( new ConcurrentHashMap<String, List<Object>>(), conversionService, Object.class );

        this.path = appFile;

        String fileName = appFile.getFileName().toString();
        this.name = fileName.substring( 0, fileName.length() - ".xml".length() );
        this.logger = LoggerFactory.getLogger( getBundleLogger( HttpApplicationImpl.class ).getName() + "." + this.name );
    }

    public void refresh() {
        if( isDirectory( this.path ) || !exists( this.path ) || !isReadable( this.path ) ) {

            if( this.modificationTime > 0 ) {

                logger.info( "Application '{}' no longer exists/readable at: {}", this.name, this.path );
                this.modificationTime = 0;
                unregister();

            }

        } else {

            try {

                long modificationTime = getLastModifiedTime( this.path ).toMillis();
                if( modificationTime > this.modificationTime ) {
                    this.modificationTime = modificationTime;
                    register();
                }

            } catch( Exception e ) {
                logger.error( "Could not refresh application '{}': {}", this.name, e.getMessage(), e );
            }

        }
    }

    public void register() throws IOException, SAXException, ParserConfigurationException {
        logger.info( "Adding application '{}' from: {}", this.name, this.path );
        parse();

        // register as a data source and transaction manager
        Dictionary<String, Object> dsDict = new Hashtable<>();
        dsDict.put( "name", this.name );
        if( this.registration != null ) {
            this.registration.setProperties( dsDict );
        } else {
            Bundle bundle = FrameworkUtil.getBundle( getClass() );
            this.registration = bundle.getBundleContext().registerService( HttpApplication.class, this, dsDict );
        }
    }

    public void unregister() {
        this.logger.info( "Removing application '{}'", this.name );
        try {
            this.registration.unregister();
        } catch( IllegalStateException ignore ) {
        }
    }

    public Path getPath() {
        return path;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public TypedDict<String> getParameters() {
        return this.parameters;
    }

    @Override
    public Set<String> getVirtualHosts() {
        Set<String> hosts = new HashSet<>();
        for( Pattern pattern : this.virtualHosts ) {
            hosts.add( pattern.pattern() );
        }
        return hosts;
    }

    @Override
    public boolean isHostIncluded( String host ) {
        if( this.virtualHosts.isEmpty() ) {
            return true;
        }

        for( Pattern pattern : this.virtualHosts ) {
            if( pattern.matcher( host ).matches() ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAddressAllowed( String address ) {
        for( Pattern pattern : this.restrictedClientAddresses ) {
            if( pattern.matcher( address ).matches() ) {
                // restricted
                return false;
            }
        }

        if( this.allowedClientAddresses.isEmpty() ) {

            // if no allowed patterns defined, allowed
            return true;

        } else {

            for( Pattern pattern : this.allowedClientAddresses ) {
                if( pattern.matcher( address ).matches() ) {
                    return true;
                }
            }
            return false;

        }
    }

    @Override
    public PermissionPolicy getPermissionPolicy() {
        return this.permissionPolicy;
    }

    private void parse() throws IOException, SAXException, ParserConfigurationException {
        Element appElt = parseDocument( this.path ).getDocumentElement();
        if( !appElt.getLocalName().equals( "application" ) ) {
            throw new IllegalArgumentException( "Could not find <application> tag in application file '" + this.path + "'" );
        }

        // parse virtual hosts
        Element virtualHostsElt = getFirstChildElement( appElt, "virtual-hosts" );
        Set<Pattern> virtualHosts = new HashSet<>();
        if( virtualHostsElt != null ) {
            for( Element virtualHostElt : getChildElements( virtualHostsElt, "virtual-host" ) ) {
                virtualHosts.add( Pattern.compile( virtualHostElt.getTextContent().trim() ) );
            }
        }
        this.virtualHosts = Collections.unmodifiableSet( virtualHosts );

        // parse security
        RulePermissionPolicy permissionPolicy = new RulePermissionPolicy();
        Element securityElt = getFirstChildElement( appElt, "security" );
        if( securityElt != null ) {

            // parse allowed-client-addresses
            Element allowedClientAddressesElt = getFirstChildElement( securityElt, "allowed-client-addresses" );
            Set<Pattern> allowedClientAddresses = new HashSet<>();
            if( allowedClientAddressesElt != null ) {
                for( Element addressElt : getChildElements( allowedClientAddressesElt, "address" ) ) {
                    allowedClientAddresses.add( Pattern.compile( addressElt.getTextContent().trim() ) );
                }
            }
            this.allowedClientAddresses = Collections.unmodifiableSet( allowedClientAddresses );

            // parse restricted-client-addresses
            Element restrictedClientAddressesElt = getFirstChildElement( securityElt, "restricted-client-addresses" );
            Set<Pattern> restrictedClientAddresses = new HashSet<>();
            if( restrictedClientAddressesElt != null ) {
                for( Element addressElt : getChildElements( restrictedClientAddressesElt, "address" ) ) {
                    restrictedClientAddresses.add( Pattern.compile( addressElt.getTextContent().trim() ) );
                }
            }
            this.restrictedClientAddresses = Collections.unmodifiableSet( restrictedClientAddresses );

            // parse role permissions
            Element rolesElt = getFirstChildElement( securityElt, "roles" );
            if( rolesElt != null ) {
                for( Role role : parseRoles( rolesElt ) ) {
                    permissionPolicy.addRolePermissionsRule( role.getName(), role.getPermissionPatterns() );
                }
            }

            // parse permission policy rules
            Element permissionPolicyElt = getFirstChildElement( securityElt, "permission-policy" );
            if( permissionPolicyElt != null ) {
                for( Element ruleElt : getChildElements( permissionPolicyElt, "rule" ) ) {
                    permissionPolicy.addCredentialTypeExpressionRule(
                            ruleElt.getAttribute( "type" ), ruleElt.getAttribute( "expression" ) );
                }
            }

        }
        this.permissionPolicy = permissionPolicy;

        // parse parameters
        Element paramsElt = getFirstChildElement( appElt, "parameters" );
        WrappingTypedDict<String> params = new WrappingTypedDict<>( this.conversionService, String.class );
        if( paramsElt != null ) {
            for( Element paramElt : getChildElements( paramsElt ) ) {
                params.add( paramElt.getLocalName(), paramElt.getTextContent().trim() );
            }
        }
        this.parameters = new WrappingTypedDict<>( unmodifiableMap( params.getMap() ), this.conversionService, String.class );
    }

    private static Collection<Role> parseRoles( Element rolesElt ) {
        Map<String, Role> rolesMap = new LinkedCaseInsensitiveMap<>();
        Set<Role> rootRoles = new HashSet<>();
        for( Element roleElt : getChildElements( rolesElt ) ) {
            rootRoles.add( new Role( roleElt ) );
        }
        for( Role rootRole : rootRoles ) {
            rootRole.populateRoles( rolesMap );
        }
        return rolesMap.values();
    }

}
