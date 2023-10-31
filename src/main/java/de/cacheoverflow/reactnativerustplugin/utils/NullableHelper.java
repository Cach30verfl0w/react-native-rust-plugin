package de.cacheoverflow.reactnativerustplugin.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

public class NullableHelper {

    private NullableHelper() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull <T> Optional<T> successOrElse(@NotNull final Supplier<T> operation,
                                                         @NotNull final Class<? extends Exception> exceptionClass,
                                                         @Nullable final T defaultValue) {
        try {
            return Optional.ofNullable(operation.get());
        } catch (Exception exception) {
            if (exception.getClass().equals(exceptionClass)) {
                return Optional.ofNullable(defaultValue);
            }
            throw exception;
        }
    }

}
