package de.cacheoverflow.reactnativerustplugin.tasks;

import de.cacheoverflow.reactnativerustplugin.exception.CargoCompileException;
import de.cacheoverflow.reactnativerustplugin.rust.analyer.SourceFileAnalyzer;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CargoCompileTask extends DefaultTask {

    private final List<Path> moduleFolders = new ArrayList<>();
    private final RegularFileProperty cargoFile;
    private final DirectoryProperty ndkFolder;

    @Inject
    public CargoCompileTask(@NotNull final Project project) {
        this.setDescription("Compile all rust projects with cargo");
        final ObjectFactory objectFactory = project.getObjects();
        this.cargoFile = objectFactory.fileProperty();
        this.ndkFolder = objectFactory.directoryProperty();
    }

    @TaskAction
    public void performTask() {
        // Get binaries folder and validate it
        Path binariesFolder = this.ndkFolder.getAsFile().get().toPath()
                .resolve(String.format("toolchains/llvm/prebuilt/%s/bin", this.getSystemSpecificFolder()));
        if (!Files.exists(binariesFolder) || !Files.isDirectory(binariesFolder))
            throw new CargoCompileException("Binaries Folder in NDK doesn't exists!");

        // Analyze all rust projects
        this.getProject().getLogger().info("Analyzing all imported Rust modules (Analyzer Pass)");
        SourceFileAnalyzer sourceFileAnalyzer = new SourceFileAnalyzer(this.getLogger());
        for (Path moduleFolder : this.moduleFolders) {
            sourceFileAnalyzer.analyzeDirectory(moduleFolder);
        }
        sourceFileAnalyzer.reformatTypes();
    }

    private String getSystemSpecificFolder() {
        return String.format("%s-%s", System.getProperty("os.name").toLowerCase(),
                System.getProperty("os.arch").replace("amd64", "x86_64"));
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
