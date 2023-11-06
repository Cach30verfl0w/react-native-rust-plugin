package de.cacheoverflow.reactnativerustplugin.codegen;

import de.cacheoverflow.reactnativerustplugin.exception.CodeGenerationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class ClassBuilder {

    final StringBuilder internalClassBuilder = new StringBuilder();
    final Stack<EnumScopeType> scopeStack = new Stack<>();
    final String className;

    public ClassBuilder(final int modifier, @NotNull final String name, @Nullable final String superClass,
                        @NotNull final List<String> interfaces) {
        // Breakup name to package and name
        final int lastDotIndex = name.lastIndexOf('.');
        final String className = name.substring(lastDotIndex == -1 ? 0 : lastDotIndex + 1);
        final String packageString = name.substring(0, lastDotIndex == -1 ? 0 : lastDotIndex);
        this.className = className;

        // Generate class string
        if (!packageString.trim().isEmpty()) {
            this.internalClassBuilder.append("package ").append(packageString).append(";\n");
        }

        this.internalClassBuilder.append(Modifier.toString(modifier)).append(" class ").append(className);
        if (superClass != null) {
            this.internalClassBuilder.append(" extends ").append(superClass);
        }

        if (!interfaces.isEmpty()) {
            this.internalClassBuilder.append(" implements ");
            for (final String interfaceString : interfaces) {
                this.internalClassBuilder.append(interfaceString).append(", ");
            }

            final int length = this.internalClassBuilder.length();
            this.internalClassBuilder.replace(length - 2, length, "");
        }

        this.internalClassBuilder.append(" {\n\n");
        this.pushScope(EnumScopeType.CLASS);
    }

    public void addField(final int modifier, @NotNull final String name, @NotNull final String type) {
        this.internalClassBuilder.repeat("    ", this.scopeStack.size());
        this.internalClassBuilder.append(Modifier.toString(modifier)).append(" ").append(type).append(" ").append(name)
                .append(";\n\n");
    }

    public @NotNull MethodBuilder addMethod(final int modifier, @NotNull final String name,
                                            @NotNull final Map<String, String> parameter,
                                            @Nullable final String returnType,
                                            @NotNull final List<String> annotations) {
        return new MethodBuilder(this, modifier, name, parameter, returnType, annotations);
    }

    public @NotNull MethodBuilder addMethod(final int modifier, @NotNull final String name,
                                            @NotNull final Map<String, String> parameter,
                                            @Nullable final String returnType) {
        return new MethodBuilder(this, modifier, name, parameter, returnType);
    }

    public @NotNull MethodBuilder addConstructor(final int modifier, @NotNull final Map<String, String> parameter) {
        return new MethodBuilder(this, modifier, parameter);
    }

    public @NotNull String build() {
        final EnumScopeType scope = this.popScope();
        if (scope != EnumScopeType.CLASS) {
            throw new CodeGenerationException("Uncomplete code generation while build function. Scope should be " +
                    "class but is %s", scope);
        }

        return this.internalClassBuilder.toString();
    }

    void pushScope(@NotNull final EnumScopeType type) {
        this.scopeStack.add(type);
    }

    @NotNull EnumScopeType popScope() {
        this.internalClassBuilder.repeat("    ", this.scopeStack.size() - 1).append("}\n\n");
        return this.scopeStack.pop();
    }

    enum EnumScopeType {
        FUNCTION,
        CLASS
    }

}
