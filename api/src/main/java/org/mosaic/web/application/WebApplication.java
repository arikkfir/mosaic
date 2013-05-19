package org.mosaic.web.application;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.security.policy.PermissionPolicy;
import org.mosaic.util.collect.MapEx;
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
    Set<String> getVirtualHosts();

    boolean isHostIncluded( @Nonnull String host );

    @Nonnull
    Collection<String> getContentLanguages();

    boolean isUriLanguageSelectionEnabled();

    @Nonnull
    String getDefaultLanguage();

    @Nullable
    String getUnknownUrlPage();

    @Nullable
    String getInternalErrorPage();

    boolean isResourceCompressionEnabled();

    @Nonnull
    MapEx<String, String> getParameters();

    @Nonnull
    Collection<String> getRealms();

    @Nonnull
    PermissionPolicy getPermissionPolicy();

    @Nonnull
    Collection<AuthenticatorType> getDefaultAuthenticatorTypes();

    @Nonnull
    Collection<AuthenticatorType> getServiceAuthenticatorTypes();

    @Nonnull
    Collection<AuthenticatorType> getResourceAuthenticatorTypes();

    @Nonnull
    Collection<AuthenticatorType> getPageAuthenticatorTypes();

    @Nullable
    String getFormLoginUrl();

    @Nullable
    String getAccessDeniedUrl();
}
