package de.cacheoverflow.reactnativerustplugin.codegen;

import de.cacheoverflow.reactnativerustplugin.codegen.expressions.IExpression;
import de.cacheoverflow.reactnativerustplugin.exception.CodeGenerationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MethodBuilder {

    private final StringBuilder internalMethodBuilder = new StringBuilder();
    private final ClassBuilder classBuilder;

    MethodBuilder(@NotNull final ClassBuilder classBuilder, final int access,
                  @NotNull final String name, @NotNull final Map<String, String> parameters,
                  @Nullable String returnType, @NotNull final List<String> annotations) {
        this.classBuilder = classBuilder;
        ClassBuilder.EnumScopeType currentScope = this.classBuilder.scopeStack.peek();
        if (currentScope != ClassBuilder.EnumScopeType.CLASS)
            throw new CodeGenerationException("Expected scope 'CLASS', but got '%s'", currentScope);

        for (String annotation : annotations) {
            this.internalMethodBuilder.repeat("    ", this.classBuilder.scopeStack.size());
            this.internalMethodBuilder.append("@").append(annotation).append("\n");
        }
        this.internalMethodBuilder.repeat("    ", this.classBuilder.scopeStack.size());
        this.internalMethodBuilder.append(Modifier.toString(access)).append(" ")
                .append(Optional.ofNullable(returnType).orElse("void")).append(" ").append(name);

        // Commit arguments
        this.internalMethodBuilder.append("(");
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            this.internalMethodBuilder.append(parameter.getValue()).append(" ").append(parameter.getKey()).append(", ");
        }

        if (!parameters.isEmpty()) {
            final int lastParameterEnd = this.internalMethodBuilder.lastIndexOf(", ");
            this.internalMethodBuilder.replace(lastParameterEnd, lastParameterEnd + 2, "");
        }

        this.internalMethodBuilder.append(")");
        if (!Modifier.has(access, Modifier.NATIVE)) {
            this.classBuilder.pushScope(ClassBuilder.EnumScopeType.FUNCTION);
            this.internalMethodBuilder.append(" {\n");
        } else this.internalMethodBuilder.append(";\n\n");
    }

    MethodBuilder(@NotNull final ClassBuilder classBuilder, final int access,
                         @NotNull final String name, @NotNull final Map<String, String> parameters,
                         @Nullable String returnType) {
        this(classBuilder, access, name, parameters, returnType, List.of());
    }

    MethodBuilder(@NotNull final ClassBuilder classBuilder, final int access,
                  @NotNull final Map<String, String> parameters) {
        this.classBuilder = classBuilder;
        ClassBuilder.EnumScopeType currentScope = this.classBuilder.scopeStack.peek();
        if (currentScope != ClassBuilder.EnumScopeType.CLASS)
            throw new CodeGenerationException("Expected scope 'CLASS', but got '%s'", currentScope);

        this.internalMethodBuilder.repeat("    ", this.classBuilder.scopeStack.size());
        this.internalMethodBuilder.append(Modifier.toString(access)).append(" ").append(classBuilder.className);

        // Commit arguments
        this.internalMethodBuilder.append("(");
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            this.internalMethodBuilder.append(parameter.getValue()).append(" ").append(parameter.getKey()).append(", ");
        }

        if (!parameters.isEmpty()) {
            final int lastParameterEnd = this.internalMethodBuilder.lastIndexOf(", ");
            this.internalMethodBuilder.replace(lastParameterEnd, lastParameterEnd + 2, "");
        }

        this.internalMethodBuilder.append(") ");
        if (!Modifier.has(access, Modifier.NATIVE)) {
            this.classBuilder.pushScope(ClassBuilder.EnumScopeType.FUNCTION);
            this.internalMethodBuilder.append("{\n");
        } else this.internalMethodBuilder.append(";\n");
    }

    public @NotNull MethodBuilder addStatement(@NotNull final IExpression expression) {
        this.internalMethodBuilder
                .repeat("    ", this.classBuilder.scopeStack.size())
                .append(expression).append(";\n");
        return this;
    }

    public void build() {
        // Append this builder to the class builder
        this.classBuilder.internalClassBuilder.append(this.internalMethodBuilder);
        if (this.classBuilder.scopeStack.peek() == ClassBuilder.EnumScopeType.FUNCTION)
            this.classBuilder.popScope();
    }

}
