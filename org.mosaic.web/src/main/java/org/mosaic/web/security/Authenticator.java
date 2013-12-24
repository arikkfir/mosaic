package org.mosaic.web.security;

import javax.annotation.Nonnull;
import org.mosaic.modules.Service;
import org.mosaic.security.Security;
import org.mosaic.security.Subject;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public interface Authenticator
{
    final class AuthenticationAction
    {
        @Nonnull
        private final Subject subject;

        @Nonnull
        private final AuthenticationResult result;

        @Nonnull
        @Service
        private Security security;

        public AuthenticationAction( @Nonnull AuthenticationResult result )
        {
            if( result == AuthenticationResult.AUTHENTICATED )
            {
                throw new IllegalArgumentException( "cannot return AUTHENTICATED result with no subject" );
            }
            this.subject = this.security.getAnonymousSubject();
            this.result = result;
        }

        public AuthenticationAction( @Nonnull Subject subject, @Nonnull AuthenticationResult result )
        {
            this.subject = subject;
            this.result = result;
        }

        @Nonnull
        public Subject getSubject()
        {
            return this.subject;
        }

        @Nonnull
        public AuthenticationResult getResult()
        {
            return this.result;
        }
    }

    @Nonnull
    String getAuthenticationMethod();

    @Nonnull
    AuthenticationAction authenticate( @Nonnull WebRequest request );

    void challange( @Nonnull WebRequest request );

    enum AuthenticationResult
    {
        /**
         * No (or irrelevant) credentials provided, thus ignored by the authenticator.
         */
        NO_CREDENTIALS,

        /**
         * Relevant credentials found, but are illegal. This does NOT signal bad credentials such as wrong password,
         * but rather that the credentials format was malformed (eg. illegal authentication header, key decryption
         * failed, etc).
         */
        INVALID_CREDENTIALS,

        /**
         * Relevant credentials found and they were legal, but the actual authentication failed - eg. incorrect password.
         */
        AUTHENTICATION_FAILED,

        /**
         * Authentication succeeded.
         */
        AUTHENTICATED
    }
}
