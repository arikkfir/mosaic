package org.mosaic.development.idea.osmorc;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.extensions.PluginId;
import javax.swing.event.HyperlinkEvent;
import org.jetbrains.annotations.NotNull;

import static com.intellij.ide.plugins.PluginManager.isPluginInstalled;
import static com.intellij.ide.plugins.PluginManagerCore.getDisabledPlugins;
import static com.intellij.notification.NotificationType.WARNING;

/**
 * @author arik
 */
public class OsmorcConflictDetector implements ApplicationComponent
{

    private static final String GROUP_DISPLAY_ID = "Mosaic";

    private static final String TITLE = "Mosaic/Osmorc conflict detected";

    private static final String CONTENT = "<html>" +
                                          "<body>" +
                                          "Mosaic and Osmorc plugins cannot run together." +
                                          "<p><a href='#'>Deactivate Osmorc</a>" +
                                          "</body>" +
                                          "</html>";

    @Override
    public void initComponent()
    {
        ApplicationManager.getApplication().invokeLater( new Runnable()
        {
            @Override
            public void run()
            {
                ApplicationManager.getApplication().runWriteAction( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if( isPluginInstalled( PluginId.getId( "Osmorc" ) ) && !getDisabledPlugins().contains( "Osmorc" ) )
                        {
                            NotificationListener listener = new NotificationListener()
                            {
                                @Override
                                public void hyperlinkUpdate( @NotNull Notification notification,
                                                             @NotNull HyperlinkEvent event )
                                {
                                    if( PluginManager.disablePlugin( "Osmorc" ) )
                                    {
                                        ApplicationManager.getApplication().restart();
                                    }
                                }
                            };
                            Notification notification = new Notification( GROUP_DISPLAY_ID, TITLE, CONTENT, WARNING, listener );
                            Notifications.Bus.notify( notification );
                        }
                    }
                } );
            }
        } );
    }

    @Override
    public void disposeComponent()
    {
        // no-op
    }

    @NotNull
    @Override
    public String getComponentName()
    {
        return getClass().getSimpleName();
    }
}
