package de.cacheoverflow.reactnativerustplugin.codegen.expressions;

import org.jetbrains.annotations.NotNull;

public class ReturnStatement implements IStatement {

    private final IExpression expression;

    public ReturnStatement(@NotNull final IExpression expression) {
        this.expression = expression;
    }

    @Override
    public @NotNull String toString() {
        return "return " + this.expression;
    }

}
