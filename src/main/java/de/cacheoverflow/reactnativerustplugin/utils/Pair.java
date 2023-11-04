package de.cacheoverflow.reactnativerustplugin.utils;

import org.jetbrains.annotations.NotNull;

public class Pair<A, B> {

    private final A first;
    private final B second;

    public Pair(@NotNull final A first, @NotNull final B second) {
        this.first = first;
        this.second = second;
    }

    public @NotNull A getFirst() {
        return this.first;
    }

    public @NotNull B getSecond() {
        return this.second;
    }
}
