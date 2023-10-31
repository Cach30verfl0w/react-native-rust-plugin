package de.cacheoverflow.rustnativerustplugin.exception;

import org.jetbrains.annotations.NotNull;

public class AnalyzerValidationException extends AnalyzerException {

    public AnalyzerValidationException(@NotNull String message, @NotNull Object... arguments) {
        super(message, arguments);
    }

}
