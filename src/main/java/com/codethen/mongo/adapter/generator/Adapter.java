package com.codethen.mongo.adapter.generator;

/**
 * Generic adapter to convert between two models.
 *
 * TODO: rename type parameters and methods, because this adapter should not know is M and D.
 */
public interface Adapter<M, D> {

	D model2doc(M model);

	M doc2model(D doc);
}
