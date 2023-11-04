package de.cacheoverflow.reactnativerustplugin.codegen;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class TypeMapper {

    private final Map<String, String> mappings = new HashMap<>();

    public TypeMapper() {
        // Rust Type Mappings
        this.registerIfNotExists("String", "String");

        this.registerIfNotExists("i32", "int");
        this.registerIfNotExists("i64", "long");
        this.registerIfNotExists("i8", "byte");
        this.registerIfNotExists("u8", "boolean");
        this.registerIfNotExists("u16", "char");
        this.registerIfNotExists("i16", "short");
        this.registerIfNotExists("f32", "float");
        this.registerIfNotExists("f64", "double");

        // JNI Type Mappings
        this.registerIfNotExists("jint", "int");
        this.registerIfNotExists("jlong", "long");
        this.registerIfNotExists("jbyte", "byte");
        this.registerIfNotExists("jboolean", "boolean");
        this.registerIfNotExists("jchar", "char");
        this.registerIfNotExists("jshort", "short");
        this.registerIfNotExists("jfloat", "float");
        this.registerIfNotExists("jdouble", "double");
        this.registerIfNotExists("jsize", "int");
        this.registerIfNotExists("jstring", "String");

        // JNI Type Mappings
        this.registerIfNotExists("jni::objects::jstring", "String");
        this.registerIfNotExists("jni::objects::jint", "int");
        this.registerIfNotExists("jni::objects::jlong", "long");
        this.registerIfNotExists("jni::objects::jbyte", "byte");
        this.registerIfNotExists("jni::objects::jboolean", "boolean");
        this.registerIfNotExists("jni::objects::jchar", "char");
        this.registerIfNotExists("jni::objects::jshort", "short");
        this.registerIfNotExists("jni::objects::jfloat", "float");
        this.registerIfNotExists("jni::objects::jdouble", "double");
        this.registerIfNotExists("jni::objects::jsize", "int");
    }

    public void registerIfNotExists(@NotNull final String rustType, @NotNull final String javaType) {
        if (mappings.get(rustType) != null)
            return;

        this.mappings.put(rustType, javaType);
    }

    public @NotNull String map(@NotNull final String rustType) {
        if (rustType.equals("void"))
            return rustType;

        String javaType = this.mappings.get(rustType);
        if (javaType == null)
            throw new IllegalArgumentException(String.format("No mapping found for %s", rustType));

        return javaType;
    }

}
