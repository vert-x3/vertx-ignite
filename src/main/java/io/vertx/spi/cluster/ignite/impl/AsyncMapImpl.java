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
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.shareddata.AsyncMap;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.lang.IgniteFuture;

import javax.cache.Cache;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static io.vertx.spi.cluster.ignite.impl.ClusterSerializationUtils.marshal;
import static io.vertx.spi.cluster.ignite.impl.ClusterSerializationUtils.unmarshal;

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
    execute(cache -> cache.getAsync(marshal(key)), handler);
  }

  @Override
  public void put(K key, V value, Handler<AsyncResult<Void>> handler) {
    execute(cache -> cache.putAsync(marshal(key), marshal(value)), handler);
  }

  @Override
  public void put(K key, V value, long ttl, Handler<AsyncResult<Void>> handler) {
    executeWithTtl(cache -> cache.putAsync(marshal(key), marshal(value)), handler, ttl);
  }

  @Override
  public void putIfAbsent(K key, V value, Handler<AsyncResult<V>> handler) {
    execute(cache -> cache.getAndPutIfAbsentAsync(marshal(key), marshal(value)), handler);
  }

  @Override
  public void putIfAbsent(K key, V value, long ttl, Handler<AsyncResult<V>> handler) {
    executeWithTtl(cache -> cache.getAndPutIfAbsentAsync(marshal(key), marshal(value)), handler, ttl);
  }

  @Override
  public void remove(K key, Handler<AsyncResult<V>> handler) {
    execute(cache -> cache.getAndRemoveAsync(marshal(key)), handler);
  }

  @Override
  public void removeIfPresent(K key, V value, Handler<AsyncResult<Boolean>> handler) {
    execute(cache -> cache.removeAsync(marshal(key), marshal(value)), handler);
  }

  @Override
  public void replace(K key, V value, Handler<AsyncResult<V>> handler) {
    execute(cache -> cache.getAndReplaceAsync(marshal(key), marshal(value)), handler);
  }

  @Override
  public void replaceIfPresent(K key, V oldValue, V newValue, Handler<AsyncResult<Boolean>> handler) {
    execute(cache -> cache.replaceAsync(marshal(key), marshal(oldValue), marshal(newValue)), handler);
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
        map.put(unmarshal(entry.getKey()), unmarshal(entry.getValue()));
      }
      fut.complete(map);
    }, resultHandler);
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

      IgniteFuture<T> future = cacheOp.apply(cache0);
      Context context = vertx.getOrCreateContext();
      future.listen(fut -> context.executeBlocking(
        f -> f.complete(unmarshal(future.get())), handler)
      );
    } catch (Exception e) {
      handler.handle(Future.failedFuture(e));
    }
  }
}
