package de.cacheoverflow.rustnativerustplugin.rust.analyer;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record RustFile(@NotNull String path, @NotNull List<RustFunction> functions, @NotNull List<RustStruct> structs,
                       @NotNull List<String> imports) {}
