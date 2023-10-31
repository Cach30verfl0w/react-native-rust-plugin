package de.cacheoverflow.reactnativerustplugin.utils;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public enum EnumAndroidTarget {

    AARCH64("aarch64-linux-android"),
    ARMV7  ("armv7-linux-androideabi"),
    X86_64 ("x86_64-linux-android");

    private final String targetTriple;
    private final Function<Byte, String> linkerFunction;

    EnumAndroidTarget(@NotNull final String targetTriple) {
        this.targetTriple = targetTriple;
        this.linkerFunction = version -> String.format("%s%s-clang", targetTriple, version);
    }

    public @NotNull Function<Byte, String> getLinkerFunction() {
        return this.linkerFunction;
    }

    public @NotNull String getTargetTriple() {
        return this.targetTriple;
    }

}
