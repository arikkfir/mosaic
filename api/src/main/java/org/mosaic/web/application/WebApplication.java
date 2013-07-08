package org.mosaic.web.application;

import java.nio.file.Path;
import java.util.Collection;
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
public interface WebApplication
{
    @Nonnull
    MapEx<String, Object> getAttributes();

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
    Collection<ContextProviderRef> getContext();

    @Nullable
    Snippet getSnippet( @Nonnull String name );

    @Nonnull
    Collection<Snippet> getSnippets();

    @Nullable
    Template getTemplate( @Nonnull String name );

    @Nonnull
    Collection<Template> getTemplates();

    @Nullable
    Page getPage( @Nonnull String name );

    @Nonnull
    Collection<Page> getPages();
}
