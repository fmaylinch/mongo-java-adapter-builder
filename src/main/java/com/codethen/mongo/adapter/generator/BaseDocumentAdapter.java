package com.codethen.mongo.adapter.generator;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.List;

/**
 * Base adapter where one of the models is a {@link Document}.
 * This class provides some utility methods for convenience. These methods could be overridden as necessary.
 */
public abstract class BaseDocumentAdapter<M> extends BaseAdapter<M, Document> {

	@Override
	public Document model2doc(M model) {
		if (model == null) return null;
		return new Document();
	}

	@Override
	public M doc2model(Document doc) {
		if (doc == null) return null;
		return newModelInstance();
	}

	public abstract M newModelInstance();


	public void appendTo(Document doc, String field, Object value) {
		if (value != null)
			doc.append(field, value);
	}

	public ObjectId string2id(String id) {
		return id == null ? null : new ObjectId(id);
	}

	public String id2string(ObjectId id) {
		return id == null ? null : id.toString();
	}

	public List<ObjectId> string2id(List<String> ids) {
		return mapToList(ids, this::string2id);
	}

	public List<String> id2string(List<ObjectId> ids) {
		return mapToList(ids, this::id2string);
	}

	public <T> List<T> getList(Document doc, String fieldName, Class<T> clazz) {
		return doc.getList(fieldName, clazz); // <-- verifies each object
		// return (List<T>) doc.get(fieldName); <-- Also works, doesn't verify
	}

	public static class Fields {
		// Subclasses will add fields
	}
}
