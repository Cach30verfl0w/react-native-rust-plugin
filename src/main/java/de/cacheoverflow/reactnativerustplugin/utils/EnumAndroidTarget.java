package de.cacheoverflow.reactnativerustplugin.utils;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public enum EnumAndroidTarget {

    AARCH64("aarch64-linux-android",   "arm64-v8a"),
    //ARMV7  ("armv7-linux-androideabi", "armeabi-v7a"),
    X86_64 ("x86_64-linux-android",    "x86_64");

    private final String targetTriple;
    private final String architecture;
    private final Function<Byte, String> linkerFunction;

    EnumAndroidTarget(@NotNull final String targetTriple, @NotNull final String architecture) {
        this.targetTriple = targetTriple;
        this.architecture = architecture;
        this.linkerFunction = version -> String.format("%s%s-clang", targetTriple, version);
    }

    public @NotNull Function<Byte, String> getLinkerFunction() {
        return this.linkerFunction;
    }

    public @NotNull String getArchitecture() {
        return this.architecture;
    }

    public @NotNull String getTargetTriple() {
        return this.targetTriple;
    }

}
