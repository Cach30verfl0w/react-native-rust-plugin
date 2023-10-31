package de.cacheoverflow.reactnativerustplugin.utils;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

public class PathHelper {

    private PathHelper() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull Optional<Path> findFileInPath(@NotNull final String file) {
        return Arrays.stream(System.getenv("PATH").split(":"))
                .map(path -> Paths.get(path).resolve(file))
                .filter(path -> Files.exists(path) && Files.isRegularFile(path))
                .findFirst();
    }

}
