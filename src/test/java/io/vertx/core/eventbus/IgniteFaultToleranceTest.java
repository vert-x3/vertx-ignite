/*
 * Copyright 2022 Red Hat, Inc.
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
package io.vertx.core.eventbus;

import io.vertx.Lifecycle;
import io.vertx.core.Vertx;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.ignite.IgniteClusterManager;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;
import java.util.List;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Andrey Gura
 */
public class IgniteFaultToleranceTest extends io.vertx.tests.eventbus.FaultToleranceTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private String igniteHome;

  @Override
  protected ClusterManager getClusterManager() {
    return new IgniteClusterManager();
  }

  @Override
  public void setUp() throws Exception {
    igniteHome = temporaryFolder.newFolder().getAbsolutePath();
    super.setUp();
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    igniteHome = null;
  }

  @Override
  protected List<String> getExternalNodeSystemProperties() {
    try {
      // is java 9+
      Class.forName("java.lang.module.ModuleFinder");
      return Arrays.asList(
        "-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory",
        "-Djava.net.preferIPv4Stack=true",
        "-DIGNITE_HOME=" + igniteHome,
        "-DIGNITE_PERFORMANCE_SUGGESTIONS_DISABLED=true",
        "-DIGNITE_NO_ASCII=true",
        "-DIGNITE_UPDATE_NOTIFIER=false",
        "-DIGNITE_ATOMIC_CACHE_DELETE_HISTORY_SIZE=1000",
        "-DIGNITE_AFFINITY_HISTORY_SIZE=500",
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED"
      );
    } catch(ClassNotFoundException e) {
      return Arrays.asList(
        "-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory",
        "-Djava.net.preferIPv4Stack=true",
        "-DIGNITE_HOME=" + igniteHome,
        "-DIGNITE_PERFORMANCE_SUGGESTIONS_DISABLED=true",
        "-DIGNITE_NO_ASCII=true",
        "-DIGNITE_UPDATE_NOTIFIER=false",
        "-DIGNITE_ATOMIC_CACHE_DELETE_HISTORY_SIZE=1000",
        "-DIGNITE_AFFINITY_HISTORY_SIZE=500"
      );
    }
  }

  @Override
  protected void afterNodeStarted(int i, Process process) throws Exception {
    super.afterNodeStarted(i, process);
    MILLISECONDS.sleep(500L);
  }

  @Override
  protected void afterNodesKilled() throws Exception {
    super.afterNodesKilled();
    // Additional wait to make sure all nodes noticed the shutdowns
    Thread.sleep(10_000);
  }

  @Override
  protected void close(List<Vertx> clustered) throws Exception {
    Lifecycle.close(clustered);
  }
}
