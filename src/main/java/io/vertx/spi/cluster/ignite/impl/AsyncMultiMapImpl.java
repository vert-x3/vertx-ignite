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
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.events.CacheEvent;
import org.apache.ignite.events.Event;
import org.apache.ignite.internal.processors.cache.IgniteCacheProxy;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.lang.IgnitePredicate;

import javax.cache.Cache;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static org.apache.ignite.events.EventType.*;

/**
 * MultiMap implementation.
 *
 * @author Andrey Gura
 */
public class AsyncMultiMapImpl<K, V> implements AsyncMultiMap<K, V> {

  private final IgniteCache<K, List<V>> cache;
  private final VertxInternal vertx;
  private final TaskQueue taskQueue = new TaskQueue();
  private final ConcurrentMap<K, ChoosableIterableImpl<V>> subs = new ConcurrentHashMap<>();

  /**
   * Constructor.
   *
   * @param cache {@link IgniteCache} instance.
   * @param vertx {@link Vertx} instance.
   */
  public AsyncMultiMapImpl(IgniteCache<K, List<V>> cache, Vertx vertx) {
    cache.unwrap(Ignite.class).events().localListen(new IgnitePredicate<Event>() {
      @Override public boolean apply(Event event) {
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
      }
    }, EVT_CACHE_OBJECT_REMOVED);

    this.cache = cache.withAsync();
    this.vertx = (VertxInternal) vertx;
  }

  @Override
  public void add(K key, V value, Handler<AsyncResult<Void>> handler) {
    execute(cache -> cache.invoke(key, (entry, arguments) -> {
      List<V> values = entry.getValue();

      if (values == null)
        values = new ArrayList<>();

      values.add(value);
      entry.setValue(values);
      return null;
    }), handler);
  }

  @Override
  public void get(K key, Handler<AsyncResult<ChoosableIterable<V>>> handler) {
    execute(
      cache -> cache.get(key),
      (List<V> items) -> {
        ChoosableIterableImpl<V> it = subs.compute(key, (k, oldValue) -> {
          if (items == null || items.isEmpty()) {
            return null;
          }

          if (oldValue == null) {
            return new ChoosableIterableImpl<V>(items);
          }
          else {
            oldValue.update(items);
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
    execute(cache -> cache.invoke(key, (entry, arguments) -> {
      List<V> values = entry.getValue();

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
      for (Cache.Entry<K, List<V>> entry : cache) {
        cache.invoke(entry.getKey(), (e, args) -> {
          List<V> values = e.getValue();

          if (values != null) {
            values.removeIf(p);

            if (values.isEmpty()) {
              e.remove();
            } else {
              e.setValue(values);
            }
          }

          return null;
        });
      }

      fut.complete();
    }, taskQueue, handler);
  }

  private <R> void execute(Consumer<IgniteCache<K, List<V>>> cacheOp, Handler<AsyncResult<R>> handler) {
    execute(cacheOp, UnaryOperator.identity(), handler);
  }

  private <T, R> void execute(Consumer<IgniteCache<K, List<V>>> cacheOp,
                              Function<T, R> mapper, Handler<AsyncResult<R>> handler) {
    vertx.getOrCreateContext().executeBlocking(f -> {
      cacheOp.accept(cache);
      IgniteFuture<T> future = cache.future();
      f.complete(mapper.apply(future.get()));
    }, taskQueue, handler);
  }
}
