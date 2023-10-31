package de.cacheoverflow.reactnativerustplugin.rust.analyer.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record RustFile(@NotNull String path, @NotNull List<RustFunction> functions, @NotNull List<RustStruct> structs,
                       @NotNull List<String> imports) {}
