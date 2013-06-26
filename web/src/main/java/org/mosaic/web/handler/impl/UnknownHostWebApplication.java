package org.mosaic.web.handler.impl;

import java.nio.file.Path;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.mosaic.security.policy.PermissionPolicy;
import org.mosaic.util.collect.EmptyMapEx;
import org.mosaic.util.collect.MapEx;
import org.mosaic.web.application.Page;
import org.mosaic.web.application.Snippet;
import org.mosaic.web.application.WebApplication;
import org.mosaic.web.application.impl.WebApplicationFactory;
import org.mosaic.web.security.AuthenticatorType;

/**
 * @author arik
 */
public class UnknownHostWebApplication implements WebApplication
{
    private static final List<String> CONTENT_LANGUAGES = Arrays.asList( "en" );

    public static final WebApplication INSTANCE = new UnknownHostWebApplication();

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
        return Collections.emptyList();
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
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Collection<AuthenticatorType> getServiceAuthenticatorTypes()
    {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Collection<AuthenticatorType> getResourceAuthenticatorTypes()
    {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Collection<AuthenticatorType> getPageAuthenticatorTypes()
    {
        return Collections.emptyList();
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
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Map<String, Snippet> getSnippetMap()
    {
        return Collections.emptyMap();
    }

    @Nonnull
    @Override
    public Collection<Snippet> getSnippets()
    {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Map<String, Page> getPageMap()
    {
        return Collections.emptyMap();
    }

    @Nonnull
    @Override
    public Collection<Page> getPages()
    {
        return Collections.emptyList();
    }

    @Override
    public int size()
    {
        return 0;
    }

    @Override
    public boolean isEmpty()
    {
        return true;
    }

    @Override
    public boolean containsKey( Object key )
    {
        return false;
    }

    @Override
    public boolean containsValue( Object value )
    {
        return false;
    }

    @Override
    public Object get( Object key )
    {
        return null;
    }

    @Override
    public Object put( String key, Object value )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove( Object key )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll( @Nonnull Map<? extends String, ?> m )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public Set<String> keySet()
    {
        return Collections.emptySet();
    }

    @Nonnull
    @Override
    public Collection<Object> values()
    {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Set<Entry<String, Object>> entrySet()
    {
        return Collections.emptySet();
    }
}
