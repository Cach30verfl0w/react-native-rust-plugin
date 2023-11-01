package de.cacheoverflow.reactnativerustplugin.tasks;

import de.cacheoverflow.reactnativerustplugin.codegen.TypeMapper;
import de.cacheoverflow.reactnativerustplugin.rust.analyer.SourceFileAnalyzer;
import de.cacheoverflow.reactnativerustplugin.rust.analyer.data.RustFile;
import de.cacheoverflow.reactnativerustplugin.rust.analyer.data.RustProject;
import de.cacheoverflow.reactnativerustplugin.rust.analyer.data.RustStruct;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JavaCodeGenTask extends DefaultTask {

    private final List<Path> moduleFolders = new ArrayList<>();

    @Inject
    public JavaCodeGenTask(@NotNull final Project project) {
        final ObjectFactory objectFactory = project.getObjects();
    }

    @TaskAction
    public void performTask() {
        // Analyze all rust projects
        this.getLogger().info("Analyzing all imported Rust modules (Analyzer Pass)");
        final SourceFileAnalyzer sourceFileAnalyzer = new SourceFileAnalyzer(this.getLogger());
        for (Path moduleFolder : this.moduleFolders) {
            sourceFileAnalyzer.analyzeProject(moduleFolder);
        }
        sourceFileAnalyzer.renameStructs();
        sourceFileAnalyzer.reformatTypes();

        this.getLogger().info("Generate Type Mappings");
        final TypeMapper typeMapper = new TypeMapper();
        for (RustProject project : sourceFileAnalyzer.getProjects()) {
            final var structs = project.files().stream().map(RustFile::structs).flatMap(Collection::stream).toList();
            for (RustStruct struct : structs) {
                String structPath = String.format("%s::%s", project.projectName().replace("-", "_"), struct.name());
                typeMapper.registerIfNotExists(structPath, "" /* TODO: Java Class from Attribute */);
                this.getLogger().info("Mapped {} to {}", struct, "" /* TODO: Java Class from Attribute */);
            }
        }

        this.getLogger().info("Generate Java Structures from Rust structures");
    }

    @Input
    public List<Path> getModuleFolders() {
        return this.moduleFolders;
    }

}
