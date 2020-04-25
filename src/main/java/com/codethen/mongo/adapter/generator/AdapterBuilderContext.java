package com.codethen.mongo.adapter.generator;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AdapterBuilderContext {

    /** Path of source files where adapters will be put (e.g. "src/main/java")*/
    private final String sourcePath;
    /** Package of the generated adapters (e.g. "com.brand.project.mongo.adapters") */
    private final String packageName;
    private final Map<Class<?>, TypeSpec> adapters;

    public AdapterBuilderContext(String sourcePath, String packageName) {
        this.sourcePath = sourcePath;
        this.packageName = packageName;
        this.adapters = new HashMap<>();
    }

    public <T> void createAdapter(AdapterBuilder<T> adapterBuilder) {
        try {
            adapterBuilder.setModelAdapters(adapters);
            final TypeSpec adapterTypeSpec = adapterBuilder.build();
            adapters.put(adapterBuilder.getModelClass(), adapterTypeSpec);
            JavaFile.builder(packageName, adapterTypeSpec).build().writeTo(new File(sourcePath));
        } catch (Exception e) {
            throw new RuntimeException("Problem building the adapter", e);
        }
    }
}
