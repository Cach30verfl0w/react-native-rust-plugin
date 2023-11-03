package de.cacheoverflow.reactnativerustplugin.codegen.expressions;

import org.jetbrains.annotations.NotNull;

public class VariableExpression implements IExpression {

    private final String name;
    private final boolean isThis;

    public VariableExpression(@NotNull final String name, final boolean isThis) {
        this.name = name;
        this.isThis = isThis;
    }

    @Override
    public @NotNull String toString() {
        return (this.isThis ? "this." : "") + this.name;
    }

}
