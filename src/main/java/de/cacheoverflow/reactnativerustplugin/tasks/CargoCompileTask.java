package de.cacheoverflow.reactnativerustplugin.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

public class CargoCompileTask extends DefaultTask {

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

    }

    public @NotNull RegularFileProperty getCargoFile() {
        return this.cargoFile;
    }

    public @NotNull DirectoryProperty getNdkFolder() {
        return this.ndkFolder;
    }

}
