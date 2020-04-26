package com.codethen.mongo.adapter.generator;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class AdapterGeneratorContext {

    /** Path of source files where adapters will be put (e.g. "src/main/java")*/
    private final String sourcePath;
    /** Package of the generated adapters (e.g. "com.brand.project.mongo.adapters") */
    private final String packageName;
    /** Known adapters for model classes. */
    private final Map<Class<?>, TypeSpec> adapters;

    public AdapterGeneratorContext(String sourcePath, String packageName) {
        this.sourcePath = sourcePath;
        this.packageName = packageName;
        this.adapters = new HashMap<>();
    }

    public <T> void createAdapter(AdapterGenerator adapterGenerator, Consumer<AdapterGenerator> config) {
        try {
            adapterGenerator.setContext(this);
            config.accept(adapterGenerator);
            final TypeSpec adapterTypeSpec = adapterGenerator.build();
            adapters.put(adapterGenerator.getModelClass(), adapterTypeSpec);
            JavaFile.builder(packageName, adapterTypeSpec).build().writeTo(new File(sourcePath));
        } catch (Exception e) {
            throw new RuntimeException("Problem building the adapter", e);
        }
    }

    public Map<Class<?>, TypeSpec> getAdapters() {
        return adapters;
    }
}
