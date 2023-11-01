package de.cacheoverflow.reactnativerustplugin.rust.analyer.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record RustProject(@NotNull String projectName, @NotNull List<RustFile> files, @NotNull List<String> crates) {
}
