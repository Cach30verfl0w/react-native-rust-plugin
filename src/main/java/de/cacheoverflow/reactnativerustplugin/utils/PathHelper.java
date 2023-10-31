package de.cacheoverflow.reactnativerustplugin.utils;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
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

    public static void createDirectoryIfNotExists(@NotNull final Project project, @NotNull final Path path) {
        if (Files.exists(path))
            return;

        project.getLogger().info("Unable to find '{}', creating it...", path.toAbsolutePath());
        try {
            Files.createDirectory(path);
        } catch (IOException ex) {
            throw new GradleException(String.format("Unable to create folder '%s'", path.toAbsolutePath()), ex);
        }
    }

    public static void createFileIfNotExists(@NotNull final Project project, @NotNull final Path path) {
        if (Files.exists(path) || !Files.isRegularFile(path))
            return;

        project.getLogger().info("Unable to found '{}', creating it...", path.toAbsolutePath());
        try {
            Files.createFile(path);
        } catch (IOException ex) {
            throw new GradleException(String.format("Unable to create file '%s'", path.toAbsolutePath()), ex);
        }
    }

}
