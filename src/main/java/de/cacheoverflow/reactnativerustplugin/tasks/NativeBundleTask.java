package de.cacheoverflow.reactnativerustplugin.tasks;

import de.cacheoverflow.reactnativerustplugin.utils.EnumAndroidTarget;
import de.cacheoverflow.reactnativerustplugin.utils.PathHelper;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class NativeBundleTask extends DefaultTask {

    private final List<Path> moduleFolders = new ArrayList<>();

    @TaskAction
    public void performTask() {
        final Path mainSourceSet = this.getProject().getProjectDir().toPath().resolve("src/main");
        if (!Files.exists(mainSourceSet) || !Files.isDirectory(mainSourceSet))
            throw new GradleException(String.format("Directory '%s' doesn't exists", mainSourceSet.toAbsolutePath()));

        final Path mainSourceSetJniLibs = mainSourceSet.resolve("jniLibs");
        PathHelper.createDirectoryIfNotExists(this.getProject(), mainSourceSetJniLibs);

        for (final Path moduleFolder : this.moduleFolders) {
            for (final EnumAndroidTarget target : EnumAndroidTarget.values()) {
                final Path targetLibraries = mainSourceSetJniLibs.resolve(target.getArchitecture());
                PathHelper.createDirectoryIfNotExists(this.getProject(), targetLibraries);
                final String fileName = moduleFolder.getFileName().toString().replace("-", "_");

                // Check if module library file does exists
                final Path rustLibraryFile = moduleFolder.resolve(String.format("target/%s/debug", target.getTargetTriple()))
                        .resolve(String.format("lib%s.so", fileName));
                if (!Files.exists(rustLibraryFile) || !Files.isRegularFile(rustLibraryFile))
                    throw new GradleException(String.format("Unable to find file '%s': Add crate-type = " +
                            "[\"staticlib\", \"cdylib\"] and name attribute to your Cargo.toml", rustLibraryFile.toAbsolutePath()));

                // Get path on Android project side
                final Path javaLibraryFile = targetLibraries.resolve(String.format("lib%s.so", fileName));
                PathHelper.createFileIfNotExists(this.getProject(), javaLibraryFile);

                try {
                    Files.copy(rustLibraryFile, javaLibraryFile, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    throw new GradleException("Unable to copy the rust library file to the java library file path", ex);
                }
            }
        }
    }

    @Input
    public @NotNull List<Path> getModuleFolders() {
        return this.moduleFolders;
    }
}
