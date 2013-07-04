package org.mosaic.shell.impl.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jline.console.ConsoleReader;
import org.apache.sshd.server.Environment;

/**
 * @author arik
 */
public class MosaicConsoleReader extends ConsoleReader
{
    private static class PipeInputStream extends InputStream
    {
        private final BlockingQueue<Integer> inputQueue;

        private PipeInputStream( BlockingQueue<Integer> inputQueue )
        {
            this.inputQueue = inputQueue;
        }

        @Override
        public int read() throws IOException
        {
            try
            {
                return this.inputQueue.take();
            }
            catch( InterruptedException e )
            {
                return -1;
            }
        }
    }

    public MosaicConsoleReader( @Nonnull InputStream in,
                                @Nonnull OutputStream out,
                                @Nonnull Environment env,
                                @Nonnull jline.console.history.History history,
                                @Nullable String username,
                                @Nonnull MosaicCommandCompleter commandCompleter ) throws IOException
    {
        super( "Mosaic", in, out, new SessionTerminal( env ) );
        setHistory( history );
        addCompleter( commandCompleter );
        setPrompt( "[" + username + "@" + InetAddress.getLocalHost().getHostName() + "]$ " );
    }
}
