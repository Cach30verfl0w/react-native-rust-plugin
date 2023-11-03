package de.cacheoverflow.reactnativerustplugin.exception;

import org.jetbrains.annotations.NotNull;

public class CodeGenerationException extends RuntimeException {

    public CodeGenerationException(@NotNull final String message, @NotNull final Object... arguments) {
        super(String.format(message, arguments));
    }

}
