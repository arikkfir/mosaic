package org.mosaic.idea.module.facet;

import com.intellij.execution.ExecutionHelper;
import com.intellij.ide.errorTreeView.NewErrorTreeViewPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.pom.Navigatable;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.MessageView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.util.ui.MessageCategory.ERROR;

/**
 * @author arik
 */
public class BuildMessages extends AbstractProjectComponent
{
    private static final String ERRORS_TITLE = "Mosaic Build Errors";

    private static final ExecutionHelper.FakeNavigatable FAKE_NAVIGATABLE = new ExecutionHelper.FakeNavigatable();

    public static BuildMessages getInstance( Project project )
    {
        return project.getComponent( BuildMessages.class );
    }

    public BuildMessages( Project project )
    {
        super( project );
    }

    @NotNull
    @Override
    public String getComponentName()
    {
        return getClass().getSimpleName();
    }

    public void clearBundleMessages()
    {
        if( ApplicationManager.getApplication().isDispatchThread() )
        {
            // obtain the messages view
            MessageView messageView = MessageView.SERVICE.getInstance( this.myProject );
            ContentManager contentManager = messageView.getContentManager();

            // get the tab for this module - creating it if there isn't one
            Content content = contentManager.findContent( ERRORS_TITLE );
            if( content != null )
            {
                contentManager.removeContent( content, true );
            }
        }
        else
        {
            ApplicationManager.getApplication().invokeAndWait( new Runnable()
            {
                @Override
                public void run()
                {
                    clearBundleMessages();
                }
            }, ModalityState.any() );
        }
    }

    public void showError( @NotNull Module module, @Nullable VirtualFile file, String... errors )
    {
        showError( module,
                   file == null
                   ? new ExecutionHelper.FakeNavigatable()
                   : new OpenFileDescriptor( module.getProject(), file, 0 ),
                   errors );
    }

    public void showError( @NotNull final Module module, final Navigatable target, final String... errors )
    {
        if( ApplicationManager.getApplication().isDispatchThread() )
        {
            addContent( module.getName(), target, errors );
        }
        else
        {
            ApplicationManager.getApplication().invokeAndWait( new Runnable()
            {
                @Override
                public void run()
                {
                    addContent( module.getName(), target, errors );
                }
            }, ModalityState.any() );
        }
    }

    private void addContent( String bundleName, Navigatable target, String[] errors )
    {
        // obtain the messages view
        MessageView messageView = MessageView.SERVICE.getInstance( this.myProject );
        ContentManager contentManager = messageView.getContentManager();

        // get the tab for this module - creating it if there isn't one
        Content content = contentManager.findContent( ERRORS_TITLE );
        if( content == null )
        {
            content = contentManager.getFactory().createContent( new ErrorTreeView(), ERRORS_TITLE, true );
            contentManager.addContent( content );
        }

        // add the message
        ErrorTreeView component = ( ErrorTreeView ) content.getComponent();
        component.addMessage( ERROR, errors, bundleName, target == null ? FAKE_NAVIGATABLE : target, null, null, null );

        ToolWindow messagesToolWindow = ToolWindowManager.getInstance( this.myProject ).getToolWindow( ToolWindowId.MESSAGES_WINDOW );
        if( messagesToolWindow != null )
        {
            messagesToolWindow.activate( null, true );
        }
    }

    private class ErrorTreeView extends NewErrorTreeViewPanel
    {
        private ErrorTreeView()
        {
            super( BuildMessages.this.myProject, null, true, true, null );
        }
    }
}
