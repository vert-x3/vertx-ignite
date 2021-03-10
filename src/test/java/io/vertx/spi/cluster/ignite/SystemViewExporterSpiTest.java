/*
 * Copyright 2021 Red Hat, Inc.
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
package io.vertx.spi.cluster.ignite;

import io.vertx.Lifecycle;
import io.vertx.LoggingTestWatcher;
import io.vertx.core.Vertx;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.ignite.impl.NoopSystemViewExporterSpi;
import io.vertx.test.core.VertxTestBase;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.systemview.jmx.JmxSystemViewExporterSpi;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

/**
 * @author Markus Spika
 */
public class SystemViewExporterSpiTest extends VertxTestBase {

  @Rule
  public LoggingTestWatcher watchman = new LoggingTestWatcher();

  private IgniteOptions options;
  private IgniteClusterManager clusterManager;

  @Override
  protected ClusterManager getClusterManager() {
    assertNotNull("options not set", options);
    return clusterManager = new IgniteClusterManager(options);
  }

  @Override
  protected void closeClustered(List<Vertx> clustered) throws Exception {
    Lifecycle.closeClustered(clustered);
  }

  @Test
  public void shouldEnableJmxSystemViewAsDefault() {
    options = new IgniteOptions().setSystemViewExporterSpiDisabled(false);
    startNodes(1);
    options = null;

    IgniteConfiguration cfg = clusterManager.getIgniteInstance().configuration();
    assertNotNull(cfg);
    assertNotNull(cfg.getSystemViewExporterSpi());
    assertEquals(1, cfg.getSystemViewExporterSpi().length);
    assertTrue(cfg.getSystemViewExporterSpi()[0] instanceof JmxSystemViewExporterSpi);

  }

  @Test
  public void shouldDisableAllSystemViews() {
    options = new IgniteOptions().setSystemViewExporterSpiDisabled(true);
    startNodes(1);
    options = null;

    IgniteConfiguration cfg = clusterManager.getIgniteInstance().configuration();
    assertNotNull(cfg);
    assertNotNull(cfg.getSystemViewExporterSpi());
    assertEquals(1, cfg.getSystemViewExporterSpi().length);
    assertTrue(cfg.getSystemViewExporterSpi()[0] instanceof NoopSystemViewExporterSpi);
  }
}
