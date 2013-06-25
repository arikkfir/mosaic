package org.mosaic.web.application;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.Period;
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

    @Nonnull
    Collection<String> getContentLanguages();

    boolean isUriLanguageSelectionEnabled();

    @Nonnull
    String getDefaultLanguage();

    @Nullable
    Page getUnknownUrlPage();

    @Nullable
    Page getInternalErrorPage();

    boolean isResourceCompressionEnabled();

    @Nonnull
    MapEx<String, String> getParameters();

    @Nonnull
    Collection<String> getRealms();

    @Nonnull
    PermissionPolicy getPermissionPolicy();

    @Nonnull
    Period getMaxSessionAge();

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
    Page getAccessDeniedPage();

    @Nonnull
    Collection<Path> getContentRoots();

    @Nonnull
    Map<String, Snippet> getSnippetMap();

    @Nonnull
    Collection<Snippet> getSnippets();

    @Nonnull
    Map<String, Page> getPageMap();

    @Nonnull
    Collection<Page> getPages();
}
