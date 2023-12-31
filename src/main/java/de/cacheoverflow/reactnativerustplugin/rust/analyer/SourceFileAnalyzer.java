package de.cacheoverflow.reactnativerustplugin.rust.analyer;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import de.cacheoverflow.reactnativerustplugin.codegen.TypeMapper;
import de.cacheoverflow.reactnativerustplugin.exception.AnalyzerException;
import de.cacheoverflow.reactnativerustplugin.exception.AnalyzerProjectException;
import de.cacheoverflow.reactnativerustplugin.rust.analyer.data.RustFile;
import de.cacheoverflow.reactnativerustplugin.rust.analyer.data.RustFunction;
import de.cacheoverflow.reactnativerustplugin.rust.analyer.data.RustProject;
import de.cacheoverflow.reactnativerustplugin.rust.analyer.data.RustStruct;
import de.cacheoverflow.reactnativerustplugin.rust.parser.RustLexer;
import de.cacheoverflow.reactnativerustplugin.rust.parser.RustParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SourceFileAnalyzer {

    // TODO: Implement Validation Pass with Type Table per Project

    private final List<RustProject> projects = new ArrayList<>();
    private final Logger logger;

    public SourceFileAnalyzer(@NotNull final Logger logger) {
        this.logger = logger;
    }

    public void analyzeProject(@NotNull final Path directory) throws AnalyzerException {
        this.logger.info("Beginning Source File Analysis in '{}'", directory.toAbsolutePath());

        // Validate Cargo.toml file
        final Path cargoFile = directory.resolve("Cargo.toml");
        if (!Files.exists(cargoFile) || !Files.isRegularFile(cargoFile))
            throw new AnalyzerProjectException("Directory '%s' isn't a Cargo Project (Cargo.toml is missing)", directory);

        // Read Cargo.toml
        String packageName;
        final List<String> dependencies = new ArrayList<>();
        try (FileConfig fileConfig = FileConfig.of(cargoFile)) {
            fileConfig.load();

            packageName = fileConfig.get("package.name");
            fileConfig.<Config>getOptional("dependencies").ifPresent(config -> {
                dependencies.addAll(config.entrySet().stream().map(UnmodifiableConfig.Entry::getKey).toList());
            });
        }

        // Validate src folder
        final Path sourceDirectory = directory.resolve("src");
        if (!Files.exists(sourceDirectory) || !Files.isDirectory(sourceDirectory))
            throw new AnalyzerProjectException("Directory '%s' isn't a Cargo Project (Source Folder is missing)", directory);

        // Create stream of source files
        List<RustFile> projectFiles = new ArrayList<>();
        try (final Stream<Path> sourceStream = Files.walk(sourceDirectory)) {
            for (final Path path : sourceStream.toList()) {
                // Ignore directories
                if (!Files.isRegularFile(path))
                    continue;

                // Ignore files without extension
                final String name = path.getFileName().toString();
                if (!name.contains("."))
                    continue;

                // Check if file is a rust source file
                if (!name.substring(name.lastIndexOf(".") + 1).equals("rs"))
                    continue;

                // Analyze single file
                this.analyzeFile(sourceDirectory, path, projectFiles);
            }

            this.logger.info("Successfully analyzed {} source files in '{}'", projectFiles.size(), directory.toAbsolutePath());
            this.projects.add(new RustProject(packageName, projectFiles, dependencies));
        } catch (IOException ex) {
            throw new AnalyzerException(ex);
        }
    }

    public void analyzeFile(@NotNull final Path sourceDirectory, @NotNull final Path file,
                            @NotNull final List<RustFile> projectFiles) throws AnalyzerException {
        // Information to the user about the analysis
        this.logger.info("Analyze rust source file '{}'", file.toAbsolutePath());
        try {
            // Tokenize
            final String fileSource = Files.readString(file);
            final RustLexer lexer = new RustLexer(CharStreams.fromString(fileSource));
            final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            tokenStream.fill();

            // Parse the lexer-generated tokens and walk over AST
            final RustParser parser = new RustParser(tokenStream);
            parser.removeErrorListeners();

            List<RustFunction> functions = new ArrayList<>();
            List<RustStruct> structures = new ArrayList<>();
            List<String> imports = new ArrayList<>();
            ParseTreeWalker.DEFAULT.walk(new SourceFileAnalyzerListener(functions, structures, imports), parser.crate());

            // Complete analyzed file to module file
            String modulePath = this.getRustModulePath(sourceDirectory, file);
            projectFiles.add(new RustFile(modulePath, functions, structures, imports));
            this.logger.debug("Modulated path to '{}'", modulePath);
        } catch (IOException ex) {
            throw new AnalyzerException(ex);
        }
    }

    public void reformatFunctions(@NotNull final TypeMapper typeMapper) {
        this.logger.info("Reformat function arguments from pathless function types to path-ful types (Function Prepare Pass)");
        for (RustProject project : this.projects) {
            for (RustFile file : project.files()) {
                final List<RustFunction> newFunctions = new ArrayList<>();
                for (RustFunction function : file.functions()) {
                    this.logger.info("Modifying types in function '{}' ({}) -> {}", function.functionName(),
                            function.parameters().entrySet().stream().map(entry -> String.format("%s: %s", entry.getKey(),
                                            entry.getValue()))
                            .collect(Collectors.joining(", ")), function.returnType().orElse("void"));

                    // Enumerate all parameters in function
                    for (final Map.Entry<String, String> parameter : function.parameters().entrySet()) {
                        // Only modify type name when name isn't already specified with path
                        if (!parameter.getValue().contains("::")) {
                            // Replace type names with new type names
                            function.parameters().put(parameter.getKey(), this.reformatType(typeMapper, parameter.getValue(),
                                    project.projectName(), file));
                        }
                    }
                    newFunctions.add(new RustFunction(function.attributes(), function.functionName(), function.parameters(),
                            function.returnType().map(type -> this.reformatType(typeMapper, type, project.projectName(), file))));

                    // Send information to user
                    this.logger.info("Finished type modification in struct '{}' ({}) -> {}", function.functionName(), function
                            .parameters().entrySet().stream().map(entry -> String.format("%s: %s", entry.getKey(),
                                    entry.getValue())), function.returnType().orElse("void"));
                }

                file.functions().clear();
                file.functions().addAll(newFunctions);
            }
        }
    }

    public void renameStructs() {
        this.logger.info("Rename structures from pathless struct names to path struct names (Struct Prepare Pass)");
        for (RustProject project : this.projects) {
            // Enumerate all files in project
            for (RustFile file : project.files()) {
                // Enumerate all structures in project, add path to name, clear structs list and insert new struct list
                List<RustStruct> newRustStructs = new ArrayList<>();
                for (RustStruct struct : file.structs()) {
                    // Generate new structure name with path
                    String structNameWithPath = String.format("%s::%s", file.path().replace("crate::", ""),
                            struct.name());

                    // Inform the user about the change and add structure to list
                    this.logger.info("Modifying struct name from {} to {}", struct.name(), structNameWithPath);
                    newRustStructs.add(new RustStruct(struct.attributes(), structNameWithPath, struct.parameters()));
                }
                file.structs().clear();
                file.structs().addAll(newRustStructs);
            }
        }
    }

    public void reformatTypes(@NotNull final TypeMapper typeMapper) {
        this.logger.info("Reformat types from pathless type declarations to path type declarations (Type Prepare Pass)");
        for (RustProject project : this.projects) {
            for (RustFile file : project.files()) {
                for (RustStruct struct : file.structs()) {
                    // Send information to user
                    this.logger.info("Modifying types in struct '{}' ({})", struct.name(), struct.parameters().entrySet()
                            .stream().map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue()))
                            .collect(Collectors.joining(", ")));

                    // Enumerate all parameters in struct
                    for (Map.Entry<String, String> parameter : struct.parameters().entrySet()) {
                        // Only modify type name when name isn't already specified with path
                        if (!parameter.getValue().contains("::")) {
                            // Replace type names with new type names
                            struct.parameters().put(parameter.getKey(), this.reformatType(typeMapper, parameter.getValue(),
                                    project.projectName(), file));
                        }
                    }

                    // Send information to user
                    this.logger.info("Finished type modification in struct '{}' ({})", struct.name(), struct.parameters().entrySet()
                            .stream().map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue())));
                }
            }
        }
    }

    private @NotNull String reformatType(@NotNull final TypeMapper mapper, @NotNull final String rustType,
                                         @NotNull final String projectName, @NotNull final RustFile file) {
        // Skip default types
        if (mapper.isDefaultTypeRust(rustType)) {
            return rustType;
        }

        // Generate type name with path
        String type = file.imports().stream()
                .map(importString -> new AbstractMap.SimpleEntry<>(importString.substring(importString
                        .lastIndexOf("::") + 2), importString))
                .filter(entry -> entry.getKey().equals(rustType))
                .map(AbstractMap.SimpleEntry::getValue)
                .findFirst().orElse(null);

        // If there is no imported path, default to own struct
        if (type == null)
            type = String.format("%s::%s::%s", projectName.replace("-", "_"), file.path(), rustType);

        // If there is no naming, set type to type decl before
        if (type == null)
            type = rustType;

        return type;
    }

    private @NotNull String getRustModulePath(@NotNull final Path sourceDirectory, @NotNull final Path child) {
        Path modulePath = child.subpath(sourceDirectory.getNameCount(), child.getNameCount());
        if (modulePath.getFileName().toString().equals("mod.rs")) {
            modulePath = modulePath.getParent();
        }

        if (modulePath.getFileName().toString().equals("lib.rs")) {
            modulePath = modulePath.getParent();
        }

        String moduleString = (modulePath != null ? String.format("crate::%s", modulePath) : "crate");
        return (moduleString.contains(".") ? moduleString.substring(0, moduleString.lastIndexOf(".")) : moduleString)
                .replace("/", "::");
    }

    public List<RustProject> getProjects() {
        return Collections.unmodifiableList(this.projects);
    }
}
