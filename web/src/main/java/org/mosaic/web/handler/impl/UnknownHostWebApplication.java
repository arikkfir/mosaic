package org.mosaic.web.handler.impl;

import java.nio.file.Path;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.mosaic.security.policy.PermissionPolicy;
import org.mosaic.util.collect.EmptyMapEx;
import org.mosaic.util.collect.MapEx;
import org.mosaic.web.application.*;
import org.mosaic.web.application.impl.WebApplicationFactory;
import org.mosaic.web.security.AuthenticatorType;

import static java.util.Collections.emptyList;

/**
 * @author arik
 */
public class UnknownHostWebApplication implements WebApplication, WebContent
{
    private static final List<String> CONTENT_LANGUAGES = Arrays.asList( "en" );

    private static final Period NO_CACHE = Period.millis( 0 );

    public static final WebApplication INSTANCE = new UnknownHostWebApplication();

    @Nonnull
    private final DateTime lastModified = DateTime.now();

    @Nonnull
    @Override
    public MapEx<String, Object> getAttributes()
    {
        return EmptyMapEx.emptyMapEx();
    }

    @Nonnull
    @Override
    public WebApplication getApplication()
    {
        return this;
    }

    @Nonnull
    @Override
    public WebContent getWebContent()
    {
        return this;
    }

    @Nonnull
    @Override
    public DateTime getLastModified()
    {
        return this.lastModified;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return "unknown";
    }

    @Nonnull
    @Override
    public String getDisplayName()
    {
        return "Unknown Web Application";
    }

    @Nonnull
    @Override
    public Set<String> getVirtualHosts()
    {
        return Collections.emptySet();
    }

    @Nonnull
    @Override
    public Collection<String> getContentLanguages()
    {
        return CONTENT_LANGUAGES;
    }

    @Override
    public boolean isUriLanguageSelectionEnabled()
    {
        return true;
    }

    @Nonnull
    @Override
    public String getDefaultLanguage()
    {
        return "en";
    }

    @Nullable
    @Override
    public Page getUnknownUrlPage()
    {
        return null;
    }

    @Nullable
    @Override
    public Page getInternalErrorPage()
    {
        return null;
    }

    @Override
    public boolean isResourceCompressionEnabled()
    {
        return true;
    }

    @Nonnull
    @Override
    public MapEx<String, String> getParameters()
    {
        return EmptyMapEx.emptyMapEx();
    }

    @Nonnull
    @Override
    public Collection<String> getRealms()
    {
        return emptyList();
    }

    @Nonnull
    @Override
    public PermissionPolicy getPermissionPolicy()
    {
        return WebApplicationFactory.NO_OP_PERMISSION_POLICY;
    }

    @Nonnull
    @Override
    public Period getMaxSessionAge()
    {
        return new Period( 1, PeriodType.years() );
    }

    @Nonnull
    @Override
    public Collection<AuthenticatorType> getDefaultAuthenticatorTypes()
    {
        return emptyList();
    }

    @Nonnull
    @Override
    public Collection<AuthenticatorType> getServiceAuthenticatorTypes()
    {
        return emptyList();
    }

    @Nonnull
    @Override
    public Collection<AuthenticatorType> getResourceAuthenticatorTypes()
    {
        return emptyList();
    }

    @Nonnull
    @Override
    public Collection<AuthenticatorType> getPageAuthenticatorTypes()
    {
        return emptyList();
    }

    @Nullable
    @Override
    public String getFormLoginUrl()
    {
        return null;
    }

    @Nullable
    @Override
    public Page getAccessDeniedPage()
    {
        return null;
    }

    @Nonnull
    @Override
    public Collection<Path> getContentRoots()
    {
        return emptyList();
    }

    @Nonnull
    @Override
    public Collection<ContextProviderRef> getContext()
    {
        return emptyList();
    }

    @Nullable
    @Override
    public Snippet getSnippet( @Nonnull String name )
    {
        return null;
    }

    @Nonnull
    @Override
    public Collection<Snippet> getSnippets()
    {
        return emptyList();
    }

    @Nullable
    @Override
    public Template getTemplate( @Nonnull String name )
    {
        return null;
    }

    @Nonnull
    @Override
    public Collection<Template> getTemplates()
    {
        return emptyList();
    }

    @Nullable
    @Override
    public Page getPage( @Nonnull String name )
    {
        return null;
    }

    @Nonnull
    @Override
    public Collection<Page> getPages()
    {
        return emptyList();
    }

    @Nonnull
    @Override
    public Period getCachePeriod( @Nonnull String path )
    {
        return NO_CACHE;
    }
}
