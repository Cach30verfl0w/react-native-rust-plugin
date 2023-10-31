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
import java.util.Optional;

public class PluginBaseExtension {

    private final Property<String> basePackage;
    private final RegularFileProperty cargoFile;
    private final DirectoryProperty ndkFolder;

    @Inject
    public PluginBaseExtension(@NotNull final Project project) {
        final ObjectFactory objectFactory = project.getObjects();
        this.basePackage = objectFactory.property(String.class);
        this.ndkFolder = objectFactory.directoryProperty();
        this.cargoFile = objectFactory.fileProperty();

        Optional.ofNullable(System.getenv("NDK_HOME")).ifPresent(path -> this.ndkFolder.set(new File(path)));
        PathHelper.findFileInPath("cargo").ifPresent(path -> this.cargoFile.set(path.toFile()));
    }

    public @NotNull RegularFileProperty getCargoFile() {
        return this.cargoFile;
    }

    public @NotNull Property<String> getBasePackage() {
        return this.basePackage;
    }

    public @NotNull DirectoryProperty getNdkFolder() {
        return this.ndkFolder;
    }

}
