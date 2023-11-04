package de.cacheoverflow.reactnativerustplugin.tasks;

import de.cacheoverflow.reactnativerustplugin.codegen.ClassBuilder;
import de.cacheoverflow.reactnativerustplugin.codegen.MethodBuilder;
import de.cacheoverflow.reactnativerustplugin.codegen.Modifier;
import de.cacheoverflow.reactnativerustplugin.codegen.TypeMapper;
import de.cacheoverflow.reactnativerustplugin.codegen.expressions.*;
import de.cacheoverflow.reactnativerustplugin.exception.AnalyzerException;
import de.cacheoverflow.reactnativerustplugin.exception.CodeGenerationException;
import de.cacheoverflow.reactnativerustplugin.rust.analyer.SourceFileAnalyzer;
import de.cacheoverflow.reactnativerustplugin.rust.analyer.data.*;
import de.cacheoverflow.reactnativerustplugin.utils.MapHelper;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        final TypeMapper typeMapper = new TypeMapper();
        sourceFileAnalyzer.renameStructs();
        sourceFileAnalyzer.reformatFunctions(typeMapper);
        sourceFileAnalyzer.reformatTypes(typeMapper);

        // Generate Type Mapping
        this.getLogger().info("Generate Type Mappings");
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

        final Map<String, ClassBuilder> generatedClasses = new HashMap<>();
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
                final ClassBuilder classBuilder = this.generateStructClass(typeMapper, struct, className);

                // Add generate class to generated classes map
                this.getLogger().info("Generated class '{}' from project '{}'", className, project.projectName());
                generatedClasses.put(className, classBuilder);
            }
        }

        // Inform the user about the generation and write generated classes to specified directories
        this.getLogger().info("Generated {} classes as Wrapper for Rust structs", generatedClasses.size());

        for (final Map.Entry<String, ClassBuilder> classEntry : generatedClasses.entrySet()) {
            final Path classPath = generatedSourceFolder.resolve(String.format("%s.java", classEntry.getKey()
                    .replace(".", "/")));
            PathHelper.createDirectoryIfNotExists(this.getProject(), classPath.getParent());
            PathHelper.writeFile(classPath, classEntry.getValue().build());
            this.getLogger().info("Successfully wrote class '{}' into '{}'", classEntry.getKey(), classPath.toAbsolutePath());
        }

        // Generate module classes for mapping between Rust and Java functions
        generatedClasses.clear();
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

                // Get name of class and check if class builder is already present
                final String className = exportAttribute.parameters().get("class").replace("\"", "");
                final boolean isClassBuilderAlreadyPresent = generatedClasses.containsKey(className);

                // Get class builder if present, otherwise create new class builder
                final ClassBuilder classBuilder = generatedClasses.entrySet().stream()
                        .filter(entry -> entry.getKey().equals(className))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(new ClassBuilder(Modifier.PUBLIC | Modifier.FINAL, className + "Module", null,
                                List.of("com.facebook.react.bridge.ReactContextBaseJavaModule")));

                // Generate constructor and getName method if necessary
                final String classNameNoPackage = className.replace(className.substring(0, className.lastIndexOf('.') + 1), "");
                if (!isClassBuilderAlreadyPresent) {
                    // Generate constructor
                    final Map<String, String> params = Map.of("context", "com.facebook.react.bridge.ReactApplicationContext");
                    classBuilder
                            .addConstructor(Modifier.PUBLIC, params)
                            .addStatement(new CallExpression("super", List.of(new VariableExpression("context", false))))
                            .build();

                    // Generate getName method
                    classBuilder
                            .addMethod(Modifier.PUBLIC, "getName", Map.of(), "String")
                            .addStatement(new ReturnStatement(new ValueExpression(classNameNoPackage)))
                            .build();
                }

                // Map types for parameters
                final Map<String, String> parameters = MapHelper.reversed(function.parameters().entrySet().stream()
                        .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), typeMapper.map(entry.getValue())))
                        .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)));

                // Generate native method
                final String mappedReturnType = typeMapper.map(function.returnType().orElse("void"));
                classBuilder.addMethod(Modifier.PUBLIC | Modifier.STATIC | Modifier.NATIVE, function.functionName(),
                        parameters, mappedReturnType).build();

                // Generate mapper method
                final Map<String, String> mapperParameters = new LinkedHashMap<>();
                mapperParameters.put("promise", "com.facebook.react.bridge.Promise");
                mapperParameters.putAll(MapHelper.replaceAllValues(parameters,
                        Predicate.not(typeMapper::isDefaultTypeJava), "com.facebook.react.bridge.ReadableMap"));
                final MethodBuilder wrapperBuilder = classBuilder.addMethod(Modifier.PUBLIC, function.functionName(),
                        mapperParameters,
                        "void", List.of("com.facebook.react.bridge.ReactMethod"));

                // Generate mapper method body
                final List<IExpression> callParameters = new ArrayList<>(parameters.entrySet().stream()
                        .map(parameter -> {
                            if (typeMapper.isDefaultTypeJava(parameter.getValue())) {
                                return new VariableExpression(parameter.getKey(), false);
                            } else {
                                return new CallExpression(parameter.getValue() + ".fromMap",
                                        List.of(new VariableExpression(parameter.getKey(), false)));
                            }
                        })
                        .toList());

                // Get return type and default information
                final boolean returnTypeDefault = function.returnType().map(typeMapper::isDefaultTypeRust).orElse(true);

                // Generate function content
                final CallExpression functionCallExpression = new CallExpression(classNameNoPackage + "." +
                        function.functionName(), callParameters, returnTypeDefault ? "" : ".toMap()");
                if (function.returnType().isPresent()) {
                    wrapperBuilder.addStatement(new CallExpression("promise.resolve", List.of(functionCallExpression)));
                } else {
                    wrapperBuilder.addStatement(functionCallExpression);
                    wrapperBuilder.addStatement(new CallExpression("promise.resolve", List.of(new ValueExpression(null))));
                }

                // Finish method generation
                wrapperBuilder.build();

                // Save class builder in map if necessary
                if (!isClassBuilderAlreadyPresent) {
                    generatedClasses.put(className, classBuilder);
                }
            }
        }


        // Inform the user about the generation and write generated classes to specified directories
        this.getLogger().info("Generated {} classes as Wrapper for Rust functions", generatedClasses.size());

        for (final Map.Entry<String, ClassBuilder> classEntry : generatedClasses.entrySet()) {
            final Path classPath = generatedSourceFolder.resolve(String.format("%s.java", classEntry.getKey()
                    .replace(".", "/")));
            PathHelper.createDirectoryIfNotExists(this.getProject(), classPath.getParent());
            PathHelper.writeFile(classPath, classEntry.getValue().build());
            this.getLogger().info("Successfully wrote class '{}' into '{}'", classEntry.getKey(), classPath.toAbsolutePath());
        }
    }

    private @NotNull ClassBuilder generateStructClass(@NotNull final TypeMapper typeMapper,
                                                            @NotNull final RustStruct struct, @NotNull final String className) {
        final ClassBuilder classBuilder = new ClassBuilder(Modifier.PUBLIC | Modifier.FINAL, className, null, List.of());

        // Map fields for types
        final Map<String, String> mappedParameters = struct.parameters().entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), typeMapper.map(entry.getValue())))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
        mappedParameters.forEach((name, value) -> classBuilder.addField(Modifier.PUBLIC, name, value));

        // Generate constructor for class
        MethodBuilder methodBuilder = classBuilder.addConstructor(Modifier.PUBLIC, mappedParameters);
        mappedParameters.forEach((name, ignored) -> methodBuilder.addStatement(new AssignmentStatement(
                new VariableExpression(name, true), new VariableExpression(name, false))));
        methodBuilder.build();

        // Generate fromMap method
        MethodBuilder fromMapBuilder = classBuilder.addMethod(Modifier.PUBLIC | Modifier.STATIC, "fromMap",
                Map.of("map", "com.facebook.react.bridge.ReadableMap"), className);

        // Collect parameters for constructor call
        final List<IExpression> parametersForBuild = new ArrayList<>();
        struct.parameters().forEach((name, type) -> {
            final String javaType = typeMapper.map(type);

            if (typeMapper.isDefaultTypeRust(type)) {
                parametersForBuild.add(new CallExpression(String.format("map.get%s", this.capitalize(javaType)),
                        List.of(new ValueExpression(name))));
            } else {
                final IExpression getMapExpression = new CallExpression("map.getMap", List.of(new ValueExpression(name)));
                parametersForBuild.add(new CallExpression(javaType + ".fromMap", List.of(getMapExpression)));
            }
        });

        // Emit constructor call and finish build
        fromMapBuilder.addStatement(new ReturnStatement(new CallExpression("new " + className, parametersForBuild)));
        fromMapBuilder.build();

        // Generate toMap method
        MethodBuilder toMapBuilder = classBuilder.addMethod(Modifier.PUBLIC, "toMap",
                Map.of(), "com.facebook.react.bridge.ReadableMap");
        toMapBuilder.addStatement(new AssignmentStatement(new VariableExpression("final WritableMap map", false),
                new CallExpression("new com.facebook.react.bridge.WritableNativeMap", List.of())));

        struct.parameters().forEach((name, type) -> {
            final String javaType = typeMapper.map(type);

            if (typeMapper.isDefaultTypeRust(type)) {
                toMapBuilder.addStatement(new CallExpression(String.format("map.put%s", this.capitalize(javaType)),
                        List.of(new VariableExpression(name, true), new ValueExpression(name))));
            } else {
                final IExpression getMapExpression = new CallExpression("this." + name + ".toMap", List.of());
                toMapBuilder.addStatement(new CallExpression("map.putMap", List.of(getMapExpression)));
            }
        });

        toMapBuilder.addStatement(new ReturnStatement(new VariableExpression("map", true)));
        toMapBuilder.build();

        // Apply getter and setter for fields
        mappedParameters.forEach((name, value) -> {
            classBuilder
                    .addMethod(Modifier.PUBLIC, String.format("set%s", this.capitalize(name)), Map.of(name, value), null)
                    .addStatement(new AssignmentStatement(new VariableExpression(name, true),
                            new VariableExpression(name, false)))
                    .build();

            classBuilder
                    .addMethod(Modifier.PUBLIC, String.format("get%s", this.capitalize(name)), Map.of(), value)
                    .addStatement(new ReturnStatement(new VariableExpression(name, true)))
                    .build();
        });

        return classBuilder;
    }

    @Input
    public List<Path> getModuleFolders() {
        return this.moduleFolders;
    }

    @Input
    public @NotNull Property<String> getBasePackage() {
        return this.basePackage;
    }

    private @NotNull String capitalize(@NotNull final String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

}
