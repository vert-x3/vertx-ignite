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

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.streams.ReadStream;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.lang.IgniteFuture;

import javax.cache.Cache;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Async wrapper for {@link MapImpl}.
 *
 * @author Andrey Gura
 */
public class AsyncMapImpl<K, V> implements AsyncMap<K, V> {

  private final Vertx vertx;
  private final IgniteCache<K, V> cache;

  /**
   * Constructor.
   *
   * @param cache {@link IgniteCache} instance.
   * @param vertx {@link Vertx} instance.
   */
  public AsyncMapImpl(IgniteCache<K, V> cache, Vertx vertx) {
    this.cache = cache;
    this.vertx = vertx;
  }

  @Override
  public void get(K key, Handler<AsyncResult<V>> handler) {
    execute(cache -> cache.getAsync(key), handler);
  }

  @Override
  public void put(K key, V value, Handler<AsyncResult<Void>> handler) {
    execute(cache -> cache.putAsync(key, value), handler);
  }

  @Override
  public void put(K key, V value, long ttl, Handler<AsyncResult<Void>> handler) {
    executeWithTtl(cache -> cache.putAsync(key, value), handler, ttl);
  }

  @Override
  public void putIfAbsent(K key, V value, Handler<AsyncResult<V>> handler) {
    execute(cache -> cache.getAndPutIfAbsentAsync(key, value), handler);
  }

  @Override
  public void putIfAbsent(K key, V value, long ttl, Handler<AsyncResult<V>> handler) {
    executeWithTtl(cache -> cache.getAndPutIfAbsentAsync(key, value), handler, ttl);
  }

  @Override
  public void remove(K key, Handler<AsyncResult<V>> handler) {
    execute(cache -> cache.getAndRemoveAsync(key), handler);
  }

  @Override
  public void removeIfPresent(K key, V value, Handler<AsyncResult<Boolean>> handler) {
    execute(cache -> cache.removeAsync(key, value), handler);
  }

  @Override
  public void replace(K key, V value, Handler<AsyncResult<V>> handler) {
    execute(cache -> cache.getAndReplaceAsync(key, value), handler);
  }

  @Override
  public void replaceIfPresent(K key, V oldValue, V newValue, Handler<AsyncResult<Boolean>> handler) {
    execute(cache -> cache.replaceAsync(key, oldValue, newValue), handler);
  }

  @Override
  public void clear(Handler<AsyncResult<Void>> handler) {
    execute(IgniteCache::clearAsync, handler);
  }

  @Override
  public void size(Handler<AsyncResult<Integer>> handler) {
    execute(IgniteCache::sizeAsync, handler);
  }

  @Override
  public void keys(Handler<AsyncResult<Set<K>>> resultHandler) {
    Future<Map<K, V>> entriesFuture = Future.future();
    entries(entriesFuture);
    entriesFuture.map(Map::keySet).setHandler(resultHandler);
  }

  @Override
  public void values(Handler<AsyncResult<List<V>>> resultHandler) {
    Future<Map<K, V>> entriesFuture = Future.future();
    entries(entriesFuture);
    entriesFuture.<List<V>>map(map -> new ArrayList<>(map.values())).setHandler(resultHandler);
  }

  @Override
  public void entries(Handler<AsyncResult<Map<K, V>>> resultHandler) {
    vertx.executeBlocking(fut -> {
      List<Cache.Entry<K, V>> all = cache.query(new ScanQuery<K, V>()).getAll();
      Map<K, V> map = new HashMap<>(all.size());
      for (Cache.Entry<K, V> entry : all) {
        map.put(entry.getKey(), entry.getValue());
      }
      fut.complete(map);
    }, resultHandler);
  }

  @Override
  public ReadStream<K> keyStream() {
    return new QueryCursorStream<>(vertx.getOrCreateContext(), () -> cache.query(new ScanQuery<K, V>()), Cache.Entry::getKey);
  }

  @Override
  public ReadStream<V> valueStream() {
    return new QueryCursorStream<>(vertx.getOrCreateContext(), () -> cache.query(new ScanQuery<K, V>()), Cache.Entry::getValue);
  }

  @Override
  public ReadStream<Map.Entry<K, V>> entryStream() {
    return new QueryCursorStream<>(vertx.getOrCreateContext(), () -> cache.query(new ScanQuery<K, V>()), cacheEntry -> {
      return new SimpleImmutableEntry<>(cacheEntry.getKey(), cacheEntry.getValue());
    });
  }

  private <T> void execute(Function<IgniteCache<K, V>, IgniteFuture<T>> cacheOp, Handler<AsyncResult<T>> handler) {
    executeWithTtl(cacheOp, handler, -1);
  }

  /**
   * @param ttl Time to live in ms.
   */
  private <T> void executeWithTtl(Function<IgniteCache<K, V>, IgniteFuture<T>> cacheOp,
    Handler<AsyncResult<T>> handler, long ttl) {
    try {
      IgniteCache<K, V> cache0 = ttl > 0 ?
        cache.withExpiryPolicy(new CreatedExpiryPolicy(new Duration(TimeUnit.MILLISECONDS, ttl))) : cache;

      IgniteFuture<T> future = cacheOp.apply(cache0);;
      future.listen(fut -> vertx.executeBlocking(
        f -> f.complete(future.get()), handler)
      );
    } catch (Exception e) {
      handler.handle(Future.failedFuture(e));
    }
  }
}
