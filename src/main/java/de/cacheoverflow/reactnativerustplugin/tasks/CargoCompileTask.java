package de.cacheoverflow.reactnativerustplugin.tasks;

import de.cacheoverflow.reactnativerustplugin.exception.CargoCompileException;
import de.cacheoverflow.reactnativerustplugin.utils.EnumAndroidTarget;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CargoCompileTask extends DefaultTask {

    // TODO: Release mode

    private final List<Path> moduleFolders = new ArrayList<>();
    private final Property<Byte> androidApiVersion;
    private final RegularFileProperty cargoFile;
    private final DirectoryProperty ndkFolder;

    @Inject
    public CargoCompileTask(@NotNull final Project project) {
        this.setDescription("Compile all rust projects with cargo");
        final ObjectFactory objectFactory = project.getObjects();
        this.cargoFile = objectFactory.fileProperty();
        this.ndkFolder = objectFactory.directoryProperty();
        this.androidApiVersion = objectFactory.property(Byte.class);
    }

    @TaskAction
    public void performTask() {
        // Get binaries folder and validate it
        final Path binariesFolder = this.ndkFolder.getAsFile().get().toPath()
                .resolve(String.format("toolchains/llvm/prebuilt/%s/bin", this.getSystemSpecificFolder()));
        if (!Files.exists(binariesFolder) || !Files.isDirectory(binariesFolder))
            throw new CargoCompileException("Binaries Folder in NDK doesn't exists!");

        // Build all projects
        this.getLogger().info("Building all imported Rust modules (Build Pass)");
        for (final Path moduleFolder : this.moduleFolders) {
            for (final EnumAndroidTarget androidTarget : EnumAndroidTarget.values()) {
                this.getLogger().info("Building '{}' for '{}'", moduleFolder.getFileName().toString(),
                        androidTarget.getTargetTriple());

                // Generate command string
                final String commandBuilder = this.cargoFile.get().getAsFile().getAbsolutePath() + " build --target " +
                        androidTarget.getTargetTriple() + " --config " + String.format("target.%s.linker=\"%s\"",
                        androidTarget.getTargetTriple(), binariesFolder.resolve(androidTarget.getLinkerFunction()
                                .apply(this.androidApiVersion.get())).toAbsolutePath());

                // Generate process builder
                final ProcessBuilder processBuilder = new ProcessBuilder(commandBuilder.split(" "));
                processBuilder.directory(moduleFolder.toFile());

                // Execute command
                try {
                    final Process process = processBuilder.start();
                    final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    final int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        throw new CargoCompileException("Cargo Build Process for '%s' exited with exit code %s\n%s",
                                moduleFolder.getFileName().toString(), exitCode, reader.lines().collect(Collectors.joining("\n")));

                    }
                } catch (IOException | InterruptedException ex) {
                    throw new CargoCompileException(ex);
                }
            }
        }
    }

    private String getSystemSpecificFolder() {
        return String.format("%s-%s", System.getProperty("os.name").toLowerCase(),
                System.getProperty("os.arch").replace("amd64", "x86_64"));
    }


    @Input
    public @NotNull Property<Byte> getAndroidApiVersion() {
        return this.androidApiVersion;
    }

    @InputFile
    public @NotNull RegularFileProperty getCargoFile() {
        return this.cargoFile;
    }

    @InputDirectory
    public @NotNull DirectoryProperty getNdkFolder() {
        return this.ndkFolder;
    }

    @Input
    public @NotNull List<Path> getModuleFolders() {
        return this.moduleFolders;
    }

}
