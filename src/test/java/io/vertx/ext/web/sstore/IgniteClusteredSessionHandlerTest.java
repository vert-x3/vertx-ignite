/*
 * Copyright 2018 Red Hat, Inc.
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

package io.vertx.ext.web.sstore;

import io.vertx.Lifecycle;
import io.vertx.LoggingTestWatcher;
import io.vertx.core.Vertx;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.it.sstore.ClusteredSessionHandlerTest;
import io.vertx.junit5.VertxTestContext;
import io.vertx.spi.cluster.ignite.IgniteClusterManager;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Thomas Segismont
 */
public class IgniteClusteredSessionHandlerTest extends ClusteredSessionHandlerTest {

//  @Rule
//  public LoggingTestWatcher watchman = new LoggingTestWatcher();

  @TempDir
  Path tmpDir;

  @BeforeEach
  @Override
  public void setUp(Vertx vertx, VertxTestContext testContext) throws Exception {
    System.setProperty("IGNITE_HOME", tmpDir.toFile().getAbsolutePath());
    super.setUp(vertx, testContext);
  }

  @AfterEach
  @Override
  public void tearDown(VertxTestContext testContext) throws Exception {
    super.tearDown(testContext);
    System.clearProperty("IGNITE_HOME");
  }

  @Override
  protected ClusterManager getClusterManager() {
    return new IgniteClusterManager();
  }

  @Override
  protected void close(List<Vertx> clustered) throws Exception {
    Lifecycle.close(clustered);
  }
}
