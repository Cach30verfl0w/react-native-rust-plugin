package de.cacheoverflow.reactnativerustplugin.codegen.expressions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ValueExpression implements IExpression {

    private final Object value;

    public ValueExpression(@Nullable final Object value) {
        this.value = value;
    }

    @Override
    public @NotNull String toString() {
        if (this.value == null)
            return "null";

        if (this.value instanceof String) {
            return String.format("\"%s\"", this.value);
        }

        return this.value.toString();
    }

}
