package com.codethen.mongo.adapter.generator;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.squareup.javapoet.*;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.lang.model.element.Modifier;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class AdapterBuilder {

	private Class<?> modelClass;
	private Class<Document> docClass = Document.class;
	private Class<? extends BaseDocumentAdapter> adapterSuperclass = BaseDocumentAdapter.class;

	/** Fields that are {@link String} in the model but {@link ObjectId} in the {@link Document} */
	private Collection<String> objectIdDocFields = Collections.singletonList("_id");

	/** Known adapters for other model classes. Must contain adapters for referenced models. */
	private Map<Class<?>, TypeSpec> modelAdapters;

	/** How the model class field names (keys) map to {@link Document} field names (values) */
	private Map<String, String> fieldNames;

	/** Class that contains the fields, named {@link #fieldsClassName} */
	private TypeSpec fieldsTypeSpec;
	private String fieldsClassName = "Fields";
	private String fieldsField = "fields";

	/** Variable that contains a singleton instance of the adapter */
	private final static String instanceVar = "INSTANCE";

	private final static String typeVar = "T";

	/**
	 * Names of methods defined in {@link BaseDocumentAdapter}.
	 * They can be used as helpers to generate code.
	 */
	protected final static String appendTo = "appendTo";
	protected final static String model2doc = "model2doc";
	protected final static String doc2model = "doc2model";
	protected final static String obj2enum = "obj2enum";
	protected final static String enum2obj = "enum2obj";
	protected final static String string2id = "string2id";
	protected final static String id2string = "id2string";


	/** Name of the variable for the model object */
	private String modelVar = "model";

	/** Name of the variable for the {@link Document} object */
	private String docVar = "doc";


	// --- Getters and setters ---

	public Class<? extends BaseDocumentAdapter> getAdapterSuperclass() {
		return adapterSuperclass;
	}

	public void setAdapterSuperclass(Class<? extends BaseDocumentAdapter> adapterSuperclass) {
		this.adapterSuperclass = adapterSuperclass;
	}

	public Map<String, String> getFieldNames() {
		return fieldNames;
	}

	public void setFieldNames(Map<String, String> fieldNames) {
		this.fieldNames = fieldNames;
	}

	/** How a model class field name (key) maps to a {@link Document} field name (value) */
	public void addFieldName(String modelField, String docField) {
		if (this.fieldNames == null) {
			this.fieldNames = new HashMap<>();
		}
		this.fieldNames.put(modelField, docField);
	}

	/** TODO: Rethink of this. Now the context must set this to have the adapters generated so far. */
	public void setModelAdapters(Map<Class<?>, TypeSpec> modelAdapters) {
		this.modelAdapters = modelAdapters;
	}

	public Class<?> getModelClass() {
		return modelClass;
	}

	public void setModelClass(Class<?> modelClass) {
		this.modelClass = modelClass;
	}

	public Class<Document> getDocClass() {
		return docClass;
	}

	public void setDocClass(Class<Document> docClass) {
		this.docClass = docClass;
	}

	public Collection<String> getObjectIdDocFields() {
		return objectIdDocFields;
	}

	public void setObjectIdDocFields(Collection<String> objectIdDocFields) {
		this.objectIdDocFields = objectIdDocFields;
	}

	public TypeSpec build() throws Exception {

		final String adapterName = modelClass.getSimpleName() + "Adapter";

		fieldsTypeSpec = buildFieldsClass();

		return TypeSpec.classBuilder(adapterName)
			.addModifiers(Modifier.PUBLIC)
			.addTypeVariable(TypeVariableName.get(typeVar).withBounds(modelClass))
			.superclass(ParameterizedTypeName.get(ClassName.get(getAdapterSuperclass()), ClassName.bestGuess(typeVar)))
			.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "unchecked").build())
			.addType(fieldsTypeSpec)
			.addField(FieldSpec
				.builder(ClassName.bestGuess(fieldsClassName), fieldsField, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
				.initializer("new $N()", fieldsTypeSpec)
				.build())
			.addField(buildInstanceField(adapterName))
			.addMethod(build_newModelInstance())
			.addMethod(build_model2doc())
			.addMethod(build_doc2model())
			.build();
	}

	public boolean isDocFieldObjectId(String docFieldName) {
		return getObjectIdDocFields().contains(docFieldName);
	}

	/** Builds a static field that contains a singleton of the adapter */
	private FieldSpec buildInstanceField(String adapterName) {
		return FieldSpec.builder(ParameterizedTypeName.get(ClassName.bestGuess(adapterName), ClassName.get(modelClass)), instanceVar, Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
			.initializer("new $L<>()", adapterName)
			.build();
	}

	/** Builds the class that will contain static String fields, each one with each of the {@link #fieldNames} */
	private TypeSpec buildFieldsClass() {

		final Class<? extends BaseDocumentAdapter> adapterSuperclass = getAdapterSuperclass();

		final TypeSpec.Builder fieldsBuilder = TypeSpec.classBuilder(fieldsClassName)
			.superclass(ClassName.get(adapterSuperclass.getPackage().getName(), adapterSuperclass.getSimpleName(), fieldsClassName))
			.addModifiers(Modifier.PUBLIC, Modifier.STATIC);

		for (String fieldName : fieldNames.keySet()) {
			fieldsBuilder.addField(FieldSpec.builder(String.class, fieldName, Modifier.PUBLIC, Modifier.FINAL)
				.initializer("$S", fieldNames.get(fieldName))
				.build());
		}

		return fieldsBuilder.build();
	}

	private MethodSpec build_newModelInstance() {

		final ClassName returnType = ClassName.bestGuess(typeVar);

		return MethodSpec.methodBuilder("newModelInstance")
			.addAnnotation(Override.class)
			.addModifiers(Modifier.PUBLIC)
			.returns(returnType)
			.addStatement("return ($T) new $T()", returnType, modelClass)
			.build();
	}

	private MethodSpec build_model2doc() throws NoSuchFieldException {

		final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(model2doc)
			.addAnnotation(Override.class)
			.addModifiers(Modifier.PUBLIC)
			.returns(docClass)
			.addParameter(ClassName.bestGuess(typeVar), modelVar)
			.addStatement("final $T $L = super.$L($L)", docClass, docVar, model2doc, modelVar)
			.addStatement("if ($L == null) return null", docVar);

		for (String fieldName : fieldNames.keySet()) {
			final Field modelField = modelClass.getDeclaredField(fieldName);
			methodBuilder
				.addStatement(buildModelFieldExtractStatement(docVar, modelVar, modelField));
		}

		return methodBuilder
			.addStatement("return $L", docVar)
			.build();
	}

	private MethodSpec build_doc2model() throws NoSuchFieldException {

		final ClassName returnType = ClassName.bestGuess(typeVar);

		final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(doc2model)
			.addAnnotation(Override.class)
			.addModifiers(Modifier.PUBLIC)
			.returns(returnType)
			.addParameter(docClass, docVar)
			.addStatement("final $T $L = super.$L($L)", returnType, modelVar, doc2model, docVar)
			.addStatement("if ($L == null) return null", modelVar);

		for (String fieldName : fieldNames.keySet()) {
			final Field modelField = modelClass.getDeclaredField(fieldName);
			methodBuilder
				.addStatement(buildDocFieldExtractStatement(modelVar, docVar, modelField));
		}

		return methodBuilder
			.addStatement("return $L", modelVar)
			.build();
	}

	/**
	 * Generates the statement to set the given modelVar field into the docVar.
	 * The result is something like: append(doc, Fields.someField, model.getSomeField())
	 * Usually you don't need to override this method, but you could do it in some special cases.
	 * It's more probable that you want to override {@link #buildModelFieldExtractor(String, Field)}.
	 */
	public CodeBlock buildModelFieldExtractStatement(String docVar, String modelVar, Field modelField) {
		return CodeBlock.builder().add("$L($L, $L.$L, $L)",
			appendTo, docVar, fieldsField, modelField.getName(), buildModelFieldExtractor(modelVar, modelField)).build();
	}

	/**
	 * Generates the statement to set the given docVar field into the modelVar.
	 * The result is something like: model.setSomeField(doc.get(Fields.someField))
	 * Usually you don't need to override this method, but you could do it in some special cases.
	 * It's more probable that you want to override {@link #buildDocFieldExtractor(String, Field, Type)}.
	 */
	public CodeBlock buildDocFieldExtractStatement(String modelVar, String docVar, Field modelField) {
		final String setterMethodName = "set" + StringUtils.capitalize(modelField.getName());
		return CodeBlock.builder().add("$L.$L($L)", modelVar, setterMethodName, buildDocFieldExtractor(docVar, modelField, modelField.getGenericType())).build();
	}


	/**
	 * Generates the code to extract the given field from the modelVar.
	 * The result is something like this for basic types: model.getSomeField().
	 * For some types an adapter function is applied, e.g. enum2obj(model.getSomeField())
	 *
	 * You may override this method in some cases. Return any object that can be converted to a String.
	 * See: https://github.com/square/javapoet#l-for-literals
	 */
	public Object buildModelFieldExtractor(String modelVar, Field modelField) {

		final String getterPrefix = modelField.getType().equals(boolean.class) ? "is" : "get";
		final String getterMethodName = getterPrefix + StringUtils.capitalize(modelField.getName());

		// model.getFIELD()
		CodeBlock result = CodeBlock.builder().add("$L.$L()", modelVar, getterMethodName).build();

		// Adaptations

		if (isFieldObjectId(modelField)) {
			result = applyFunction(string2id, result);

		} else if (containsEnum(modelField.getGenericType())) {
			result = applyFunction(enum2obj, result);

		} else {
			// Model adapters
			final Type itemType = getTypeOrTypeArgument(modelField.getGenericType());
			if (modelAdapters.containsKey(itemType)) {
				result = applyFunction(getAdapterFunction(model2doc, itemType), result);
			}
		}

		return result;
	}

	/**
	 * Generates the code to extract the given field from the docVar.
	 * The result is something like this for basic types: doc.getInteger(Fields.someField).
	 * For some types an adapter function is applied, e.g. obj2enum(doc.get(Fields.someField))
	 *
	 * You may override this method in some cases. Return any object that can be converted to a String.
	 * See: https://github.com/square/javapoet#l-for-literals
	 */
	public Object buildDocFieldExtractor(String docVar, Field modelField, Type fieldType) {

		// doc.get(Fields.someField)
		Object result = CodeBlock.builder().add("$L.$L($L.$L)", docVar, "get", fieldsField, modelField.getName()).build();

		// Adaptations

		final boolean isList = isaParametrizedList(fieldType);

		if (isFieldObjectId(modelField)) {
			result = applyFunction(id2string, applyCast(getType(isList, ObjectId.class), result));

		} else if (containsEnum(fieldType)) {
			result = applyFunction(obj2enum, applyCast(getType(isList, Object.class), result), getTypeOrTypeArgument(fieldType));

		} else {
			// Model adapters
			final Type itemType = getTypeOrTypeArgument(fieldType);
			if (modelAdapters.containsKey(itemType)) {
				result = applyFunction(getAdapterFunction(doc2model, itemType), applyCast(getType(isList, Document.class), result));
			} else {
				result = applyCast(fieldType, result);
			}
		}

		return result;
	}

	private <T> Type getType(boolean inList, Class<T> clazz) {
		return inList ? typeForListOf(clazz) : clazz;
	}

	/**
	 * Creates a type for List[T] where T is the given clazz.
	 * This way, when writing this type in code, we get the real "List[SomeClass]" and not just "List" or "List[T]".
	 */
	private <T> Type typeForListOf(Class<T> clazz) {
		return new TypeToken<List<T>>(){}.where(new TypeParameter<T>() {}, clazz).getType();
	}

	private boolean isFieldObjectId(Field modelField) {
		final boolean result = isDocFieldObjectId(fieldNames.get(modelField.getName()));
		if (result && !getTypeOrTypeArgument(modelField.getGenericType()).equals(String.class)) {
			throw new IllegalArgumentException("Model field " + modelField.getName() + " must be String to be converted to ObjectId");
		}
		return result;
	}

	/**
	 * We overload the adapter functions for one item and a list of items.
	 * That's why we look for the Enum type inside a {@link List} too.
	 * See {@link BaseAdapter#enum2obj} and {@link BaseAdapter#obj2enum}.
	 */
	private boolean containsEnum(Type type) {
		return isEnum(getTypeOrTypeArgument(type));
	}

	private boolean isEnum(Type typeArgument) {
		return typeArgument instanceof Class && ((Class<?>) typeArgument).isEnum();
	}

	/**
	 * We overload the adapter functions for one item and a list of items.
	 * That's why we look for the parameter type inside a {@link List} too.
	 * See {@link BaseAdapter#doc2model} and {@link BaseAdapter#model2doc}.
	 */
	private Type getTypeOrTypeArgument(Type type) {
		return isaParametrizedList(type) ? getTypeArgument(type) : type;
	}

	private boolean isaParametrizedList(Type type) {
		return type instanceof ParameterizedType && ((ParameterizedType) type).getRawType().equals(List.class);
	}

	private Type getTypeArgument(Type type) {
		return ((ParameterizedType) type).getActualTypeArguments()[0];
	}

	private Object getAdapterFunction(String functionName, Type adaptedType) {
		final TypeSpec adapterTypeSpec = modelAdapters.get(adaptedType);
		if (adapterTypeSpec == null) throw new IllegalArgumentException("Unexpectedly, I can't find adapter for type: " + adaptedType);
		return CodeBlock.builder().add("$N.$L.$L", adapterTypeSpec, instanceVar, functionName).build();
	}

	private CodeBlock applyFunction(Object functionName, Object arg) {
		return CodeBlock.builder().add("$L($L)", functionName, arg).build();
	}

	private CodeBlock applyFunction(Object functionName, Object arg, Type type) {
		return CodeBlock.builder().add("$L($L, $T.class)", functionName, arg, type).build();
	}

	private Object applyCast(Type type, Object arg) {
		if (type.equals(Object.class)) return arg;
		return CodeBlock.builder().add("($T) $L", type, arg).build();
	}
}
