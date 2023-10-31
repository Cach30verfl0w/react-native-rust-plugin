package de.cacheoverflow.rustnativerustplugin.exception;

import org.jetbrains.annotations.NotNull;

public class AnalyzerException extends RuntimeException {

    public AnalyzerException(@NotNull final String message, @NotNull final Object... arguments) {
        super(String.format(message, arguments));
    }

    public AnalyzerException(@NotNull final Throwable cause) {
        super(cause);
    }
}
