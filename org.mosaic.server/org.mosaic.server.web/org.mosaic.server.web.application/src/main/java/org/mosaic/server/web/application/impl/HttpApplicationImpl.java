package org.mosaic.server.web.application.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import org.mosaic.collection.TypedDict;
import org.mosaic.collection.WrappingTypedDict;
import org.mosaic.web.HttpApplication;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import static org.mosaic.server.web.application.impl.DomUtils.*;

/**
 * @author arik
 */
public class HttpApplicationImpl extends WrappingTypedDict<Object> implements HttpApplication {

    private final String name;

    private TypedDict<String> parameters;

    private Set<Pattern> virtualHosts;

    private Set<Pattern> allowedClientAddresses;

    private Set<Pattern> restrictedClientAddresses;

    private Map<String, Role> rolesMap;

    public HttpApplicationImpl( Path appFile, ConversionService conversionService )
            throws ParserConfigurationException, IOException, SAXException {
        super( new ConcurrentHashMap<String, List<Object>>(), conversionService, Object.class );

        String fileName = appFile.getFileName().toString();
        this.name = fileName.substring( 0, fileName.length() - ".xml".length() );

        Element appElt = parseDocument( appFile ).getDocumentElement();
        if( !appElt.getLocalName().equals( "application" ) ) {
            throw new IllegalArgumentException( "Could not find <application> tag in application file '" + appFile + "'" );
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
            Set<Role> rootRoles = new HashSet<>();
            if( rolesElt != null ) {
                for( Element roleElt : getChildElements( rolesElt ) ) {
                    rootRoles.add( new Role( roleElt ) );
                }
            }

            // populate roles map
            Map<String, Role> rolesMap = new LinkedCaseInsensitiveMap<>();
            for( Role rootRole : rootRoles ) {
                rootRole.populateRoles( rolesMap );
            }
            this.rolesMap = Collections.unmodifiableMap( rolesMap );
        }


        this.parameters = new WrappingTypedDict<>( Collections.<String, List<String>>emptyMap(), this.conversionService, String.class );
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
    public Set<String> getAvailableRoles() {
        return this.rolesMap.keySet();
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
    public boolean isPermissionIncluded( String permission, String... roles ) {
        for( String roleName : roles ) {
            Role role = this.rolesMap.get( roleName );
            if( role != null && role.hasPermission( permission ) ) {
                return true;
            }
        }
        return false;
    }

}
