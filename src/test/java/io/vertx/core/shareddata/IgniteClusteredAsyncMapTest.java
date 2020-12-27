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

package io.vertx.core.shareddata;

import io.vertx.Lifecycle;
import io.vertx.LoggingTestWatcher;
import io.vertx.core.Vertx;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.ignite.IgniteClusterManager;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author Andrey Gura
 */
public class IgniteClusteredAsyncMapTest extends ClusteredAsyncMapTest {

  @Rule
  public LoggingTestWatcher watchman = new LoggingTestWatcher();

  @Override
  protected ClusterManager getClusterManager() {
    return new IgniteClusterManager();
  }

  @Override
  protected void closeClustered(List<Vertx> clustered) throws Exception {
    Lifecycle.closeClustered(clustered);
  }

  @Test
  public void testMapPutThenPutTtl() {
    this.getVertx().sharedData().<String, String>getAsyncMap("foo", this.onSuccess((map) -> {
        map.put("pipo", "mili", this.onSuccess((vd) -> {
          map.put("pipo", "mala", 10L, this.onSuccess((vd2) -> {
            this.getVertx().sharedData().<String, String>getAsyncMap("foo", this.onSuccess((map2) -> {
              assertWaitUntil2(map2, "pipo", 20L, Objects::isNull);
            }));
        }));
      }));
    }));
    this.await();
  }

  private void assertWaitUntil2(AsyncMap<String, String> map, String key, long delay, Function<String, Boolean> checks) {
    this.vertx.setTimer(delay, (l) -> {
      map.get(key, this.onSuccess((value) -> {
        assertTrue(checks.apply(value));
        this.testComplete();
      }));
    });
  }
}
