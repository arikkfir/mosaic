package org.mosaic.shell.impl.command.std;

import com.google.common.collect.ComparisonChain;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import javax.annotation.Nonnull;
import org.mosaic.lifecycle.Module;
import org.mosaic.lifecycle.ModuleManager;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.mosaic.shell.Console;
import org.mosaic.shell.annotation.Arguments;
import org.mosaic.shell.annotation.Command;

/**
 * @author arik
 */
@Bean
public class Metrics
{
    private final ThreadLocal<DecimalFormat> smallValueFormatHolder = new ThreadLocal<DecimalFormat>()
    {
        @Override
        protected DecimalFormat initialValue()
        {
            return new DecimalFormat( "000.000" );
        }
    };

    private final ThreadLocal<DecimalFormat> bigValueFormatHolder = new ThreadLocal<DecimalFormat>()
    {
        @Override
        protected DecimalFormat initialValue()
        {
            return new DecimalFormat( "000.000" );
        }
    };

    private ModuleManager moduleManager;

    @ServiceRef
    public void setModuleManager( ModuleManager moduleManager )
    {
        this.moduleManager = moduleManager;
    }

    @Command(name = "metrics", label = "List available metrics", desc = "Lists all available metrics, optionally filtered by name.")
    public void listMetrics( @Nonnull Console console, @Arguments String... filters ) throws IOException
    {
        List<Module.MetricsTimer> timers = new LinkedList<>();
        for( Module module : this.moduleManager.getModules() )
        {
            Module.Metrics metrics = module.getMetrics();
            if( metrics != null )
            {
                Collection<? extends Module.MetricsTimer> moduleTimers = metrics.getTimers();
                if( filters.length == 0 )
                {
                    timers.addAll( moduleTimers );
                }
                else
                {
                    for( String filter : filters )
                    {
                        for( Module.MetricsTimer timer : moduleTimers )
                        {
                            if( timer.getName().toLowerCase().contains( filter.toLowerCase() ) )
                            {
                                timers.add( timer );
                            }
                        }
                    }
                }
            }
        }

        Collections.sort( timers, new Comparator<Module.MetricsTimer>()
        {
            @Override
            public int compare( Module.MetricsTimer o1, Module.MetricsTimer o2 )
            {
                return ComparisonChain.start()
                                      .compare( o1.getModule().getName(), o2.getModule().getName() )
                                      .compare( o1.getName(), o2.getName() )
                                      .result();
            }
        } );

        Console.TableHeaders table = console.createTable();
        table.addHeader( "Module", 0.2 );
        table.addHeader( "Name" );
        table.addHeader( "Count", 5 );
        table.addHeader( "Min", 8 );
        table.addHeader( "Max", 8 );
        table.addHeader( "1-min", 8 );
        table.addHeader( "5-min", 8 );
        table.addHeader( "15-min", 8 );
        table.addHeader( "StdDev.", 8 );
        table.addHeader( "Sum", 8 );
        Console.TablePrinter printer = table.start();
        for( Module.MetricsTimer timer : timers )
        {
            printer.print( timer.getModule().getName(),
                           timer.getName(),
                           timer.count(),
                           formatSmall( timer.min() ),
                           formatSmall( timer.max() ),
                           formatSmall( timer.oneMinuteRate() ),
                           formatSmall( timer.fiveMinuteRate() ),
                           formatSmall( timer.fifteenMinuteRate() ),
                           formatSmall( timer.stdDev() ),
                           formatBig( timer.sum() ) );
        }
        printer.done();
    }

    private String formatBig( double value )
    {
        return this.bigValueFormatHolder.get().format( value );
    }

    private String formatSmall( double value )
    {
        return this.smallValueFormatHolder.get().format( value );
    }
}
