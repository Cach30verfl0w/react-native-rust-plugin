package de.cacheoverflow.reactnativerustplugin.codegen.expressions;

import org.jetbrains.annotations.NotNull;

public class AssignmentExpression implements IStatement {

    private final IExpression expression1;
    private final IExpression expression2;

    public AssignmentExpression(@NotNull final IExpression expression1, @NotNull final IExpression expression2) {
        this.expression1 = expression1;
        this.expression2 = expression2;
    }

    @Override
    public @NotNull String toString() {
        return this.expression1 + " = " + this.expression2;
    }
}
