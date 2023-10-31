package de.cacheoverflow.reactnativerustplugin.exception;

import org.jetbrains.annotations.NotNull;

public class AnalyzerProjectException extends AnalyzerException {

    public AnalyzerProjectException(@NotNull String message, @NotNull Object... arguments) {
        super(message, arguments);
    }

}
