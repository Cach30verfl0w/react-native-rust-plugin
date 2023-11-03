package de.cacheoverflow.reactnativerustplugin.codegen;

import org.jetbrains.annotations.NotNull;

public class Modifier {

    public static final byte PACKAGE   = 0b0000_0001;
    public static final byte PUBLIC    = 0b0000_0010;
    public static final byte PRIVATE   = 0b0000_0100;
    public static final byte PROTECTED = 0b0000_1000;
    public static final byte STATIC    = 0b0001_0000;
    public static final byte NATIVE    = 0b0010_0000;
    public static final byte FINAL     = 0b0100_0000;

    private Modifier() {
        throw new UnsupportedOperationException();
    }

    public static boolean has(final int accessBits, final byte access) {
        return (accessBits & access) == access;
    }

    public static @NotNull String toString(final int modifier) {
        StringBuilder stringBuilder = new StringBuilder();
        if (!Modifier.has(modifier, Modifier.PACKAGE)) {
            if (Modifier.has(modifier, Modifier.PUBLIC)) {
                stringBuilder.append("public ");
            } else if (Modifier.has(modifier, Modifier.PRIVATE)) {
                stringBuilder.append("private ");
            } else if (Modifier.has(modifier, Modifier.PROTECTED)) {
                stringBuilder.append("protected ");
            }
        }

        if (Modifier.has(modifier, Modifier.STATIC)) {
            stringBuilder.append("static ");
        }

        if (Modifier.has(modifier, Modifier.NATIVE)) {
            stringBuilder.append("native ");
        }

        if (Modifier.has(modifier, Modifier.FINAL)) {
            stringBuilder.append("final ");
        }

        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }

}
