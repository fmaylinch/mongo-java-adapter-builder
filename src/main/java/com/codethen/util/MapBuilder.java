package com.codethen.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Convenience builder to chain put calls and make map creation less verbose.
 * By default, creates a {@link HashMap}.
 */
public class MapBuilder<K,V> {

	private Map<K,V> map;
	private boolean unmodifiable = false;

	private MapBuilder(Map<K, V> map) {
		this.map = map;
	}

	public static <K,V> MapBuilder<K,V> normal() {
		return new MapBuilder<>(new HashMap<>());
	}

	public static <K,V> MapBuilder<K,V> linked() {
		return new MapBuilder<>(new LinkedHashMap<>());
	}

	public MapBuilder<K, V> put(K key, V value) {
		map.put(key, value);
		return this;
	}

	public MapBuilder<K, V> unmodifiable() {
		this.unmodifiable = true;
		return this;
	}

	public Map<K, V> build() {
		return unmodifiable
			? Collections.unmodifiableMap(map)
			: map;
	}
}