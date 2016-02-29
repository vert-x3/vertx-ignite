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

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

/**
 * ChoosableIterable implementation.
 *
 * @author Andrey Gura
 */
class ChoosableIterableImpl<T> implements ChoosableIterable<T> {

  private final Collection<T> col;
  private Iterator<T> iter;

  public ChoosableIterableImpl(Collection<T> col) {
    this.col = Objects.requireNonNull(col, "col");
  }

  @Override
  public boolean isEmpty() {
    return col.isEmpty();
  }

  @Override
  public Iterator<T> iterator() {
    return col.iterator();
  }

  @Override
  public T choose() {
    if (col.isEmpty())
      return null;

    if (iter == null || !iter.hasNext()) {
      iter = col.iterator();
    }

    return iter.hasNext() ? iter.next() : null;
  }
}
