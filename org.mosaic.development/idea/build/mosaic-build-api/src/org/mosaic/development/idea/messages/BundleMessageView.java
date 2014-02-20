package org.mosaic.development.idea.messages;

import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author arik
 */
public interface BundleMessageView
{
    void clear();

    void showError( @NotNull String bundleName, @Nullable Navigatable target, @NotNull String... errors );

    static class SERVICE
    {
        public static BundleMessageView getInstance( @NotNull Project project )
        {
            return project.getComponent( BundleMessageView.class );
        }
    }
}
