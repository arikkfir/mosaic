package org.mosaic.server.shell.impl.auth;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.mosaic.MosaicHome;
import org.mosaic.lifecycle.ServiceRef;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class MosaicPasswordAuthenticator implements PasswordAuthenticator {

    private static final Logger LOG = LoggerFactory.getBundleLogger( MosaicPasswordAuthenticator.class );

    private MosaicHome home;

    @ServiceRef
    public void setHome( MosaicHome home ) {
        this.home = home;
    }

    @Override
    public boolean authenticate( String username, String password, ServerSession session ) {

        Path passwordFile = this.home.getEtc().resolve( "passwd" );
        if( !Files.exists( passwordFile ) || !Files.isReadable( passwordFile ) ) {
            LOG.warn( "The '{}' file could not be found or read - no password authentication can occur", passwordFile );
            return false;
        }

        try( Reader reader = Files.newBufferedReader( passwordFile, Charset.forName( "UTF-8" ) ) ) {

            Properties properties = new Properties();
            properties.load( reader );

            String sentMd5Password = DigestUtils.md5Hex( password );
            String md5Password = properties.getProperty( username );
            if( md5Password == null ) {
                LOG.warn( "Unknown user: {}", username );
                return false;
            }

            return md5Password.equalsIgnoreCase( sentMd5Password );

        } catch( IOException e ) {
            LOG.warn( "Error reading passwords from '{}': {}", passwordFile, e.getMessage(), e );
            return false;
        }

    }
}
