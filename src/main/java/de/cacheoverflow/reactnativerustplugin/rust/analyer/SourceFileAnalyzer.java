package de.cacheoverflow.reactnativerustplugin.rust.analyer;

import de.cacheoverflow.reactnativerustplugin.exception.AnalyzerException;
import de.cacheoverflow.reactnativerustplugin.exception.AnalyzerProjectException;
import de.cacheoverflow.reactnativerustplugin.rust.analyer.data.RustFile;
import de.cacheoverflow.reactnativerustplugin.rust.analyer.data.RustFunction;
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

public class SourceFileAnalyzer {

    // TODO: Implement Validation Pass with Type Table per Project

    private final List<RustFile> rustFiles = new ArrayList<>();
    private final Logger logger;

    public SourceFileAnalyzer(@NotNull final Logger logger) {
        this.logger = logger;
    }

    public void analyzeDirectory(@NotNull final Path directory) throws AnalyzerException {
        this.logger.info("Beginning Source File Analysis in '{}'", directory.toAbsolutePath());

        // Validate Cargo.toml file
        final Path cargoFile = directory.resolve("Cargo.toml");
        if (!Files.exists(cargoFile) || !Files.isRegularFile(cargoFile))
            throw new AnalyzerProjectException("Directory '%s' isn't a Cargo Project (Cargo.toml is missing)", directory);

        // Validate src folder
        final Path sourceDirectory = directory.resolve("src");
        if (!Files.exists(sourceDirectory) || !Files.isDirectory(sourceDirectory))
            throw new AnalyzerProjectException("Directory '%s' isn't a Cargo Project (Source Folder is missing)", directory);

        // Create stream of source files
        try (final Stream<Path> sourceStream = Files.walk(sourceDirectory)) {
            int preCount = rustFiles.size();
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
                this.analyzeFile(sourceDirectory, path);
            }

            this.logger.info("Successfully analyzed {} source files in '{}'", rustFiles.size() - preCount, directory.toAbsolutePath());
        } catch (IOException ex) {
            throw new AnalyzerException(ex);
        }
    }

    public void analyzeFile(@NotNull final Path sourceDirectory, @NotNull final Path file) throws AnalyzerException {
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
            this.rustFiles.add(new RustFile(modulePath, functions, structures, imports));
            this.logger.debug("Modulated path to '{}'", modulePath);
        } catch (IOException ex) {
            throw new AnalyzerException(ex);
        }
    }

    public void reformatTypes() {
        this.logger.info("Reformat types from pathless type declaration to path type declaration (Type Prepare Pass)");
        for (RustFile file : this.rustFiles) {
            for (RustStruct struct : file.structs()) {
                this.logger.info("Modifying types in struct '{}' ({})", struct.name(), struct.parameters().entrySet()
                        .stream().map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue()))
                        .collect(Collectors.joining(", ")));

                for (Map.Entry<String, String> parameter : struct.parameters().entrySet()) {
                    if (!parameter.getValue().contains("::")) {
                        String type = file.imports().stream()
                                .map(importString -> new AbstractMap.SimpleEntry<>(importString.substring(importString
                                        .lastIndexOf("::") + 2), importString))
                                .filter(entry -> entry.getKey().equals(parameter.getValue()))
                                .map(AbstractMap.SimpleEntry::getValue)
                                .findFirst().orElse(parameter.getValue());
                        struct.parameters().put(parameter.getKey(), type);
                    }
                }

                this.logger.info("Finished type modification in struct '{}' ({})", struct.name(), struct.parameters().entrySet()
                        .stream().map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue()))
                        .collect(Collectors.joining(", ")));
            }
        }
    }

    public void validate() {

    }

    private  @NotNull String getRustModulePath(@NotNull final Path sourceDirectory, @NotNull final Path child) {
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

    public @NotNull List<RustFile> getRustFiles() {
        return Collections.unmodifiableList(this.rustFiles);
    }

}