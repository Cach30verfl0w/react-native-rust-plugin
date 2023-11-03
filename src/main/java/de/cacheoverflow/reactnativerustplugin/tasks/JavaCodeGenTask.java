package de.cacheoverflow.reactnativerustplugin.tasks;

import de.cacheoverflow.reactnativerustplugin.codegen.ClassBuilder;
import de.cacheoverflow.reactnativerustplugin.codegen.Modifier;
import de.cacheoverflow.reactnativerustplugin.codegen.TypeMapper;
import de.cacheoverflow.reactnativerustplugin.exception.AnalyzerException;
import de.cacheoverflow.reactnativerustplugin.exception.CodeGenerationException;
import de.cacheoverflow.reactnativerustplugin.rust.analyer.SourceFileAnalyzer;
import de.cacheoverflow.reactnativerustplugin.rust.analyer.data.*;
import de.cacheoverflow.reactnativerustplugin.utils.PathHelper;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.*;

public class JavaCodeGenTask extends DefaultTask {

    public static final String JNI_EXPORT_ATTR_NAME = "jni_export";

    private final List<Path> moduleFolders = new ArrayList<>();
    private final Property<String> basePackage;

    @Inject
    public JavaCodeGenTask(@NotNull final Project project) {
        final ObjectFactory objectFactory = project.getObjects();
        this.basePackage = objectFactory.property(String.class);
    }

    @TaskAction
    public void performTask() {
        // Analyze all rust projects
        this.getLogger().info("Analyzing all imported Rust modules");
        final SourceFileAnalyzer sourceFileAnalyzer = new SourceFileAnalyzer(this.getLogger());
        for (final Path moduleFolder : this.moduleFolders) {
            sourceFileAnalyzer.analyzeProject(moduleFolder);
        }

        // Adjust names for following passes
        sourceFileAnalyzer.renameStructs();
        sourceFileAnalyzer.reformatTypes();

        // Generate Type Mapping
        this.getLogger().info("Generate Type Mappings");
        final TypeMapper typeMapper = new TypeMapper();
        for (final RustProject project : sourceFileAnalyzer.getProjects()) {
            final var structs = project.files().stream().map(RustFile::structs).flatMap(Collection::stream).toList();
            for (RustStruct struct : structs) {
                final String structPath = String.format("%s::%s", project.projectName().replace("-", "_"), struct.name());

                // Skip struct if no jni_import attribute found
                if (struct.attributes().stream().map(RustAttribute::name).noneMatch(name -> name
                        .equals(JavaCodeGenTask.JNI_EXPORT_ATTR_NAME))) {
                    this.getLogger().warn("Skipping {} because of missing jni_export attribute", structPath);
                    continue;
                }

                // Get name of java class/type
                final String javaName = struct.attributes().stream().filter(attr -> attr.name()
                                .equals(JavaCodeGenTask.JNI_EXPORT_ATTR_NAME))
                        .map(attr -> attr.parameters().get("class")).filter(Objects::nonNull).findFirst().orElse(null);
                if (javaName == null)
                    throw new AnalyzerException("Illegal jni_export attribute => Missing class name in definition");

                // Generate path mapping and inform user about it
                typeMapper.registerIfNotExists(structPath, javaName);
                this.getLogger().info("Mapped {} to {}", structPath, javaName);
            }
        }

        // Generate generated sources directory if needed
        this.getLogger().info("Generate if needed and cleanup if needed generated source directory");
        final Path generatedSourceFolder = this.getProject().getBuildDir().toPath().resolve("generated/source")
                .resolve("react-native-rust/main/java");
        PathHelper.createDirectoryIfNotExists(this.getProject(), generatedSourceFolder);

        final Path generatedSourcePackage = generatedSourceFolder.resolve(this.basePackage.get().replace(".", "/"))
                .resolve("generated");
        PathHelper.deleteDirectory(this.getProject(), generatedSourcePackage);
        PathHelper.createDirectoryIfNotExists(this.getProject(), generatedSourcePackage);

        // Generate Java classes from Struct structures
        this.getLogger().info("Generate Java classes from Rust structures");

        final Map<String, String> generatedClasses = new HashMap<>();
        for (final RustProject project : sourceFileAnalyzer.getProjects()) {
            // Enumerate all structs in project
            for (final RustStruct struct : project.files().stream().map(RustFile::structs).flatMap(Collection::stream)
                    .toList()) {

                // Filter not exported structures
                final RustAttribute exportAttribute = struct.attributes().stream()
                        .filter(attr -> attr.name().equals(JavaCodeGenTask.JNI_EXPORT_ATTR_NAME))
                        .findFirst().orElse(null);
                if (exportAttribute == null)
                    continue;

                // Avoid collisions in class generation
                final String className = exportAttribute.parameters().get("class").replace("\"", "");
                if (generatedClasses.containsKey(className))
                    throw new CodeGenerationException("Unable to generate class '%s' => Class was already created", className);

                // Generate class builder by struct
                final ClassBuilder classBuilder = new ClassBuilder(Modifier.PUBLIC | Modifier.FINAL, className, null, List.of());
                for (Map.Entry<String, String> field : struct.parameters().entrySet()) {
                    classBuilder.addField(Modifier.PUBLIC, field.getKey(), typeMapper.map(field.getValue()));
                }

                // Add generate class to generated classes map
                this.getLogger().info("Generated class '{}' from project '{}'", className, project.projectName());
                generatedClasses.put(className, classBuilder.build());
            }
        }

        // Inform the user about the generation and write generated classes to specified directories
        this.getLogger().info("Generated {} class as Wrapper for Rust structs", generatedClasses.size());

        for (final Map.Entry<String, String> classEntry : generatedClasses.entrySet()) {
            final Path classPath = generatedSourceFolder.resolve(String.format("%s.java", classEntry.getKey()
                    .replace(".", "/")));
            PathHelper.createDirectoryIfNotExists(this.getProject(), classPath.getParent());
            PathHelper.writeFile(classPath, classEntry.getValue());
            this.getLogger().info("Successfully wrote class '{}' into '{}'", classEntry.getKey(), classPath.toAbsolutePath());
        }

        // Generate package classes for mapping between Rust and Java functions
        this.getLogger().info("Generate Rust mapping classes as React Native modules");

        for (final RustProject project : sourceFileAnalyzer.getProjects()) {
            // Enumerate all functions in project
            for (final RustFunction function : project.files().stream().map(RustFile::functions)
                    .flatMap(Collection::stream).toList()) {

                // Filter not exported functions
                final RustAttribute exportAttribute = function.attributes().stream()
                        .filter(attr -> attr.name().equals(JavaCodeGenTask.JNI_EXPORT_ATTR_NAME))
                        .findFirst().orElse(null);
                if (exportAttribute == null)
                    continue;

                
            }
        }
    }

    @Input
    public List<Path> getModuleFolders() {
        return this.moduleFolders;
    }

    @Input
    public @NotNull Property<String> getBasePackage() {
        return this.basePackage;
    }

}
