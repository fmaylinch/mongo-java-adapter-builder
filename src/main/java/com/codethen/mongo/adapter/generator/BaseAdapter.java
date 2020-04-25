package com.codethen.mongo.adapter.generator;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class provides some utility methods for convenience. These methods could be overridden as necessary.
 */
public abstract class BaseAdapter<M, D> implements Adapter<M, D> {

	public List<D> model2doc(List<M> models) {
		return mapToList(models, this::model2doc);
	}

	public List<M> doc2model(List<D> docs) {
		return mapToList(docs, this::doc2model);
	}

	public <T extends Enum<T>> T obj2enum(Object object, Class<T> clazz) {
		return enumFromName(clazz, (String) object);
	}

	public <T extends Enum<T>> List<T> obj2enum(List<Object> object, Class<T> clazz) {
		return mapToList(object, x -> obj2enum(x, clazz));
	}

	public <T extends Enum<T>> Object enum2obj(T enumValue) {
		return enumValue == null ? null : enumValue.name();
	}

	public <T extends Enum<T>> List<Object> enum2obj(List<T> enumValues) {
		return mapToList(enumValues, this::enum2obj);
	}

	public <T extends Enum<T>> T enumFromName(Class<T> clazz, String name) {
		return name == null ? null : T.valueOf(clazz, name);
	}

	public <T extends Enum<T>> T enumFromOrdinal(Integer ordinal, Class<T> clazz) {
		return ordinal == null ? null : clazz.getEnumConstants()[ordinal];
	}

	public <I,O> List<O> mapToList(List<I> list, Function<I,O> mapper) {
		return list == null ? null : list.stream().map(mapper).collect(Collectors.toList());
	}
}
