package de.cacheoverflow.reactnativerustplugin.rust.analyer.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public record RustStruct(@NotNull List<String> attributes, @NotNull String name,
                         @NotNull Map<String, String> parameters) { }
