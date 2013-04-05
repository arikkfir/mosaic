package org.mosaic.shell;

import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface Options
{
    void printHelp() throws IOException;

    void printUsage() throws CommandDefinitionException;

    boolean has( @Nonnull String option );

    @Nullable
    String get( @Nonnull String option );

    @Nonnull
    String get( @Nonnull String option, @Nonnull String defaultValue );

    @Nullable
    <T> T get( @Nonnull String option, @Nonnull TypeToken<T> typeToken );

    @Nonnull
    <T> T get( @Nonnull String option, @Nonnull String defaultValue, @Nonnull TypeToken<T> typeToken );

    @Nonnull
    Collection<String> getAll( @Nonnull String option );

    @Nonnull
    <T> Collection<T> getAll( @Nonnull String option, @Nonnull TypeToken<T> typeToken );

    @Nonnull
    String require( @Nonnull String option ) throws RequiredOptionMissingException;

    @Nonnull
    <T> T require( @Nonnull String option, @Nonnull TypeToken<T> typeToken ) throws RequiredOptionMissingException;

    @Nonnull
    List<String> getExtraArguments();
}
