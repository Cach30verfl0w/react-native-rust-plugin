package de.cacheoverflow.reactnativerustplugin.extension;

import de.cacheoverflow.reactnativerustplugin.utils.PathHelper;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PluginBaseExtension {

    private final List<Path> moduleFolders = new ArrayList<>();
    private final Property<String> basePackage;
    private final Property<String> rustBaseFolder;
    private final Property<Byte> androidApiVersion;
    private final RegularFileProperty cargoFile;
    private final DirectoryProperty ndkFolder;
    private final Path projectFolder;

    @Inject
    public PluginBaseExtension(@NotNull final Project project) {
        final ObjectFactory objectFactory = project.getObjects();
        this.basePackage = objectFactory.property(String.class);
        this.rustBaseFolder = objectFactory.property(String.class);
        this.ndkFolder = objectFactory.directoryProperty();
        this.cargoFile = objectFactory.fileProperty();
        this.androidApiVersion = objectFactory.property(Byte.class);

        Optional.ofNullable(System.getenv("NDK_HOME")).ifPresent(path -> this.ndkFolder.set(new File(path)));
        PathHelper.findFileInPath("cargo").ifPresent(path -> this.cargoFile.set(path.toFile()));
        this.projectFolder = project.getProjectDir().toPath();
    }

    public void module(@NotNull final String path) {
        this.moduleFolders.add(this.projectFolder.resolve(this.rustBaseFolder.get()).resolve(path));
    }

    public @NotNull RegularFileProperty getCargoFile() {
        return this.cargoFile;
    }

    public @NotNull Property<Byte> getAndroidApiVersion() {
        return this.androidApiVersion;
    }

    public @NotNull Property<String> getRustBaseFolder() {
        return this.rustBaseFolder;
    }

    public @NotNull Property<String> getBasePackage() {
        return this.basePackage;
    }

    public @NotNull DirectoryProperty getNdkFolder() {
        return this.ndkFolder;
    }

    public @NotNull List<Path> getModuleFolders() {
        return this.moduleFolders;
    }

}
