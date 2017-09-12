/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.spi.cluster.ignite.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.shareddata.AsyncMapStream;
import org.apache.ignite.cache.query.QueryCursor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Thomas Segismont
 */
public class QueryCursorStream<IN, OUT> implements AsyncMapStream<OUT> {

  private static final int BATCH_SIZE = 10;

  private final Context context;
  private final Supplier<QueryCursor<IN>> queryCursorSupplier;
  private final Function<IN, OUT> converter;

  private QueryCursor<IN> queryCursor;
  private Iterator<IN> iterator;
  private Deque<IN> queue;
  private Handler<OUT> dataHandler;
  private Handler<Throwable> exceptionHandler;
  private Handler<Void> endHandler;
  private boolean paused;
  private boolean readInProgress;
  private boolean closed;

  public QueryCursorStream(Context context, Supplier<QueryCursor<IN>> queryCursorSupplier, Function<IN, OUT> converter) {
    this.context = context;
    this.queryCursorSupplier = queryCursorSupplier;
    this.converter = converter;
  }

  @Override
  public synchronized QueryCursorStream<IN, OUT> exceptionHandler(Handler<Throwable> handler) {
    checkClosed();
    this.exceptionHandler = handler;
    return this;
  }

  private void checkClosed() {
    if (closed) {
      throw new IllegalArgumentException("Stream is closed");
    }
  }

  @Override
  public synchronized QueryCursorStream<IN, OUT> handler(Handler<OUT> handler) {
    checkClosed();
    this.dataHandler = handler;
    context.<QueryCursor<IN>>executeBlocking(fut -> fut.complete(queryCursorSupplier.get()), ar -> {
      synchronized (this) {
        if (ar.succeeded()) {
          queryCursor = ar.result();
          if (canRead()) {
            doRead();
          }
        } else {
          handleException(ar.cause());
        }
      }
    });
    return this;
  }

  private boolean canRead() {
    return dataHandler != null && !paused && !closed;
  }

  @Override
  public synchronized QueryCursorStream<IN, OUT> pause() {
    checkClosed();
    paused = true;
    return this;
  }

  @Override
  public synchronized QueryCursorStream<IN, OUT> resume() {
    checkClosed();
    if (paused) {
      paused = false;
      if (dataHandler != null) {
        doRead();
      }
    }
    return this;
  }

  private synchronized void doRead() {
    if (readInProgress) {
      return;
    }
    readInProgress = true;
    if (iterator == null) {
      context.<Iterator<IN>>executeBlocking(fut -> fut.complete(queryCursor.iterator()), ar -> {
        synchronized (this) {
          readInProgress = false;
          if (ar.succeeded()) {
            iterator = ar.result();
            if (canRead()) {
              doRead();
            }
          } else {
            handleException(ar.cause());
          }
        }
      });
      return;
    }
    if (queue == null) {
      queue = new ArrayDeque<>(BATCH_SIZE);
    }
    if (!queue.isEmpty()) {
      context.runOnContext(v -> emitQueued());
      return;
    }
    for (int i = 0; i < BATCH_SIZE && iterator.hasNext(); i++) {
      queue.add(iterator.next());
    }
    if (queue.isEmpty()) {
      context.runOnContext(v -> {
        synchronized (this) {
          readInProgress = false;
          if (endHandler != null) {
            endHandler.handle(null);
          }
        }
      });
      return;
    }
    context.runOnContext(v -> emitQueued());
  }

  private void handleException(Throwable cause) {
    if (exceptionHandler != null) {
      exceptionHandler.handle(cause);
    }
  }

  private synchronized void emitQueued() {
    while (!queue.isEmpty() && canRead()) {
      dataHandler.handle(converter.apply(queue.remove()));
    }
    readInProgress = false;
    if (canRead()) {
      doRead();
    }
  }

  @Override
  public synchronized QueryCursorStream<IN, OUT> endHandler(Handler<Void> handler) {
    endHandler = handler;
    return this;
  }

  @Override
  public void close(Handler<AsyncResult<Void>> completionHandler) {
    context.<Void>executeBlocking(fut -> {
      queryCursor.close();
      fut.complete();
    }, ar -> {
      synchronized (this) {
        closed = true;
        if (completionHandler != null) {
          completionHandler.handle(ar);
        }
      }
    });
  }
}
