package org.mosaic.runner;

import org.apache.felix.framework.Felix;
import org.mosaic.runner.boot.FrameworkBootstrapper;
import org.osgi.framework.FrameworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.currentTimeMillis;

/**
 * @author arik
 */
public class Runner
{
    private final Logger logger = LoggerFactory.getLogger( Runner.class );

    private final ServerHome home;

    public Runner( ServerHome home )
    {
        this.home = home;
    }

    public ExitCode run( ) throws SystemExitException
    {
        long start = currentTimeMillis( );

        // print home configuration
        this.logger.info( "******************************************************************************************" );
        this.logger.info( " Starting Mosaic server:" );
        this.logger.info( "    Home:    {}", this.home.getHome( ) );
        this.logger.info( "    Boot:    {}", this.home.getBoot( ) );
        this.logger.info( "    Config:  {}", this.home.getEtc( ) );
        this.logger.info( "    Work:    {}", this.home.getWork( ) );
        this.logger.info( "******************************************************************************************" );
        this.logger.info( " " );

        // create and start the server
        FrameworkBootstrapper bootstrapper = new FrameworkBootstrapper( this.home );
        Felix felix = bootstrapper.boot( );

        // print summary and wait for the server to shutdown
        long startupDurationMillis = currentTimeMillis( ) - start;
        synchronized( Runner.class )
        {
            this.logger.info( " " );
            this.logger.info( "*************************************************************************" );
            this.logger.info( " Server is running (initialized in {} seconds, or {} milli-seconds)",
                              startupDurationMillis /
                              1000, startupDurationMillis );
            this.logger.info( "*************************************************************************" );
            this.logger.info( " " );
        }

        // wait until server stops
        return waitForOsgiContainerToStop( felix );
    }

    private ExitCode waitForOsgiContainerToStop( Felix felix )
    {
        while( true )
        {
            try
            {
                FrameworkEvent event = felix.waitForStop( 1000 * 60 );
                switch( event.getType( ) )
                {
                    case FrameworkEvent.STOPPED:

                        // framework stopped normally
                        this.logger.info( "Mosaic has been stopped" );
                        return ExitCode.SUCCESS;

                    case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED:

                        // boot class-path has changed which requires a JVM restart (exit-code accordingly,
                        // shell script should pick this up and restart us)
                        this.logger.info( "Mosaic boot class-path has been modified, restarting JVM" );
                        return ExitCode.RESTART;

                    case FrameworkEvent.ERROR:

                        // framework stopped abnormally, return error exit code
                        this.logger.info( "Mosaic has been stopped due to an error" );
                        return ExitCode.RUNTIME_ERROR;

                    default:

                        // framework stopped abnormally, with an unspecified reason, return an error exit code
                        this.logger.info( "Mosaic has been stopped due to an unknown cause (" +
                                          event.getType( ) +
                                          ")" );
                        return ExitCode.RUNTIME_ERROR;

                    case FrameworkEvent.STOPPED_UPDATE:

                        // framework is restart in the same JVM - just log and continue
                        this.logger.info( "Mosaic system has been updated and will now restart (same JVM)" );
                        continue;

                    case FrameworkEvent.WAIT_TIMEDOUT:
                        // no-op: framework is still running - do nothing and keep looping+waiting
                        // (do not remove this or the 'switch' case will go to 'default' which will stop the JVM)
                }

            }
            catch( InterruptedException e )
            {
                this.logger.warn( "Mosaic has been interrupted - exiting", e );
                return ExitCode.INTERRUPTED;

            }
        }
    }
}
