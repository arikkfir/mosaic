package org.mosaic.web.server.impl.marshall;

import com.google.common.base.Optional;
import freemarker.cache.TemplateLoader;
import freemarker.template.*;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nonnull;
import org.mosaic.modules.Component;
import org.mosaic.web.application.Application;

/**
 * @author arik
 */
@Component
final class FreemarkerRenderer
{
    private static final String CFG_KEY = FreemarkerRenderer.class.getName() + "#configuration";

    void render( @Nonnull Application application,
                 @Nonnull Map<String, Object> context,
                 @Nonnull String path,
                 @Nonnull Locale locale,
                 @Nonnull Writer writer ) throws IOException, TemplateException
    {
        Configuration configuration = getConfigurationForApplication( application );
        Template template = configuration.getTemplate( "app:" + path, locale );
        template.process( context, writer );
    }

    void render( @Nonnull Application application,
                 @Nonnull Map<String, Object> context,
                 @Nonnull Path path,
                 @Nonnull Locale locale,
                 @Nonnull Writer writer ) throws IOException, TemplateException
    {
        Configuration configuration = getConfigurationForApplication( application );
        Template template = configuration.getTemplate( "path:" + path, locale );
        template.process( context, writer );
    }

    @Nonnull
    private Configuration getConfigurationForApplication( @Nonnull Application application )
    {
        Optional<Configuration> cfg = application.getAttributes().find( CFG_KEY, Configuration.class );
        if( !cfg.isPresent() )
        {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized( application )
            {
                cfg = application.getAttributes().find( CFG_KEY, Configuration.class );
                if( !cfg.isPresent() )
                {
                    cfg = Optional.of( new Configuration() );
                    cfg.get().setDefaultEncoding( "UTF-8" );
                    cfg.get().setIncompatibleImprovements( new Version( 2, 3, 20 ) );
                    cfg.get().setTemplateLoader( new ApplicationTemplateLoader( application ) );
                    cfg.get().setTemplateExceptionHandler( TemplateExceptionHandler.RETHROW_HANDLER );
                }
            }
        }
        return cfg.get();
    }

    private class ApplicationTemplateLoader implements TemplateLoader
    {
        @Nonnull
        private final Application application;

        private ApplicationTemplateLoader( @Nonnull Application application )
        {
            this.application = application;
        }

        @Override
        public Object findTemplateSource( String name ) throws IOException
        {
            if( name.startsWith( "app:" ) )
            {
                Application.ApplicationResource resource = this.application.getResource( name.substring( "app:".length() ) + ".ftl" );
                return resource == null ? null : resource.getPath();
            }
            else if( name.startsWith( "path:" ) )
            {
                Path path = Paths.get( name.substring( "path:".length() ) );
                return Files.exists( path ) && Files.isRegularFile( path ) ? path : null;
            }
            else
            {
                return null;
            }
        }

        @Override
        public long getLastModified( Object templateSource )
        {
            Path file = ( Path ) templateSource;
            try
            {
                return Files.getLastModifiedTime( file ).toMillis();
            }
            catch( IOException e )
            {
                return -1;
            }
        }

        @Override
        public Reader getReader( Object templateSource, String encoding ) throws IOException
        {
            Path file = ( Path ) templateSource;
            return Files.newBufferedReader( file, Charset.forName( "UTF-8" ) );
        }

        @Override
        public void closeTemplateSource( Object templateSource ) throws IOException
        {
            // no-op
        }
    }
}
