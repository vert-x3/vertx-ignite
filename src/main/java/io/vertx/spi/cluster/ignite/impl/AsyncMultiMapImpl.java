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
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.TaskQueue;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.spi.cluster.AsyncMultiMap;
import io.vertx.core.spi.cluster.ChoosableIterable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import javax.cache.Cache;
import javax.cache.CacheException;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.events.CacheEvent;
import org.apache.ignite.events.Event;
import org.apache.ignite.internal.processors.cache.IgniteCacheProxy;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.lang.IgnitePredicate;

import static org.apache.ignite.events.EventType.EVT_CACHE_OBJECT_REMOVED;

/**
 * MultiMap implementation.
 *
 * @author Andrey Gura
 */
public class AsyncMultiMapImpl<K, V> implements AsyncMultiMap<K, V> {

  private final IgniteCache<K, Set<V>> cache;
  private final VertxInternal vertx;
  private final TaskQueue taskQueue = new TaskQueue();
  private final ConcurrentMap<K, ChoosableIterableImpl<V>> subs = new ConcurrentHashMap<>();

  /**
   * Constructor.
   *
   * @param cache {@link IgniteCache} instance.
   * @param vertx {@link Vertx} instance.
   */
  public AsyncMultiMapImpl(IgniteCache<K, Set<V>> cache, Vertx vertx) {
    cache.unwrap(Ignite.class).events().localListen((IgnitePredicate<Event>)event -> {
      if (!(event instanceof CacheEvent)) {
        throw new IllegalArgumentException("Unknown event received: " + event);
      }

      CacheEvent cacheEvent = (CacheEvent)event;

      if (Objects.equals(cacheEvent.cacheName(), cache.getName()) &&
          ((IgniteCacheProxy)cache).context().localNodeId().equals(cacheEvent.eventNode().id())) {
        K key = cacheEvent.key();

        switch (cacheEvent.type()) {
          case EVT_CACHE_OBJECT_REMOVED:
            subs.remove(key);
            break;

          default:
            throw new IllegalArgumentException("Unknown event received: " + event);
        }
      }

      return true;
    }, EVT_CACHE_OBJECT_REMOVED);

    this.cache = cache;
    this.vertx = (VertxInternal) vertx;
  }

  @Override
  public void add(K key, V value, Handler<AsyncResult<Void>> handler) {
    execute(cache -> cache.invokeAsync(key, (entry, arguments) -> {
      Set<V> values = entry.getValue();

      if (values == null)
        values = new HashSet<>();

      values.add(value);
      entry.setValue(values);
      return null;
    }), handler);
  }

  @Override
  public void get(K key, Handler<AsyncResult<ChoosableIterable<V>>> handler) {
    execute(
      cache -> cache.getAsync(key),
      (Set<V> items) -> {
        ChoosableIterableImpl<V> it = subs.compute(key, (k, oldValue) -> {
          if (items == null || items.isEmpty()) {
            return null;
          }

          if (oldValue == null) {
            return new ChoosableIterableImpl<>(new ArrayList<>(items));
          }
          else {
            oldValue.update(new ArrayList<>(items));
            return oldValue;
          }
        });

        return it == null ? ChoosableIterableImpl.empty() : it;
      },
      handler
    );
  }

  @Override
  public void remove(K key, V value, Handler<AsyncResult<Boolean>> handler) {
    execute(cache -> cache.invokeAsync(key, (entry, arguments) -> {
      Set<V> values = entry.getValue();

      if (values != null) {
        boolean removed = values.remove(value);

        if (values.isEmpty()) {
          entry.remove();
        } else {
          entry.setValue(values);
        }

        return removed;
      }

      return false;
    }), handler);
  }

  @Override
  public void removeAllForValue(V value, Handler<AsyncResult<Void>> handler) {
    removeAllMatching(value::equals, handler);
  }

  @Override
  public void removeAllMatching(Predicate<V> p, Handler<AsyncResult<Void>> handler) {
    vertx.getOrCreateContext().executeBlocking(fut -> {
      boolean success = false;
      Exception err = null;

      for (int i = 0; i < 5; i++) { // Cache iterator can be broken in case when node left topology. Need repeat.
        try {
          for (Cache.Entry<K, Set<V>> entry : cache) {
            cache.invokeAsync(entry.getKey(), (e, args) -> {
              Set<V> values = e.getValue();

              if (values != null) {
                values.removeIf(p);

                if (values.isEmpty()) {
                  e.remove();
                }
                else {
                  e.setValue(values);
                }
              }

              return null;
            });
          }

          success = true;

          fut.complete();

          break;
        }
        catch (CacheException e) {
          err = e;
        }
      }

      if (!success) {
        fut.fail(err);
      }
    }, taskQueue, handler);
  }

  private <T> void execute(Function<IgniteCache<K, Set<V>>, IgniteFuture<T>> cacheOp, Handler<AsyncResult<T>> handler) {
    execute(cacheOp, UnaryOperator.identity(), handler);
  }

  private <T, R> void execute(Function<IgniteCache<K, Set<V>>, IgniteFuture<T>> cacheOp,
                              Function<T, R> mapper, Handler<AsyncResult<R>> handler) {
    vertx.getOrCreateContext().executeBlocking(f -> {
      IgniteFuture<T> future = cacheOp.apply(cache);
      f.complete(mapper.apply(future.get()));
    }, taskQueue, handler);
  }
}
