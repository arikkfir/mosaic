package org.mosaic.it.modules.module02.impl;

import java.io.IOException;
import java.nio.file.Paths;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.mosaic.shell.annotation.Command;
import org.mosaic.web.application.WebApplication;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author arik
 */
@Bean
public class WebAppWired
{
    @Nullable
    private WebApplication webApp;

    @ServiceRef( value = "name=app01", required = false )
    public void setWebApp( @Nullable WebApplication webApp )
    {
        this.webApp = webApp;
    }

    @Command
    public int checkWebAppContentRoot() throws IOException
    {
        if( this.webApp == null )
        {
            return -1;
        }
        checkState( this.webApp.getContentRoots().size() == 1, "wrong content roots size: " + this.webApp.getContentRoots().size() );
        checkState( this.webApp.getContentRoots().contains( Paths.get( "/some/dir/module02-updated" ) ), "wrong content roots: " + this.webApp.getContentRoots() );
        return 0;
    }

    @Command
    public int checkWebAppWired() throws IOException
    {
        if( this.webApp == null )
        {
            return -1;
        }

        checkState( "app01".equals( this.webApp.getName() ), "incorrect name: " + this.webApp.getName() );
        checkState( "Module 02".equals( this.webApp.getDisplayName() ), "incorrect display name: " + this.webApp.getDisplayName() );
        checkState( this.webApp.getVirtualHosts().size() == 1, "wrong vhosts size: " + this.webApp.getVirtualHosts() );
        checkState( this.webApp.getVirtualHosts().contains( "module02.mosaic.local" ), "missing vhost entry from: " + this.webApp.getVirtualHosts() );
        checkState( this.webApp.getContentLanguages().size() == 2, "wrong content languages size: " + this.webApp.getContentLanguages().size() );
        checkState( this.webApp.getContentLanguages().contains( "en" ), "missing en content language from: " + this.webApp.getContentLanguages() );
        checkState( this.webApp.getContentLanguages().contains( "he" ), "missing he content language from: " + this.webApp.getContentLanguages() );
        checkState( this.webApp.isUriLanguageSelectionEnabled(), "false from isUriLanguageSelectionEnabled" );
        checkState( "en".equals( this.webApp.getDefaultLanguage() ), "wrong getDefaultLanguage: " + this.webApp.getDefaultLanguage() );
        checkState( this.webApp.getUnknownUrlPage() != null, "null unknown URL page" );
        checkState( this.webApp.getInternalErrorPage() != null, "null internal error page" );
        checkState( this.webApp.isResourceCompressionEnabled(), "false from isResourceCompressionEnabled" );
        checkState( this.webApp.getParameters().size() == 1, "wrong parameters size: " + this.webApp.getParameters().size() );
        checkState( this.webApp.getParameters().containsKey( "p1" ), "missing p1 parameter from: " + this.webApp.getParameters() );
        checkState( "v1".equals( this.webApp.getParameters().get( "p1" ) ), "wrong p1 parameter value: " + this.webApp.getParameters().get( "p1" ) );
        checkState( this.webApp.getRealms().size() == 1, "wrong realms size: " + this.webApp.getRealms().size() );
        checkState( this.webApp.getRealms().contains( "local" ), "missing local realm from: " + this.webApp.getRealms() );
        //noinspection ConstantConditions
        checkState( this.webApp.getPermissionPolicy() != null, "null permission policy" );
        checkState( "/login".equals( this.webApp.getFormLoginUrl() ), "wrong form login url: " + this.webApp.getFormLoginUrl() );
        checkState( this.webApp.getAccessDeniedPage() != null, "null access denied page" );
        checkState( this.webApp.getContentRoots().size() == 1, "wrong content roots size: " + this.webApp.getContentRoots().size() );
        checkState( this.webApp.getContentRoots().contains( Paths.get( "/some/dir/module02" ) ), "wrong content roots: " + this.webApp.getContentRoots() );
        return 0;
    }
}
