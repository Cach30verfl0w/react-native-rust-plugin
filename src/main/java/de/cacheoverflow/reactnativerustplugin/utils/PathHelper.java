package de.cacheoverflow.reactnativerustplugin.utils;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
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

    public static void writeFile(@NotNull final Path path, @NotNull String content) {
        try {
            Files.writeString(path, content);
        } catch (IOException ex) {
            throw new GradleException(String.format("Unable to write file '%s' with %s bytes content",
                    path.toAbsolutePath(), content.getBytes(StandardCharsets.UTF_8).length));
        }
    }

    public static void deleteDirectory(@NotNull final Project project, @NotNull final Path path) {
        if (!Files.exists(path))
            return;

        project.getLogger().info("Directory '{}' exists, deleting it...", path.toAbsolutePath());
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes ignored) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException ignored) throws IOException {
                    Files.delete(directory);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            throw new GradleException(String.format("Unable to delete folder '%s'", path.toAbsolutePath()), ex);
        }
    }

    public static void createDirectoryIfNotExists(@NotNull final Project project, @NotNull final Path path) {
        if (Files.exists(path))
            return;

        project.getLogger().info("Unable to find '{}', creating it...", path.toAbsolutePath());
        try {
            if (path.getParent() != null) {
                PathHelper.createDirectoryIfNotExists(project, path.getParent());
            }

            Files.createDirectory(path);
        } catch (IOException ex) {
            throw new GradleException(String.format("Unable to create folder '%s'", path.toAbsolutePath()), ex);
        }
    }

    public static void createFileIfNotExists(@NotNull final Project project, @NotNull final Path path) {
        if (Files.exists(path))
            return;

        project.getLogger().info("Unable to found '{}', creating it...", path.toAbsolutePath());
        try {
            Files.createFile(path);
        } catch (IOException ex) {
            throw new GradleException(String.format("Unable to create file '%s'", path.toAbsolutePath()), ex);
        }
    }

}
