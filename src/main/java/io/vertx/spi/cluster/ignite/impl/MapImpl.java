/*
 * Copyright (c) 2015 The original author or authors
 * ---------------------------------
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.spi.cluster.ignite.impl;

import org.apache.ignite.IgniteCache;

import javax.cache.Cache;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents Apache Ignite cache as {@link java.util.Map} interface implementation.
 */
public class MapImpl<K, V> implements Map<K, V> {

  private final IgniteCache<K, V> cache;

  /**
   * Constructor.
   *
   * @param cache Ignite cache instance.
   */
  public MapImpl(IgniteCache<K, V> cache) {
    this.cache = cache;
  }

  IgniteCache<K, V> getCache() {
    return cache;
  }

  @Override
  public int size() {
    return cache.size();
  }

  @Override
  public boolean isEmpty() {
    return cache.size() == 0;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean containsKey(Object key) {
    return cache.containsKey((K) key);
  }

  @Override
  public boolean containsValue(Object value) {
    for (Cache.Entry<K, V> entry : cache) {
      if (entry.getValue().equals(value))
        return true;
    }

    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public V get(Object key) {
    return cache.get((K) key);
  }

  @Override
  public V put(K key, V value) {
    return cache.getAndPut(key, value);
  }

  @Override
  @SuppressWarnings("unchecked")
  public V remove(Object key) {
    return cache.getAndRemove((K) key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    cache.putAll(map);
  }

  @Override
  public void clear() {
    cache.clear();
  }

  @Override
  public Set<K> keySet() {
    Set<K> res = new HashSet<>();

    for (Cache.Entry<K, V> entry : cache) {
      res.add(entry.getKey());
    }

    return res;
  }

  @Override
  public Collection<V> values() {
    Collection<V> res = new ArrayList<>();

    for (Cache.Entry<K, V> entry : cache) {
      res.add(entry.getValue());
    }

    return res;
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    Set<Entry<K, V>> res = new HashSet<>();

    for (Cache.Entry<K, V> entry : cache) {
      res.add(new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()));
    }

    return res;
  }

  @Override
  public V putIfAbsent(K key, V value) {
    return cache.getAndPutIfAbsent(key, value);
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean remove(Object key, Object value) {
    return cache.remove((K) key, (V) value);
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    return cache.replace(key, oldValue, newValue);
  }

  @Override
  public V replace(K key, V value) {
    return cache.getAndReplace(key, value);
  }
}
