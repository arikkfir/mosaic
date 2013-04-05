package org.mosaic.web.application;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.security.policy.PermissionPolicy;
import org.mosaic.web.security.AuthenticatorType;

/**
 * @author arik
 */
public interface WebApplication extends Map<String, Object>
{
    @Nonnull
    String getName();

    @Nonnull
    String getDisplayName();

    @Nonnull
    Map<String, String> getParameters();

    @Nonnull
    Set<String> getVirtualHosts();

    boolean isHostIncluded( @Nonnull String host );

    boolean isAddressAllowed( @Nonnull String address );

    @Nonnull
    Collection<String> getRealms();

    @Nonnull
    PermissionPolicy getPermissionPolicy();

    boolean isUriLanguageSelectionEnabled();

    @Nonnull
    String getDefaultCmsLanguage();

    @Nonnull
    Collection<String> getSupportedCmsLanguages();

    boolean isResourceCompressionEnabled();

    @Nullable
    String getFormLoginUrl();

    @Nullable
    String getAccessDeniedUrl();

    @Nullable
    String getUnknownUrlPage();

    @Nullable
    String getInternalErrorPage();

    @Nonnull
    Collection<AuthenticatorType> getDefaultAuthenticatorTypes();

    @Nonnull
    Collection<AuthenticatorType> getServiceAuthenticatorTypes();

    @Nonnull
    Collection<AuthenticatorType> getResourceAuthenticatorTypes();

    @Nonnull
    Collection<AuthenticatorType> getPageAuthenticatorTypes();
}
