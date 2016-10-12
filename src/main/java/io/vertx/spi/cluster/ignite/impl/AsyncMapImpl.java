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
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.lang.IgniteFuture;

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
    this.cache = cache.withAsync();
    this.vertx = vertx;
  }

  @Override
  public void get(K key, Handler<AsyncResult<V>> handler) {
    execute(cache -> cache.get(key), handler);
  }

  @Override
  public void put(K key, V value, Handler<AsyncResult<Void>> handler) {
    execute(cache -> cache.put(key, value), handler);
  }

  @Override
  public void put(K key, V value, long ttl, Handler<AsyncResult<Void>> handler) {
    executeWithTtl(cache -> cache.put(key, value), handler, ttl);
  }

  @Override
  public void putIfAbsent(K key, V value, Handler<AsyncResult<V>> handler) {
    execute(cache -> cache.getAndPutIfAbsent(key, value), handler);
  }

  @Override
  public void putIfAbsent(K key, V value, long ttl, Handler<AsyncResult<V>> handler) {
    executeWithTtl(cache -> cache.getAndPutIfAbsent(key, value), handler, ttl);
  }

  @Override
  public void remove(K key, Handler<AsyncResult<V>> handler) {
    execute(cache -> cache.getAndRemove(key), handler);
  }

  @Override
  public void removeIfPresent(K key, V value, Handler<AsyncResult<Boolean>> handler) {
    execute(cache -> cache.remove(key, value), handler);
  }

  @Override
  public void replace(K key, V value, Handler<AsyncResult<V>> handler) {
    execute(cache -> cache.getAndReplace(key, value), handler);
  }

  @Override
  public void replaceIfPresent(K key, V oldValue, V newValue, Handler<AsyncResult<Boolean>> handler) {
    execute(cache -> cache.replace(key, oldValue, newValue), handler);
  }

  @Override
  public void clear(Handler<AsyncResult<Void>> handler) {
    execute(IgniteCache::clear, handler);
  }

  @Override
  public void size(Handler<AsyncResult<Integer>> handler) {
    execute(IgniteCache::size, handler);
  }

  private <T> void execute(Consumer<IgniteCache<K, V>> cacheOp, Handler<AsyncResult<T>> handler) {
    executeWithTtl(cacheOp, handler, -1);
  }

  /**
   * @param ttl Time to live in ms.
   */
  private <T> void executeWithTtl(Consumer<IgniteCache<K, V>> cacheOp, Handler<AsyncResult<T>> handler, long ttl) {
    try {
      IgniteCache<K, V> cache0 = ttl > 0 ?
        cache.withExpiryPolicy(new CreatedExpiryPolicy(new Duration(TimeUnit.MILLISECONDS, ttl))) : cache;
      cacheOp.accept(cache0);

      IgniteFuture<T> future = cache0.future();
      future.listen(fut -> vertx.executeBlocking(
        f -> f.complete(future.get()), handler)
      );
    } catch (Exception e) {
      handler.handle(Future.failedFuture(e));
    }
  }
}
