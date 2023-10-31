package de.cacheoverflow.reactnativerustplugin.tasks;

import de.cacheoverflow.reactnativerustplugin.exception.CargoCompileException;
import de.cacheoverflow.reactnativerustplugin.rust.analyer.SourceFileAnalyzer;
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
        Path binariesFolder = this.ndkFolder.getAsFile().get().toPath()
                .resolve(String.format("toolchains/llvm/prebuilt/%s/bin", this.getSystemSpecificFolder()));
        if (!Files.exists(binariesFolder) || !Files.isDirectory(binariesFolder))
            throw new CargoCompileException("Binaries Folder in NDK doesn't exists!");

        // Analyze all rust projects
        this.getLogger().info("Analyzing all imported Rust modules (Analyzer Pass)");
        SourceFileAnalyzer sourceFileAnalyzer = new SourceFileAnalyzer(this.getLogger());
        for (Path moduleFolder : this.moduleFolders) {
            sourceFileAnalyzer.analyzeDirectory(moduleFolder);
        }
        sourceFileAnalyzer.reformatTypes();

        // Build all projects
        this.getLogger().info("Building all imported Rust modules (Build Pass)");
        for (Path moduleFolder : this.moduleFolders) {
            // Generate variables
            String currentTarget = "x86_64-linux-android";
            String linkerConfig = String.format("target.%s.linker=\"%s/%s%s-clang\"", currentTarget, binariesFolder.toAbsolutePath(),
                    currentTarget, this.androidApiVersion.get());

            // Generate process command string
            StringBuilder commandBuilder = new StringBuilder(this.cargoFile.get().getAsFile().getAbsolutePath())
                    .append(" build");
            commandBuilder.append(" --config ").append(linkerConfig);
            commandBuilder.append(" --target ").append(currentTarget);

            // Generate Command Builder
            this.getLogger().info("Running command '{}' in '{}'", commandBuilder, moduleFolder.toFile());
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(commandBuilder.toString().split(" "));
            processBuilder.directory(moduleFolder.toFile());
            processBuilder.redirectErrorStream(true);

            try {
                Process process = processBuilder.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new CargoCompileException("Cargo Build Process for '%s' exited with exit code %s\n%s",
                            moduleFolder.getFileName().toString(), exitCode, reader.lines().collect(Collectors.joining("\n")));

                }
            } catch (IOException | InterruptedException ex) {
                throw new CargoCompileException(ex);
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
