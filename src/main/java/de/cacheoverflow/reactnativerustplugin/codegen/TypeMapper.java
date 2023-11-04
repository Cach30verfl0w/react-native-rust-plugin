package de.cacheoverflow.reactnativerustplugin.codegen;

import de.cacheoverflow.reactnativerustplugin.utils.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TypeMapper {

    private final Map<String, Pair<String, Boolean>> mappings = new HashMap<>();

    public TypeMapper() {
        // Rust Type Mappings
        this.registerIfNotExists("String", "String", true);

        this.registerIfNotExists("i32", "int", true);
        this.registerIfNotExists("i64", "long", true);
        this.registerIfNotExists("i8", "byte", true);
        this.registerIfNotExists("u8", "boolean", true);
        this.registerIfNotExists("u16", "char", true);
        this.registerIfNotExists("i16", "short", true);
        this.registerIfNotExists("f32", "float", true);
        this.registerIfNotExists("f64", "double", true);

        // JNI Type Mappings
        this.registerIfNotExists("jint", "int", true);
        this.registerIfNotExists("jlong", "long", true);
        this.registerIfNotExists("jbyte", "byte", true);
        this.registerIfNotExists("jboolean", "boolean", true);
        this.registerIfNotExists("jchar", "char", true);
        this.registerIfNotExists("jshort", "short", true);
        this.registerIfNotExists("jfloat", "float", true);
        this.registerIfNotExists("jdouble", "double", true);
        this.registerIfNotExists("jsize", "int", true);
        this.registerIfNotExists("jstring", "String", true);

        // JNI Type Mappings
        this.registerIfNotExists("jni::objects::jstring", "String", true);
        this.registerIfNotExists("jni::objects::jint", "int", true);
        this.registerIfNotExists("jni::objects::jlong", "long", true);
        this.registerIfNotExists("jni::objects::jbyte", "byte", true);
        this.registerIfNotExists("jni::objects::jboolean", "boolean", true);
        this.registerIfNotExists("jni::objects::jchar", "char", true);
        this.registerIfNotExists("jni::objects::jshort", "short", true);
        this.registerIfNotExists("jni::objects::jfloat", "float", true);
        this.registerIfNotExists("jni::objects::jdouble", "double", true);
        this.registerIfNotExists("jni::objects::jsize", "int", true);
    }

    public void registerIfNotExists(@NotNull final String rustType, @NotNull final String javaType) {
        this.registerIfNotExists(rustType, javaType, false);
    }

    public void registerIfNotExists(@NotNull final String rustType, @NotNull final String javaType, final boolean standard) {
        if (mappings.get(rustType) != null)
            return;

        this.mappings.put(rustType, new Pair<>(javaType.replace("\"", ""), standard));
    }

    public @NotNull String map(@NotNull final String rustType) {
        if (rustType.equals("void"))
            return rustType;

        String javaType = Optional.ofNullable(this.mappings.get(rustType)).map(Pair::getFirst).orElse(null);
        if (javaType == null)
            throw new IllegalArgumentException(String.format("No mapping found for %s", rustType));

        return javaType;
    }

    public boolean isDefaultTypeRust(@NotNull final String rustType) {
        return Optional.ofNullable(this.mappings.get(rustType)).map(Pair::getSecond).orElse(false);
    }

    public boolean isDefaultTypeJava(@NotNull final String javaType) {
        return this.mappings.values().stream().filter(stringBooleanPair -> stringBooleanPair.getFirst().equals(javaType))
                .map(Pair::getSecond)
                .findFirst().orElse(false);
    }


}
