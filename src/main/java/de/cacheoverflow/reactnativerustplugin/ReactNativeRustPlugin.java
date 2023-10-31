package de.cacheoverflow.reactnativerustplugin;

import de.cacheoverflow.reactnativerustplugin.extension.PluginBaseExtension;
import de.cacheoverflow.reactnativerustplugin.tasks.CargoCompileTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.tasks.TaskContainer;
import org.jetbrains.annotations.NotNull;

public class ReactNativeRustPlugin implements Plugin<Project> {

    public static final String TASK_GROUP = "react-native-rust";

    @Override
    public void apply(@NotNull final Project project) {
        // Extensions
        final ExtensionContainer extensionContainer = project.getExtensions();
        final PluginBaseExtension extension = extensionContainer.create("react_native_rust", PluginBaseExtension.class);

        // Tasks
        final TaskContainer taskContainer = project.getTasks();
        taskContainer.register("cargoCompile", CargoCompileTask.class, task -> {
            task.setGroup(ReactNativeRustPlugin.TASK_GROUP);
            task.getCargoFile().set(extension.getCargoFile());
            task.getNdkFolder().set(extension.getNdkFolder());
        });
    }

}
