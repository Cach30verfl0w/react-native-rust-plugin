package de.cacheoverflow.reactnativerustplugin.codegen;

import de.cacheoverflow.reactnativerustplugin.exception.CodeGenerationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Stack;

public class ClassBuilder {

    private final StringBuilder internalClassBuilder = new StringBuilder();
    private final Stack<EnumScopeType> scopeStack = new Stack<>();

    public ClassBuilder(final int modifier, @NotNull final String name, @Nullable final String superClass,
                        @NotNull final List<String> interfaces) {
        // Breakup name to package and name
        final int lastDotIndex = name.lastIndexOf('.');
        final String className = name.substring(lastDotIndex == -1 ? 0 : lastDotIndex + 1);
        final String packageString = name.substring(0, lastDotIndex == -1 ? 0 : lastDotIndex);

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

        this.internalClassBuilder.append(" {\n");
        this.pushScope(EnumScopeType.CLASS);
    }

    public void addField(final byte modifier, @NotNull final String name, @NotNull final String type) {
        this.internalClassBuilder.repeat("   ", this.scopeStack.size());
        this.pushScope(EnumScopeType.FIELD);
        this.internalClassBuilder.append(Modifier.toString(modifier)).append(" ").append(type).append(" ").append(name)
                .append(";");
        this.popScope();
    }

    public @NotNull String build() {
        EnumScopeType scope = this.popScope();
        if (scope != EnumScopeType.CLASS) {
            throw new CodeGenerationException("Uncomplete code generation while build function. Scope should be " +
                    "class but is %s", scope);
        }

        return this.internalClassBuilder.toString();
    }

    private void pushScope(@NotNull final EnumScopeType type) {
        this.scopeStack.add(type);
    }

    private @NotNull EnumScopeType popScope() {
        EnumScopeType type = this.scopeStack.pop();
        if (type.hasBody) {
            this.internalClassBuilder.repeat("    ", this.scopeStack.size()).append("}");
        }
        return type;
    }

    private enum EnumScopeType {
        BODY_LESS_FUNCTION(false),
        FUNCTION(true),
        CLASS(true),
        FIELD(false);

        private final boolean hasBody;

        EnumScopeType(final boolean hasBody) {
            this.hasBody = hasBody;
        }
    }

}