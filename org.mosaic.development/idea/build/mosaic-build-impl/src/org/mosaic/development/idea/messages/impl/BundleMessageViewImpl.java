package org.mosaic.development.idea.messages.impl;

import com.intellij.execution.ExecutionHelper;
import com.intellij.ide.errorTreeView.NewErrorTreeViewPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.pom.Navigatable;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.MessageView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mosaic.development.idea.messages.BundleMessageView;

import static com.intellij.util.ui.MessageCategory.ERROR;

/**
 * @author arik
 */
public class BundleMessageViewImpl extends AbstractProjectComponent implements BundleMessageView
{
    private static final String OSGI_BUNDLE_ERRORS_TITLE = "OSGi Bundle Errors";

    private static final ExecutionHelper.FakeNavigatable FAKE_NAVIGATABLE = new ExecutionHelper.FakeNavigatable();

    public BundleMessageViewImpl( @NotNull Project project )
    {
        super( project );
    }

    @NotNull
    @Override
    public String getComponentName()
    {
        return getClass().getSimpleName();
    }

    public void clear()
    {
        if( ApplicationManager.getApplication().isDispatchThread() )
        {
            clearContent();
        }
        else
        {
            ApplicationManager.getApplication().invokeAndWait( new Runnable()
            {
                @Override
                public void run()
                {
                    clearContent();
                }
            }, ModalityState.any() );
        }
    }

    public void showError( @NotNull final String bundleName,
                           @Nullable final Navigatable target,
                           @NotNull final String... errors )
    {
        if( ApplicationManager.getApplication().isDispatchThread() )
        {
            addContent( bundleName, target, errors );
        }
        else
        {
            ApplicationManager.getApplication().invokeAndWait( new Runnable()
            {
                @Override
                public void run()
                {
                    addContent( bundleName, target, errors );
                }
            }, ModalityState.any() );
        }
    }

    private void clearContent()
    {
        // obtain the messages view
        MessageView messageView = MessageView.SERVICE.getInstance( this.myProject );
        ContentManager contentManager = messageView.getContentManager();

        // get the tab for this module - creating it if there isn't one
        Content content = contentManager.findContent( OSGI_BUNDLE_ERRORS_TITLE );
        if( content != null )
        {
            contentManager.removeContent( content, true );
        }
    }

    private void addContent( @NotNull String bundleName, @Nullable Navigatable target, @NotNull String... errors )
    {
        // obtain the messages view
        MessageView messageView = MessageView.SERVICE.getInstance( this.myProject );
        ContentManager contentManager = messageView.getContentManager();

        // get the tab for this module - creating it if there isn't one
        Content content = contentManager.findContent( OSGI_BUNDLE_ERRORS_TITLE );
        if( content == null )
        {
            content = contentManager.getFactory().createContent(
                    new OsgiErrorTreeView( this.myProject ), OSGI_BUNDLE_ERRORS_TITLE, true );
            contentManager.addContent( content );
        }

        // add the message
        OsgiErrorTreeView component = ( OsgiErrorTreeView ) content.getComponent();
        component.addMessage( ERROR, errors, bundleName, target == null ? FAKE_NAVIGATABLE : target, null, null, null );

        ToolWindow messagesToolWindow = ToolWindowManager.getInstance( this.myProject ).getToolWindow( ToolWindowId.MESSAGES_WINDOW );
        if( messagesToolWindow != null )
        {
            messagesToolWindow.activate( null, true );
        }
    }

    private static class OsgiErrorTreeView extends NewErrorTreeViewPanel
    {
        private OsgiErrorTreeView( @NotNull Project project )
        {
            super( project, null, true, true, null );
        }
    }
}
