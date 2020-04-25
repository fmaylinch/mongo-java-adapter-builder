package com.codethen.mongo.adapter.generator;

/**
 * Generic adapter to convert between two models.
 *
 * TODO: rename methods because this adapter doesn't know what are the types.
 */
public interface Adapter<M, D> {

	D model2doc(M model);

	M doc2model(D doc);
}
