package de.cacheoverflow.reactnativerustplugin.rust.analyer.data;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record RustAttribute(@NotNull String name, @NotNull Map<String, String> parameters) { }
