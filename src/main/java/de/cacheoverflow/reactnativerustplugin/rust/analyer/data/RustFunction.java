package de.cacheoverflow.reactnativerustplugin.rust.analyer.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record RustFunction(@NotNull List<RustAttribute> attributes, @NotNull String functionName,
                           @NotNull Map<String, String> parameters, @NotNull Optional<String> returnType) { }
