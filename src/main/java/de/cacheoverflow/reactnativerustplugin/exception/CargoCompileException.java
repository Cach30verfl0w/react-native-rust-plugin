package de.cacheoverflow.reactnativerustplugin.exception;

import org.jetbrains.annotations.NotNull;

public class CargoCompileException extends RuntimeException {

    public CargoCompileException(@NotNull final String message, @NotNull final Object... arguments) {
        super(String.format(message, arguments));
    }

}
