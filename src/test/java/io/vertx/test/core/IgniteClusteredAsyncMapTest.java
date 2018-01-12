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

package io.vertx.test.core;

import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.ignite.IgniteClusterManager;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Andrey Gura
 */
public class IgniteClusteredAsyncMapTest extends ClusteredAsyncMapTest {

  @Override
  protected ClusterManager getClusterManager() {
    return new IgniteClusterManager();
  }

  @Override
  @Test
  @Ignore("Ignite removes the binding even if a new entry is added without ttl")
  public void testMapPutTtlThenPut() {
    super.testMapPutTtlThenPut();
  }

  @Override
  @Test
  @Ignore("Ignite does not support ClusterSerializable yet")
  public void testMapPutGetClusterSerializableObject() {
    super.testMapPutGetClusterSerializableObject();
  }

  @Override
  @Test
  @Ignore("Ignite does not support ClusterSerializable yet")
  public void testMapPutIfAbsentGetClusterSerializableObject() {
    super.testMapPutIfAbsentGetClusterSerializableObject();
  }

  @Override
  @Test
  @Ignore("Ignite does not support ClusterSerializable yet")
  public void testMapRemoveClusterSerializableObject() {
    super.testMapRemoveClusterSerializableObject();
  }

  @Override
  @Test
  @Ignore("Ignite does not support ClusterSerializable yet")
  public void testMapRemoveIfPresentClusterSerializableObject() {
    super.testMapRemoveIfPresentClusterSerializableObject();
  }

  @Override
  @Test
  @Ignore("Ignite does not support ClusterSerializable yet")
  public void testMapReplaceClusterSerializableObject() {
    super.testMapReplaceClusterSerializableObject();
  }

  @Override
  @Test
  @Ignore("Ignite does not support ClusterSerializable yet")
  public void testMapReplaceIfPresentClusterSerializableObject() {
    super.testMapReplaceIfPresentClusterSerializableObject();
  }
}
