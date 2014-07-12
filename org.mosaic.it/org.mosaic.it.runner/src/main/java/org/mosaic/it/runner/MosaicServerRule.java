package org.mosaic.it.runner;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mosaic.core.components.Inject;
import org.mosaic.core.launcher.impl.ServerImpl;
import org.mosaic.core.modules.Module;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.management.ManagementFactory.getRuntimeMXBean;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * @author arik
 */
class MosaicServerRule implements MethodRule
{
    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger( MosaicServerRule.class );

    @Override
    public Statement apply( @Nonnull Statement base, @Nonnull FrameworkMethod method, @Nonnull Object target )
    {
        WithServer withServerAnn = method.getAnnotation( WithServer.class );
        return withServerAnn != null ? new ServerStatement( base, method, withServerAnn, target ) : base;
    }

    private class ServerStatement extends Statement
    {
        @Nonnull
        private final Statement base;

        @Nonnull
        private final FrameworkMethod method;

        @Nonnull
        private final List<Path> modulePaths;

        @Nonnull
        private final Object instance;

        private ServerStatement( @Nonnull Statement base,
                                 @Nonnull FrameworkMethod method,
                                 @Nonnull WithServer withServerAnn,
                                 @Nonnull Object target )
        {
            this.base = base;
            this.method = method;
            this.modulePaths = stream( withServerAnn.modules() ).map( this::findModule ).collect( toList() );
            this.instance = target;
        }

        @Override
        public void evaluate() throws Throwable
        {
            ServerImpl server = createServer( createTempHome() );
            try
            {
                LOG.info( "Starting server {}", server );
                server.start();
            }
            catch( Throwable e )
            {
                deletePath( server.getHome() );
                throw e;
            }

            try
            {
                deployModules( server );
                injectServer( server );
                this.base.evaluate();
            }
            finally
            {
                LOG.info( "Stopping server {}", server );
                server.stop();
                injectServer( null );
                deletePath( server.getHome() );
            }
        }

        @Nonnull
        private Path findModule( @Nonnull String name )
        {
            Stream<String> classPathStream;
            ClassLoader classLoader = getClass().getClassLoader();
            if( classLoader instanceof URLClassLoader )
            {
                classPathStream = stream( ( ( URLClassLoader ) classLoader ).getURLs() ).map( URL::getPath );
            }
            else
            {
                classPathStream = stream( getRuntimeMXBean().getClassPath().split( File.pathSeparator ) );
            }

            String modulePathName = name.matches( "it\\d+" ) ? "org.mosaic.it.modules." + name : name;
            return classPathStream
                    .filter( path -> path.endsWith( modulePathName + ".jar" ) || path.contains( "/" + modulePathName + "/" ) )
                    .findFirst()
                    .map( Paths::get )
                    .orElseThrow( () -> new IllegalStateException( "could not find module '" + name + "'" ) );
        }

        @Nonnull
        private Path createTempHome()
        {
            String testName = this.method.getMethod().getDeclaringClass().getName() + "." + this.method.getName();
            try
            {
                Path home = Files.createTempDirectory( String.format( "org.mosaic.it-%s", testName ) );
                Files.createDirectories( home );
                Files.createDirectories( home.resolve( "bin" ) );
                Files.createDirectories( home.resolve( "apps" ) );
                Files.createDirectories( home.resolve( "etc" ) );
                Files.createDirectories( home.resolve( "lib" ) );
                Files.createDirectories( home.resolve( "logs" ) );
                Files.createDirectories( home.resolve( "schemas" ) );
                Files.createDirectories( home.resolve( "work" ) );
                return home;
            }
            catch( Throwable e )
            {
                throw new IllegalStateException( "could not create Mosaic home directory for test " + testName, e );
            }
        }

        @Nonnull
        private ServerImpl createServer( @Nonnull Path home )
        {
            LOG.info( "Creating server at: {}", home );

            Properties properties = new Properties();
            properties.setProperty( "org.mosaic.home", home.toString() );
            try
            {
                return new ServerImpl( properties );
            }
            catch( Throwable e )
            {
                throw new IllegalStateException( "could not start server", e );
            }
        }

        private void deployModules( ServerImpl server ) throws IOException
        {
            this.modulePaths.stream().map( path -> server.getModuleManager().installModule( path ) ).forEach( Module::start );
        }

        private void injectServer( @Nullable ServerImpl server ) throws IllegalAccessException
        {
            Class<?> testClass = this.instance.getClass();
            while( testClass != null )
            {
                for( Field field : testClass.getDeclaredFields() )
                {
                    if( field.isAnnotationPresent( Inject.class ) && field.getType().isAssignableFrom( ServerImpl.class ) )
                    {
                        field.setAccessible( true );
                        field.set( this.instance, server );
                    }
                }
                testClass = testClass.getSuperclass();
            }
        }

        private void deletePath( Path home ) throws IOException
        {
            Files.walkFileTree( home, new SimpleFileVisitor<Path>()
            {
                @Nonnull
                @Override
                public FileVisitResult visitFile( @Nonnull Path file, @Nonnull BasicFileAttributes attrs )
                        throws IOException
                {
                    Files.delete( file );
                    return FileVisitResult.CONTINUE;
                }

                @Nonnull
                @Override
                public FileVisitResult postVisitDirectory( @Nonnull Path dir, IOException exc ) throws IOException
                {
                    Files.delete( dir );
                    return FileVisitResult.CONTINUE;
                }
            } );
        }
    }
}
