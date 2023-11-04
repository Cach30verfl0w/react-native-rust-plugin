package de.cacheoverflow.reactnativerustplugin.codegen.expressions;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class CallExpression implements IExpression {

    private final String name;
    private final Collection<IExpression> arguments;

    public CallExpression(@NotNull final String name, @NotNull final Collection<IExpression> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public @NotNull String toString() {
        final StringBuilder stringBuilder = new StringBuilder(name).append("(");
        for (final IExpression argument : arguments) {
            stringBuilder.append(argument).append(", ");
        }
        final int commaIndex = stringBuilder.append(")").lastIndexOf(", ");
        return commaIndex != -1 ? stringBuilder.replace(commaIndex, commaIndex + 2, "").toString() : stringBuilder.toString();
    }
}
