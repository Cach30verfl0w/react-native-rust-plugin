package de.cacheoverflow.rustnativerustplugin.rust.analyer;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public record RustStruct(@NotNull List<String> attributes, @NotNull String functionName,
                         @NotNull Map<String, String> parameters) { }
