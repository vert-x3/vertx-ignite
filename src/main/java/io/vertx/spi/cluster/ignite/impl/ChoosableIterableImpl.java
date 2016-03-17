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

import io.vertx.core.spi.cluster.ChoosableIterable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ChoosableIterable implementation.
 *
 * @author Andrey Gura
 */
class ChoosableIterableImpl<T> implements ChoosableIterable<T> {
  private static final ChoosableIterable<Object> EMPTY = new ChoosableIterable<Object>() {
    @Override public boolean isEmpty() {
      return true;
    }

    @Override public Object choose() {
      return null;
    }

    @Override public Iterator<Object> iterator() {
      return Collections.emptyIterator();
    }
  };

  private final AtomicReference<List<T>> itemsRef;
  private AtomicInteger chooseCnt = new AtomicInteger();

  public ChoosableIterableImpl(List<T> items) {
    this.itemsRef = new AtomicReference<>(Objects.requireNonNull(items, "items"));
  }

  public void update(List<T> items) {
    itemsRef.set(Objects.requireNonNull(items, "items"));
  }

  @Override
  public boolean isEmpty() {
    return itemsRef.get().isEmpty();
  }

  @Override
  public Iterator<T> iterator() {
    return itemsRef.get().iterator();
  }

  @Override
  public T choose() {
    List<T> items = itemsRef.get();

    if (items.isEmpty()) {
      return null;
    }

    return items.get(Math.abs(chooseCnt.getAndIncrement()) % items.size());
  }

  public static <T> ChoosableIterable<T> empty() {
    return (ChoosableIterable<T>)EMPTY;
  }
}
