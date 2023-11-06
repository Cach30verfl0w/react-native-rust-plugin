package de.cacheoverflow.reactnativerustplugin.utils;

import org.jetbrains.annotations.NotNull;

public class StringHelper {

    private StringHelper() {
        throw new UnsupportedOperationException();
    }

    public static void repeat(@NotNull final StringBuilder builder, @NotNull final String string, final int count) {
        for (int i = 0; i < count; i++) {
            builder.append(string);
        }
    }

}
