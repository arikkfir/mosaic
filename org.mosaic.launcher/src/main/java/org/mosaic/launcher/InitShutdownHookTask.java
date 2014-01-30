package org.mosaic.launcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
final class InitShutdownHookTask extends InitTask
{
    /**
     * The port on which the shutdown and restart requests are sent. If changing this value, remember to change it also
     * in ServerImpl.
     */
    private static final int SHUTDOWN_PORT = 38631;

    @Nonnull
    private final MosaicShutdownHook shutdownHook = new MosaicShutdownHook();

    @Nullable
    private ServerSocket serverSocket;

    @Override
    public void start()
    {
        this.log.debug( "Installing shutdown hooks" );

        this.log.debug( "Installing JVM shutdown hook" );
        Runtime.getRuntime().addShutdownHook( this.shutdownHook );

        this.log.debug( "Starting shutdown listener" );
        try
        {
            this.serverSocket = new ServerSocket( SHUTDOWN_PORT, 5, InetAddress.getLoopbackAddress() );
            new Thread( new ShutdownController(), "MosaicShutdownController" ).start();
        }
        catch( IOException e )
        {
            throw SystemError.bootstrapError( "Could not create shutdown socket: {}", e.getMessage(), e );
        }
    }

    @Override
    public void stop()
    {
        this.log.info( "Removing shutdown hooks" );

        this.log.debug( "Stopping shutdown listener..." );
        if( this.serverSocket != null )
        {
            try
            {
                this.serverSocket.close();
            }
            catch( IOException ignore )
            {
            }
        }
        this.serverSocket = null;

        this.log.debug( "Uninstalling JVM shutdown hook..." );
        try
        {
            Runtime.getRuntime().removeShutdownHook( this.shutdownHook );
        }
        catch( IllegalStateException ignore )
        {
        }
    }

    private class MosaicShutdownHook extends Thread
    {
        public MosaicShutdownHook()
        {
            super( "Mosaic Shutdown Hook" );
        }

        @Override
        public void run()
        {
            try
            {
                Mosaic.stop();
            }
            catch( Exception e )
            {
                InitShutdownHookTask.this.log.warn( e.getMessage(), e );
            }
        }
    }

    private class ShutdownController implements Runnable
    {
        @Override
        public void run()
        {
            while( true )
            {
                ServerSocket serverSocket = InitShutdownHookTask.this.serverSocket;
                if( serverSocket == null || serverSocket.isClosed() )
                {
                    break;
                }

                final Socket clientSocket;
                try
                {
                    clientSocket = serverSocket.accept();
                }
                catch( SocketException e )
                {
                    InitShutdownHookTask.this.log.debug( "Mosaic shutdown listener closed" );
                    break;
                }
                catch( IOException e )
                {
                    InitShutdownHookTask.this.log.error( "Could not listen for shutdown requests on port {}: {}", SHUTDOWN_PORT, e.getMessage(), e );
                    break;
                }

                new Thread( "ShutdownRequestHandler" )
                {
                    @Override
                    public void run()
                    {
                        try( BufferedReader reader = new BufferedReader( new InputStreamReader( clientSocket.getInputStream(), "UTF-8" ) ) )
                        {
                            String instruction = reader.readLine();
                            switch( instruction )
                            {
                                case "SHUTDOWN":
                                    Mosaic.stop();
                                    break;
                                case "RESTART":
                                    Mosaic.stop();
                                    Mosaic.start();
                                    break;
                            }
                        }
                        catch( IOException e )
                        {
                            InitShutdownHookTask.this.log.error( "Error while processing request to shutdown listener: {}", e.getMessage(), e );
                        }
                    }
                }.start();
            }
        }
    }
}
